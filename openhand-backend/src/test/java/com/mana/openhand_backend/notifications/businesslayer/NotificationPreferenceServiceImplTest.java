package com.mana.openhand_backend.notifications.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreference;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreferenceRepository;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceItemRequestModel;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceUpdateRequestModel;
import com.mana.openhand_backend.notifications.utils.InvalidNotificationPreferenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceImplTest {

    @Mock
    private NotificationPreferenceRepository preferenceRepository;

    @Mock
    private UserRepository userRepository;

    private NotificationPreferenceServiceImpl preferenceService;

    @BeforeEach
    void setUp() {
        preferenceService = new NotificationPreferenceServiceImpl(preferenceRepository, userRepository);
    }

    @Test
    void updatePreferences_disableCriticalCategory_throwsBadRequest() {
        Long userId = 7L;
        User user = mock(User.class);
        NotificationPreference preference = new NotificationPreference(user);

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        NotificationPreferenceUpdateRequestModel request = new NotificationPreferenceUpdateRequestModel(
                List.of(new NotificationPreferenceItemRequestModel("CANCELLATION", false))
        );

        assertThrows(InvalidNotificationPreferenceException.class,
                () -> preferenceService.updatePreferences(userId, request));
    }

    @Test
    void updatePreferences_unknownCategory_throwsBadRequest() {
        Long userId = 7L;
        User user = mock(User.class);
        NotificationPreference preference = new NotificationPreference(user);

        when(preferenceRepository.findByUserId(userId)).thenReturn(Optional.of(preference));

        NotificationPreferenceUpdateRequestModel request = new NotificationPreferenceUpdateRequestModel(
                List.of(new NotificationPreferenceItemRequestModel("UNKNOWN", true))
        );

        assertThrows(InvalidNotificationPreferenceException.class,
                () -> preferenceService.updatePreferences(userId, request));
    }
}
