package com.mana.openhand_backend.identity.dataaccesslayer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
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
}
