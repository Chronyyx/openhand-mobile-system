package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.presentationlayer.payload.UpdateUserProfileRequest;
import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;

import java.util.List;
import java.util.Set;

public interface UserAdminService {

    List<User> getAllUsers();

    User updateUserRoles(Long userId, Set<String> roles);

    User updateUserProfile(Long userId, UpdateUserProfileRequest request);

    User updateUserStatus(Long userId, MemberStatus status);

    List<String> getAvailableRoles();
}
