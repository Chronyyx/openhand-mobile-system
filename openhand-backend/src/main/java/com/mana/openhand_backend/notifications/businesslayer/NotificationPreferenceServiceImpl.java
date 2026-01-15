package com.mana.openhand_backend.notifications.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreference;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationPreferenceRepository;
import com.mana.openhand_backend.notifications.dataaccesslayer.NotificationType;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceCategory;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceItemRequestModel;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceResponseModel;
import com.mana.openhand_backend.notifications.domainclientlayer.NotificationPreferenceUpdateRequestModel;
import com.mana.openhand_backend.notifications.utils.InvalidNotificationPreferenceException;
import com.mana.openhand_backend.notifications.utils.NotificationPreferencePolicy;
import com.mana.openhand_backend.notifications.utils.NotificationPreferenceResponseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    public NotificationPreferenceServiceImpl(NotificationPreferenceRepository preferenceRepository,
                                             UserRepository userRepository) {
        this.preferenceRepository = preferenceRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public NotificationPreferenceResponseModel getPreferencesForUser(Long userId) {
        NotificationPreference preference = getOrCreatePreferences(userId);
        return NotificationPreferenceResponseMapper.toResponseModel(userId, preference);
    }

    @Override
    @Transactional
    public NotificationPreferenceResponseModel updatePreferences(Long userId, NotificationPreferenceUpdateRequestModel request) {
        NotificationPreference preference = getOrCreatePreferences(userId);

        if (request != null && request.getPreferences() != null) {
            applyUpdates(preference, request.getPreferences());
        }

        enforceCriticalCategories(preference);
        NotificationPreference saved = preferenceRepository.save(preference);
        return NotificationPreferenceResponseMapper.toResponseModel(userId, saved);
    }

    @Override
    @Transactional
    public boolean isNotificationEnabled(Long userId, NotificationType notificationType) {
        NotificationPreferenceCategory category = NotificationPreferencePolicy.toCategory(notificationType);
        if (NotificationPreferencePolicy.isCritical(category)) {
            return true;
        }

        NotificationPreference preference = getOrCreatePreferences(userId);
        return NotificationPreferencePolicy.isEnabled(preference, category);
    }

    private NotificationPreference getOrCreatePreferences(Long userId) {
        return preferenceRepository.findByUserId(userId)
                .orElseGet(() -> preferenceRepository.save(new NotificationPreference(loadUser(userId))));
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private void applyUpdates(NotificationPreference preference, List<NotificationPreferenceItemRequestModel> updates) {
        for (NotificationPreferenceItemRequestModel update : updates) {
            NotificationPreferenceCategory category = parseCategory(update.getCategory());
            if (NotificationPreferencePolicy.isCritical(category) && !update.isEnabled()) {
                throw new InvalidNotificationPreferenceException(
                        "Category " + category.name() + " is critical and cannot be disabled."
                );
            }
            NotificationPreferencePolicy.applyEnabled(preference, category, update.isEnabled());
        }
    }

    private NotificationPreferenceCategory parseCategory(String rawCategory) {
        if (rawCategory == null || rawCategory.isBlank()) {
            throw new InvalidNotificationPreferenceException("Category is required.");
        }
        try {
            return NotificationPreferenceCategory.valueOf(rawCategory);
        } catch (IllegalArgumentException ex) {
            throw new InvalidNotificationPreferenceException("Unknown category: " + rawCategory);
        }
    }

    private void enforceCriticalCategories(NotificationPreference preference) {
        for (NotificationPreferenceCategory category : NotificationPreferenceCategory.values()) {
            if (NotificationPreferencePolicy.isCritical(category)) {
                NotificationPreferencePolicy.applyEnabled(preference, category, true);
            }
        }
    }
}
