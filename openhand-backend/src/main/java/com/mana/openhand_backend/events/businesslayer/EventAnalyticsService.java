package com.mana.openhand_backend.events.businesslayer;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.domainclientlayer.EventAnalyticsResponseModel;
import com.mana.openhand_backend.events.domainclientlayer.GlobalAnalyticsResponseModel;
import com.mana.openhand_backend.events.utils.EventNotFoundException;
import com.mana.openhand_backend.registrations.dataaccesslayer.Registration;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventAnalyticsService {

    private final EventRepository eventRepository;
    private final RegistrationRepository registrationRepository;

    public EventAnalyticsService(EventRepository eventRepository, RegistrationRepository registrationRepository) {
        this.eventRepository = eventRepository;
        this.registrationRepository = registrationRepository;
    }

    @Transactional(readOnly = true)
    public EventAnalyticsResponseModel getEventAnalytics(Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        EventAnalyticsResponseModel response = new EventAnalyticsResponseModel();
        response.setEventId(event.getId());
        response.setTitle(event.getTitle());
        response.setCategory(event.getCategory());

        // STAGE 1 & 2: Timeline Reconstruction & Normalization for THIS event
        List<Registration> eventRegs = registrationRepository.findByEventIdAndStatusIn(eventId,
                Arrays.asList(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED,
                        RegistrationStatus.CANCELLED));

        List<EventAnalyticsResponseModel.DailyMetric> thisTimeline = buildNormalizedTimeline(event, eventRegs);
        response.setEventTimeline(thisTimeline);

        // STAGE 3: Historical Aggregation ("The Usual") — uses ALL other events
        // globally
        List<Event> otherEvents = getAllOtherEvents(eventId);
        List<EventAnalyticsResponseModel.DailyMetric> usualTimeline = buildAggregatedUsualTimeline(otherEvents);
        response.setUsualTrendTimeline(usualTimeline);

        // STAGE 4 & 5: Forecasting & Metric Extraction
        calculateAndSetPerformanceMetrics(event, thisTimeline, usualTimeline, otherEvents, response);

        return response;
    }

    @Transactional(readOnly = true)
    public GlobalAnalyticsResponseModel getGlobalAnalytics() {
        GlobalAnalyticsResponseModel global = new GlobalAnalyticsResponseModel();

        List<Event> allEvents = eventRepository.findAll();
        // Include ALL events, not just future ones, for a complete global view
        List<Event> activeEvents = allEvents.stream()
                .filter(e -> e.getStartDateTime() != null)
                .collect(Collectors.toList());

        int totalConfirmed = 0;
        int totalWaitlisted = 0;
        double totalVelocity = 0;

        List<GlobalAnalyticsResponseModel.EventPerformanceSummary> performanceSummaries = new ArrayList<>();

        for (Event e : activeEvents) {
            // Aggregate totals
            List<Registration> regs = registrationRepository.findByEventIdAndStatusIn(e.getId(),
                    Arrays.asList(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED));

            for (Registration r : regs) {
                if (r.getStatus() == RegistrationStatus.CONFIRMED)
                    totalConfirmed++;
                else if (r.getStatus() == RegistrationStatus.WAITLISTED)
                    totalWaitlisted++;
            }

            // Get velocity and performance from individual event analytics
            EventAnalyticsResponseModel eAnalytics = getEventAnalytics(e.getId());
            totalVelocity += eAnalytics.getCurrentVelocity();

            double attendanceDelta = eAnalytics.getConfirmedDeltaVsUsual();

            performanceSummaries.add(new GlobalAnalyticsResponseModel.EventPerformanceSummary(
                    e.getId(),
                    e.getTitle(),
                    e.getCurrentRegistrations() != null ? e.getCurrentRegistrations() : 0,
                    e.getMaxCapacity(),
                    attendanceDelta,
                    attendanceDelta > 0));
        }

        double waitlistPct = (totalConfirmed + totalWaitlisted) > 0
                ? ((double) totalWaitlisted / (totalConfirmed + totalWaitlisted)) * 100
                : 0.0;

        global.setTotalWaitlistedPercentage(waitlistPct);
        global.setTotalConfirmed(totalConfirmed);
        global.setTotalWaitlisted(totalWaitlisted);
        global.setCurrentGlobalVelocity(totalVelocity);

        // Mock historical velocity for now (can expand logic later)
        double historicalVelocity = activeEvents.size() * 2.5; // Assuming avg 2.5 regs/day historically
        global.setHistoricalGlobalVelocity(historicalVelocity);

        double velocityDelta = historicalVelocity > 0
                ? ((totalVelocity - historicalVelocity) / historicalVelocity) * 100
                : 0.0;
        global.setVelocityDeltaPercentage(velocityDelta);
        global.setPerformingBetterThanUsual(velocityDelta > 0);

        global.setActiveEventPerformances(performanceSummaries);

        return global;
    }

    /**
     * STAGE 1 & 2: Algorithm to reconstruct timeline and normalize to "T-Minus
     * Days" buckets
     */
    private List<EventAnalyticsResponseModel.DailyMetric> buildNormalizedTimeline(Event event,
            List<Registration> registrations) {
        if (event.getStartDateTime() == null)
            return new ArrayList<>();

        LocalDate startDate = event.getStartDateTime().toLocalDate();
        LocalDate earliestDate = startDate;

        // Collect all distinct timestamp events
        class TimelineEvent {
            LocalDate date;
            int confirmedDelta;
            int waitlistDelta;
            int cancelledDelta;

            public TimelineEvent(LocalDate date, int c, int w, int ca) {
                this.date = date;
                this.confirmedDelta = c;
                this.waitlistDelta = w;
                this.cancelledDelta = ca;
            }
        }

        List<TimelineEvent> rawEvents = new ArrayList<>();

        for (Registration r : registrations) {
            if (r.getRequestedAt() != null) {
                LocalDate reqDate = r.getRequestedAt().toLocalDate();
                if (reqDate.isBefore(earliestDate))
                    earliestDate = reqDate;

                // Track when they requested (could be straight to confirmed or to waitlist)
                if (r.getConfirmedAt() != null && r.getRequestedAt().equals(r.getConfirmedAt())) {
                    rawEvents.add(new TimelineEvent(reqDate, 1, 0, 0));
                } else if (r.getWaitlistedPosition() != null && r.getWaitlistedPosition() > 0) {
                    rawEvents.add(new TimelineEvent(reqDate, 0, 1, 0));
                } else {
                    // Safety net for edge cases, default to waitlist inference if unknown
                    rawEvents.add(new TimelineEvent(reqDate, 0, 1, 0));
                }
            }

            // Track waitlist promotion
            if (r.getConfirmedAt() != null && r.getRequestedAt() != null
                    && !r.getConfirmedAt().equals(r.getRequestedAt())) {
                rawEvents.add(new TimelineEvent(r.getConfirmedAt().toLocalDate(), 1, -1, 0));
            }

            // Track cancellations
            if (r.getCancelledAt() != null) {
                rawEvents.add(new TimelineEvent(r.getCancelledAt().toLocalDate(), 0, 0, 1));
                // We also must decrement whatever state they were in when they cancelled
                // (Approximation: If they had a confirmed date prior to cancellation, decrement
                // confirmed. else waitlist)
                if (r.getConfirmedAt() != null && r.getConfirmedAt().isBefore(r.getCancelledAt())) {
                    rawEvents.add(new TimelineEvent(r.getCancelledAt().toLocalDate(), -1, 0, 0));
                } else {
                    rawEvents.add(new TimelineEvent(r.getCancelledAt().toLocalDate(), 0, -1, 0));
                }
            }
        }

        // Sort chronologically
        rawEvents.sort(Comparator.comparing(e -> e.date));

        // Group by Date to find net changes per day
        Map<LocalDate, TimelineEvent> dailyNetChanges = new HashMap<>();
        for (TimelineEvent ev : rawEvents) {
            TimelineEvent daily = dailyNetChanges.computeIfAbsent(ev.date, d -> new TimelineEvent(d, 0, 0, 0));
            daily.confirmedDelta += ev.confirmedDelta;
            daily.waitlistDelta += ev.waitlistDelta;
            daily.cancelledDelta += ev.cancelledDelta;
        }

        // Traverse from earliest date to Event Start Date to build continuous running
        // totals
        List<EventAnalyticsResponseModel.DailyMetric> metrics = new ArrayList<>();
        int runningConfirmed = 0;
        int runningWaitlist = 0;
        int runningCancelled = 0;

        LocalDate iteratorDate = earliestDate;
        LocalDate endDate = LocalDate.now().isBefore(startDate) ? LocalDate.now() : startDate; // Stop at today if event
                                                                                               // is in the future

        if (iteratorDate.isAfter(endDate)) {
            // Event was created today or after today (rare edge case)
            iteratorDate = endDate;
        }

        while (!iteratorDate.isAfter(endDate)) {
            TimelineEvent dailyChange = dailyNetChanges.get(iteratorDate);
            if (dailyChange != null) {
                runningConfirmed += dailyChange.confirmedDelta;
                runningWaitlist += dailyChange.waitlistDelta;
                runningCancelled += dailyChange.cancelledDelta;
            }

            // Normalization: Calculate T-Minus days
            int daysBefore = (int) ChronoUnit.DAYS.between(iteratorDate, startDate);
            metrics.add(new EventAnalyticsResponseModel.DailyMetric(daysBefore, iteratorDate.toString(),
                    Math.max(0, runningConfirmed),
                    Math.max(0, runningWaitlist), Math.max(0, runningCancelled)));

            iteratorDate = iteratorDate.plusDays(1);
        }

        // Return sorted by mostly negative (furthest out) to 0 (day of event)
        metrics.sort((m1, m2) -> Integer.compare(m2.getDaysBeforeEvent(), m1.getDaysBeforeEvent()));
        return metrics;
    }

    /**
     * STAGE 3: Aggregate Historical Baselines
     */
    private List<EventAnalyticsResponseModel.DailyMetric> buildAggregatedUsualTimeline(List<Event> pastEvents) {
        // Collect normalized timelines for all past events
        Map<Integer, List<Integer>> dailyConfirmedAgg = new HashMap<>();
        Map<Integer, List<Integer>> dailyWaitlistAgg = new HashMap<>();

        for (Event pastEvent : pastEvents) {
            List<Registration> regs = registrationRepository.findByEventIdAndStatusIn(pastEvent.getId(),
                    Arrays.asList(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED,
                            RegistrationStatus.CANCELLED));
            List<EventAnalyticsResponseModel.DailyMetric> timeline = buildNormalizedTimeline(pastEvent, regs);

            for (EventAnalyticsResponseModel.DailyMetric metric : timeline) {
                dailyConfirmedAgg.computeIfAbsent(metric.getDaysBeforeEvent(), k -> new ArrayList<>())
                        .add(metric.getConfirmed());
                dailyWaitlistAgg.computeIfAbsent(metric.getDaysBeforeEvent(), k -> new ArrayList<>())
                        .add(metric.getWaitlisted());
            }
        }

        // Compute averages safely
        List<EventAnalyticsResponseModel.DailyMetric> usualTimeline = new ArrayList<>();
        for (Integer tMinusDay : dailyConfirmedAgg.keySet()) {
            double avgConf = dailyConfirmedAgg.get(tMinusDay).stream().mapToInt(Integer::intValue).average()
                    .orElse(0.0);
            double avgWait = dailyWaitlistAgg.getOrDefault(tMinusDay, new ArrayList<>()).stream()
                    .mapToInt(Integer::intValue).average().orElse(0.0);

            usualTimeline.add(new EventAnalyticsResponseModel.DailyMetric(tMinusDay, null, (int) Math.round(avgConf),
                    (int) Math.round(avgWait), 0));
        }

        usualTimeline.sort((m1, m2) -> Integer.compare(m2.getDaysBeforeEvent(), m1.getDaysBeforeEvent()));
        return usualTimeline;
    }

    /**
     * STAGE 5 & 6: Forecasting and Metrics
     * Compares this event's velocity against the global average velocity.
     * Forecasts days-to-fill instead of just repeating capacity.
     */
    private void calculateAndSetPerformanceMetrics(Event event,
            List<EventAnalyticsResponseModel.DailyMetric> thisTimeline,
            List<EventAnalyticsResponseModel.DailyMetric> usualTimeline,
            List<Event> otherEvents,
            EventAnalyticsResponseModel response) {

        if (thisTimeline.isEmpty()) {
            response.setConfirmedDeltaVsUsual(0.0);
            response.setWaitlistDeltaVsUsual(0.0);
            response.setCurrentVelocity(0.0);
            response.setPredictedFinalAttendance(0);
            response.setEstimatedDaysToFill(null);
            return;
        }

        EventAnalyticsResponseModel.DailyMetric latestLive = thisTimeline.get(thisTimeline.size() - 1);

        // --- Velocity for THIS event (7-day rolling average) ---
        double thisVelocity = 0.0;
        if (thisTimeline.size() >= 8) {
            EventAnalyticsResponseModel.DailyMetric recent = thisTimeline.get(thisTimeline.size() - 1);
            EventAnalyticsResponseModel.DailyMetric weekAgo = thisTimeline.get(thisTimeline.size() - 8);
            thisVelocity = (recent.getConfirmed() + recent.getWaitlisted()
                    - weekAgo.getConfirmed() - weekAgo.getWaitlisted()) / 7.0;
        } else if (thisTimeline.size() > 1) {
            EventAnalyticsResponseModel.DailyMetric first = thisTimeline.get(0);
            EventAnalyticsResponseModel.DailyMetric last = thisTimeline.get(thisTimeline.size() - 1);
            double totalDays = Math.max(1, thisTimeline.size() - 1);
            thisVelocity = (last.getConfirmed() + last.getWaitlisted()
                    - first.getConfirmed() - first.getWaitlisted()) / totalDays;
        }
        response.setCurrentVelocity(thisVelocity);

        // --- Global average velocity across other events ---
        double totalGlobalVelocity = 0.0;
        int velocityCount = 0;
        for (Event other : otherEvents) {
            List<Registration> otherRegs = registrationRepository.findByEventIdAndStatusIn(other.getId(),
                    Arrays.asList(RegistrationStatus.CONFIRMED, RegistrationStatus.WAITLISTED,
                            RegistrationStatus.CANCELLED));
            List<EventAnalyticsResponseModel.DailyMetric> otherTimeline = buildNormalizedTimeline(other, otherRegs);
            if (otherTimeline.size() > 1) {
                EventAnalyticsResponseModel.DailyMetric oFirst = otherTimeline.get(0);
                EventAnalyticsResponseModel.DailyMetric oLast = otherTimeline.get(otherTimeline.size() - 1);
                double days = Math.max(1, otherTimeline.size() - 1);
                double v = (oLast.getConfirmed() + oLast.getWaitlisted()
                        - oFirst.getConfirmed() - oFirst.getWaitlisted()) / days;
                totalGlobalVelocity += v;
                velocityCount++;
            }
        }
        double avgGlobalVelocity = velocityCount > 0 ? totalGlobalVelocity / velocityCount : 0.0;

        // --- Delta vs usual: how much faster/slower this event is filling compared to
        // the global average ---
        if (avgGlobalVelocity > 0.01) {
            double confirmedDelta = ((thisVelocity - avgGlobalVelocity) / avgGlobalVelocity) * 100;
            response.setConfirmedDeltaVsUsual(confirmedDelta);
        } else {
            response.setConfirmedDeltaVsUsual(thisVelocity > 0 ? 100.0 : 0.0);
        }

        // Waitlist delta: compare waitlist ratio vs global average waitlist ratio
        int thisTotal = latestLive.getConfirmed() + latestLive.getWaitlisted();
        double thisWaitlistRatio = thisTotal > 0 ? (double) latestLive.getWaitlisted() / thisTotal : 0.0;

        // compute global average waitlist ratio
        double totalWaitlistRatio = 0.0;
        int ratioCount = 0;
        for (EventAnalyticsResponseModel.DailyMetric um : usualTimeline) {
            int total = um.getConfirmed() + um.getWaitlisted();
            if (total > 0) {
                totalWaitlistRatio += (double) um.getWaitlisted() / total;
                ratioCount++;
            }
        }
        double avgWaitlistRatio = ratioCount > 0 ? totalWaitlistRatio / ratioCount : 0.0;
        if (avgWaitlistRatio > 0.01) {
            response.setWaitlistDeltaVsUsual(((thisWaitlistRatio - avgWaitlistRatio) / avgWaitlistRatio) * 100);
        } else {
            response.setWaitlistDeltaVsUsual(thisWaitlistRatio > 0 ? 100.0 : 0.0);
        }

        // --- Forecast: estimated days to fill ---
        int currentConfirmed = latestLive.getConfirmed();
        int capacity = event.getMaxCapacity() != null ? event.getMaxCapacity() : 0;
        int remaining = Math.max(0, capacity - currentConfirmed);

        if (thisVelocity > 0.01 && remaining > 0) {
            int daysToFill = (int) Math.ceil(remaining / thisVelocity);
            response.setEstimatedDaysToFill(daysToFill);
        } else if (remaining <= 0) {
            response.setEstimatedDaysToFill(0); // already full
        } else {
            response.setEstimatedDaysToFill(null); // not enough data
        }

        response.setPredictedFinalAttendance(currentConfirmed);
    }

    /**
     * Get all events globally EXCEPT the given eventId, for building the "usual"
     * baseline.
     */
    private List<Event> getAllOtherEvents(Long excludeEventId) {
        return eventRepository.findAll().stream()
                .filter(e -> e.getStartDateTime() != null)
                .filter(e -> !e.getId().equals(excludeEventId))
                .collect(Collectors.toList());
    }

}
