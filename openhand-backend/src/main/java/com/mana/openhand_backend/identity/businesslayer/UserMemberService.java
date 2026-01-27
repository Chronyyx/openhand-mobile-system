package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;

public interface UserMemberService {

    /**
     * Deactivate the authenticated member account. This marks the account as INACTIVE,
     * records the status change timestamp, and revokes all refresh tokens so existing
     * sessions are terminated.
     *
     * @param userId authenticated user id
     * @return updated user entity
     */
    User deactivateAccount(Long userId);
}
