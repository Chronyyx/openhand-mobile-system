package com.mana.openhand_backend.identity.presentationlayer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
class AccountControllerTest {

    @Mock
    private UserMemberService userMemberService;

    @InjectMocks
    private AccountController controller;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deactivateAccount_callsService_andReturnsUpdatedUser() {
        // arrange
        User user = new User();
        user.setId(3L);
        user.setEmail("account@test.com");
        user.setRoles(Set.of("ROLE_MEMBER"));
        user.setMemberStatus(MemberStatus.ACTIVE);

        User deactivated = new User();
        deactivated.setId(3L);
        deactivated.setEmail("account@test.com");
        deactivated.setRoles(Set.of("ROLE_MEMBER"));
        deactivated.setMemberStatus(MemberStatus.INACTIVE);

        UserDetailsImpl principal = UserDetailsImpl.build(user);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));

        when(userMemberService.deactivateAccount(3L)).thenReturn(deactivated);

        // act
        ResponseEntity<UserResponseModel> response = controller.deactivateAccount();

        // assert
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMemberStatus()).isEqualTo(MemberStatus.INACTIVE.name());
        verify(userMemberService).deactivateAccount(3L);
    }
}
