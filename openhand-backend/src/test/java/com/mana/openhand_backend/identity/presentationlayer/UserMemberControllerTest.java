package com.mana.openhand_backend.identity.presentationlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mana.openhand_backend.identity.businesslayer.UserMemberService;
import com.mana.openhand_backend.identity.dataaccesslayer.MemberStatus;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.domainclientlayer.UserResponseModel;
import com.mana.openhand_backend.security.services.UserDetailsImpl;

import java.util.Set;

@ExtendWith(MockitoExtension.class)
class UserMemberControllerTest {

    @Mock
    private UserMemberService userMemberService;

    @InjectMocks
    private UserMemberController controller;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deactivateAccount_returnsUpdatedUser() {
        // arrange
        User user = new User();
        user.setId(1L);
        user.setEmail("member@test.com");
        user.setRoles(Set.of("ROLE_MEMBER"));
        user.setMemberStatus(MemberStatus.INACTIVE);
        UserDetailsImpl principal = UserDetailsImpl.build(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(userMemberService.deactivateAccount(1L)).thenReturn(user);

        // act
        ResponseEntity<UserResponseModel> response = controller.deactivateAccount();

        // assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMemberStatus()).isEqualTo(MemberStatus.INACTIVE.name());
    }
}
