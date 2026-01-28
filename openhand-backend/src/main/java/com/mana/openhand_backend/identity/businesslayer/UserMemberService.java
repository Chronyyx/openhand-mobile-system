package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;

public interface UserMemberService {

    /**
     * Get profile of the authenticated member.
     *
     * @param userId authenticated user id
     * @return user entity
     */
    User getProfile(Long userId);

    /**
     * Update profile of the authenticated member.
     *
     * @param userId  authenticated user id
     * @param request profile update request
     * @return updated user entity
     */
    User updateProfile(Long userId,
            com.mana.openhand_backend.identity.presentationlayer.payload.ProfileRequest request);

    /**
     * Deactivate the authenticated member account. This marks the account as
     * INACTIVE,
     * records the status change timestamp, and revokes all refresh tokens so
     * existing
     * sessions are terminated.
     *
     * @param userId authenticated user id
     * @return updated user entity
     */
    User deactivateAccount(Long userId);
}
