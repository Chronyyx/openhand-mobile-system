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
    void run_deletesAllAndSavesInitialEvents() throws Exception {
        // act
        eventDataLoader.run();

        // assert
        verify(registrationRepository, times(1)).deleteAll();
        verify(eventRepository, times(1)).deleteAll();
        // four events are created in the loader
        verify(eventRepository, times(4)).save(any(Event.class));
        verifyNoMoreInteractions(eventRepository);
        verifyNoMoreInteractions(registrationRepository);
    }
}
