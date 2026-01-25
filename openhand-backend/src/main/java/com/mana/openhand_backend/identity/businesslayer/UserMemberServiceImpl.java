package com.mana.openhand_backend.identity.businesslayer;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.utils.UserNotFoundException;
import com.mana.openhand_backend.security.services.RefreshTokenService;

@Service
public class UserMemberServiceImpl implements UserMemberService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;

    public UserMemberServiceImpl(UserRepository userRepository, RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public User deactivateAccount(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));

        if (user.getMemberStatus() == MemberStatus.INACTIVE) {
            return user;
        }

        user.setMemberStatus(MemberStatus.INACTIVE);
        user.setStatusChangedAt(LocalDateTime.now());

        // Revoke all refresh tokens to ensure any active sessions are closed.
        refreshTokenService.deleteByUserId(userId);

        return userRepository.save(user);
    }
}
