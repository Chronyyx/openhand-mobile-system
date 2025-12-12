package com.mana.openhand_backend.events.utils;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EventDataLoader implements CommandLineRunner {

        private final EventRepository eventRepository;
        private final RegistrationRepository registrationRepository;

        public EventDataLoader(EventRepository eventRepository,
                        RegistrationRepository registrationRepository) {
                this.eventRepository = eventRepository;
                this.registrationRepository = registrationRepository;
        }

        @Override
        public void run(String... args) {
                // Only seed initial events for fresh databases.
                // Do not wipe existing data (otherwise admin-created events disappear on restart).
                if (eventRepository.count() > 0) {
                        return;
                }

                LocalDateTime now = LocalDateTime.now();

                Event gala2025 = new Event(
                                "Gala de reconnaissance MANA",
                                "Soirée de reconnaissance du patrimoine interculturel MANA.",
                                now.plusDays(3).withHour(18).withMinute(0),
                                now.plusDays(3).withHour(22).withMinute(0),
                                "Maison d’Accueil des Nouveaux Arrivants MANA",
                                "Montréal, Québec",
                                EventStatus.FULL,
                                200,
                                200,
                                "GALA");

                Event foodDistribution1 = new Event(
                                "Distribution Alimentaire - Mardi",
                                "Distribution alimentaire hebdomadaire pour les familles.",
                                now.plusDays(5).withHour(8).withMinute(0),
                                now.plusDays(5).withHour(12).withMinute(0),
                                "Centre MANA",
                                "1910 Boulevard René-Lévesque, Montréal, QC",
                                EventStatus.OPEN,
                                100,
                                40,
                                "DISTRIBUTION");

                Event foodDistribution2 = new Event(
                                "Distribution Alimentaire - Jeudi",
                                "Distribution alimentaire hebdomadaire pour les familles.",
                                now.plusDays(7).withHour(8).withMinute(0),
                                now.plusDays(7).withHour(12).withMinute(0),
                                "Centre MANA",
                                "1910 Boulevard René-Lévesque, Montréal, QC",
                                EventStatus.NEARLY_FULL,
                                100,
                                85,
                                "DISTRIBUTION");

                Event mediatorTraining = new Event(
                                "Formation MANA – Médiateur interculturel",
                                "Formation gratuite pour devenir médiateur interculturel.",
                                now.plusDays(10).withHour(18).withMinute(30),
                                now.plusDays(10).withHour(21).withMinute(0),
                                "Maison d’Accueil des Nouveaux Arrivants MANA",
                                "Montréal, Québec",
                                EventStatus.OPEN,
                                60,
                                20,
                                "FORMATION");

                Event christmasBasketEvent = new Event(
                                "Panier Noël Mana 2025",
                                "Le Panier de Noël MANA offre aux familles un panier de denrées non périssables et d’articles d’hygiène distribué à Noël.",
                                now.plusDays(6).withHour(14).withMinute(0),
                                now.plusDays(6).withHour(17).withMinute(0),
                                "Centre Communautaire MANA",
                                "Montréal, Québec",
                                EventStatus.OPEN,
                                100,
                                79,
                                "ATELIER");

                eventRepository.save(gala2025);
                eventRepository.save(foodDistribution1);
                eventRepository.save(foodDistribution2);
                eventRepository.save(mediatorTraining);
                eventRepository.save(christmasBasketEvent);
        }
}
