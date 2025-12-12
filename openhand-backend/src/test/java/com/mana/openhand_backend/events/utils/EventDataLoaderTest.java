package com.mana.openhand_backend.events.utils;

import com.mana.openhand_backend.events.dataaccesslayer.Event;
import com.mana.openhand_backend.events.dataaccesslayer.EventRepository;
import com.mana.openhand_backend.registrations.dataaccesslayer.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventDataLoaderTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private RegistrationRepository registrationRepository;

    @InjectMocks
    private EventDataLoader eventDataLoader;

    @Test
    void run_whenNoEvents_seedsInitialEvents() throws Exception {
        when(eventRepository.count()).thenReturn(0L);

        // act
        eventDataLoader.run();

        // assert
        verify(eventRepository).count();
        // five events are created in the loader
        verify(eventRepository, times(5)).save(any(Event.class));
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(registrationRepository);
    }

    @Test
    void run_whenEventsExist_doesNothing() throws Exception {
        when(eventRepository.count()).thenReturn(1L);

        eventDataLoader.run();

        verify(eventRepository).count();
        verifyNoMoreInteractions(eventRepository);
        verifyNoInteractions(registrationRepository);
    }
}
