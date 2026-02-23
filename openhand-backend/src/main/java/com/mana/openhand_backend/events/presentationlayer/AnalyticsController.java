package com.mana.openhand_backend.events.presentationlayer;

import com.mana.openhand_backend.events.businesslayer.DataSeederService;
import com.mana.openhand_backend.events.businesslayer.EventAnalyticsService;
import com.mana.openhand_backend.events.domainclientlayer.GlobalAnalyticsResponseModel;
import com.mana.openhand_backend.events.domainclientlayer.EventAnalyticsResponseModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {

    private final DataSeederService dataSeederService;
    private final EventAnalyticsService eventAnalyticsService;

    public AnalyticsController(DataSeederService dataSeederService, EventAnalyticsService eventAnalyticsService) {
        this.dataSeederService = dataSeederService;
        this.eventAnalyticsService = eventAnalyticsService;
    }

    @PostMapping("/seed")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> seedData() {
        String result = dataSeederService.seedMassiveAnalyticsData();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/global")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GlobalAnalyticsResponseModel> getGlobalAnalytics() {
        return ResponseEntity.ok(eventAnalyticsService.getGlobalAnalytics());
    }

    @GetMapping("/compare")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<EventAnalyticsResponseModel>> getCompareAnalytics(@RequestParam List<Long> eventIds) {
        List<EventAnalyticsResponseModel> comparisons = eventIds.stream()
                .map(eventAnalyticsService::getEventAnalytics)
                .collect(Collectors.toList());
        return ResponseEntity.ok(comparisons);
    }
}
