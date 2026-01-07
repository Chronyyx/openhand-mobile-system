package com.mana.openhand_backend.events.utils;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.events.dataaccesslayer.EventStatus;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Seeds sample event data into the database on application startup.
 * Uses translation key identifiers for titles and descriptions that can be translated by the frontend.
 * The frontend maps these identifiers to proper translations in all supported languages (en/fr/es).
 */
@Component
public class EventDataLoader implements CommandLineRunner {

        private final EventRepository eventRepository;
        @SuppressWarnings("unused")
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

                // Event 1: MANA Recognition Gala
                // Frontend will translate using key "gala" from events.names.gala and events.descriptions.gala
                Event gala2025 = new Event(
                                "gala",  // Translation key identifier
                                "gala_description",  // Translation key identifier for description
                                now.plusDays(3).withHour(18).withMinute(0),
                                now.plusDays(3).withHour(22).withMinute(0),
                                "Maison d’Accueil des Nouveaux Arrivants MANA",
                                "Montréal, Québec",
                                EventStatus.FULL,
                                200,
                                200,
                                "GALA");

                // Event 2: Food Distribution - Tuesday
                Event foodDistribution1 = new Event(
                                "distribution_mardi",
                                "distribution_mardi_description",
                                now.plusDays(5).withHour(8).withMinute(0),
                                now.plusDays(5).withHour(12).withMinute(0),
                                "Centre MANA",
                                "1910 Boulevard René-Lévesque, Montréal, QC",
                                EventStatus.OPEN,
                                100,
                                40,
                                "DISTRIBUTION");

                // Event 3: Food Distribution - Thursday
                Event foodDistribution2 = new Event(
                                "distribution_jeudi",
                                "distribution_jeudi_description",
                                now.plusDays(7).withHour(8).withMinute(0),
                                now.plusDays(7).withHour(12).withMinute(0),
                                "Centre MANA",
                                "1910 Boulevard René-Lévesque, Montréal, QC",
                                EventStatus.NEARLY_FULL,
                                100,
                                85,
                                "DISTRIBUTION");

                // Event 4: MANA Training - Intercultural Mediator
                Event mediatorTraining = new Event(
                                "formation_mediateur",
                                "formation_mediateur_description",
                                now.plusDays(10).withHour(18).withMinute(30),
                                now.plusDays(10).withHour(21).withMinute(0),
                                "Maison d’Accueil des Nouveaux Arrivants MANA",
                                "Montréal, Québec",
                                EventStatus.OPEN,
                                60,
                                20,
                                "FORMATION");

                // Event 5: MANA Christmas Basket 2025
                Event christmasBasketEvent = new Event(
                                "panier_noel",
                                "panier_noel_description",
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
