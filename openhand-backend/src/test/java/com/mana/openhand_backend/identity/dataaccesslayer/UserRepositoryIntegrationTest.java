package com.mana.openhand_backend.identity.dataaccesslayer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@TestPropertySource(properties = {
    "spring.sql.init.mode=never"
})
class UserRepositoryIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void saveAndLoad_preservesMemberStatusAndTimestamp() {
        // arrange
        User user = new User();
        user.setEmail("repo@test.com");
        user.setPasswordHash("hash");
        user.setRoles(Set.of("ROLE_MEMBER"));
        user.setMemberStatus(MemberStatus.INACTIVE);

        // act
        User saved = userRepository.save(user);
        User found = userRepository.findById(saved.getId()).orElseThrow();

        // assert
        assertThat(found.getMemberStatus()).isEqualTo(MemberStatus.INACTIVE);
        assertThat(found.getStatusChangedAt()).isNull();
    }

    @Test
    void updateMemberStatus_fromActiveToInactive_persists() {
        // arrange
        User user = new User();
        user.setEmail("deactivate@test.com");
        user.setPasswordHash("hash");
        user.setRoles(new HashSet<>(Set.of("ROLE_MEMBER")));
        user.setMemberStatus(MemberStatus.ACTIVE);
        User saved = userRepository.save(user);

        // act
        saved.setMemberStatus(MemberStatus.INACTIVE);
        saved.setStatusChangedAt(java.time.LocalDateTime.now());
        User updated = userRepository.save(saved);

        // assert
        User reloaded = userRepository.findById(updated.getId()).orElseThrow();
        assertThat(reloaded.getMemberStatus()).isEqualTo(MemberStatus.INACTIVE);
        assertThat(reloaded.getStatusChangedAt()).isNotNull();
    }

    @Test
    void findByEmail_returnsUserWithCorrectStatus() {
        // arrange
        User user = new User();
        user.setEmail("status@test.com");
        user.setPasswordHash("hash");
        user.setRoles(new HashSet<>(Set.of("ROLE_MEMBER")));
        user.setMemberStatus(MemberStatus.INACTIVE);
        userRepository.save(user);

        // act
        User found = userRepository.findByEmail("status@test.com").orElseThrow();

        // assert
        assertThat(found.getMemberStatus()).isEqualTo(MemberStatus.INACTIVE);
    }
}
