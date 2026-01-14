package com.mana.openhand_backend.identity.businesslayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.utils.InvalidRoleException;
import com.mana.openhand_backend.identity.utils.RoleUtils;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class UserAdminServiceImpl implements UserAdminService {

    private final UserRepository userRepository;

    public UserAdminServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "email"));
    }

    @Override
    public User updateUserRoles(Long userId, Set<String> roles) {
        Set<String> normalizedRoles;
        try {
            normalizedRoles = RoleUtils.normalizeRoles(roles);
        } catch (IllegalArgumentException ex) {
            throw new InvalidRoleException(ex.getMessage());
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.setRoles(normalizedRoles);
        return userRepository.save(user);
    }

    @Override
    public List<String> getAvailableRoles() {
        List<String> roles = new ArrayList<>(RoleUtils.ALLOWED_ROLES);
        roles.sort(String::compareTo);
        return roles;
    }
}
