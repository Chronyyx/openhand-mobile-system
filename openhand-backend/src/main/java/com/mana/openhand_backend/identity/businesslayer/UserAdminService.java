package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;

import java.util.List;
import java.util.Set;

public interface UserAdminService {

    List<User> getAllUsers();

    User updateUserRoles(Long userId, Set<String> roles);

    List<String> getAvailableRoles();
}
