package com.mana.openhand_backend.identity.presentationlayer;

import com.mana.openhand_backend.identity.businesslayer.UserAdminService;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.domainclientlayer.UserResponseModel;
import com.mana.openhand_backend.identity.presentationlayer.payload.UpdateUserRolesRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAdminControllerTest {

    @Mock
    private UserAdminService userAdminService;

    private UserAdminController controller;

    @BeforeEach
    void setUp() {
        controller = new UserAdminController(userAdminService);
    }

    // ---------------- getAllUsers ----------------

    @Test
    void getAllUsers_returnsMappedResponseModels() {
        User user1 = new User();
        User user2 = new User();

        when(userAdminService.getAllUsers()).thenReturn(List.of(user1, user2));

        List<UserResponseModel> result = controller.getAllUsers();

        verify(userAdminService).getAllUsers();
        assertNotNull(result);
        assertEquals(2, result.size());   // mapping done via UserResponseModel::fromEntity
    }

    // ---------------- getAvailableRoles ----------------

    @Test
    void getAvailableRoles_delegatesToService() {
        List<String> roles = List.of("ROLE_ADMIN", "ROLE_MEMBER");
        when(userAdminService.getAvailableRoles()).thenReturn(roles);

        List<String> result = controller.getAvailableRoles();

        verify(userAdminService).getAvailableRoles();
        assertEquals(roles, result);
    }

    // ---------------- updateUserRoles ----------------

    @Test
    void updateUserRoles_callsServiceAndReturnsMappedResponse() {
        Long id = 1L;
        Set<String> roles = Set.of("ROLE_MEMBER");

        UpdateUserRolesRequest request = mock(UpdateUserRolesRequest.class);
        when(request.getRoles()).thenReturn(roles);

        User updatedUser = new User();
        when(userAdminService.updateUserRoles(id, roles)).thenReturn(updatedUser);

        UserResponseModel response = controller.updateUserRoles(id, request);

        verify(userAdminService).updateUserRoles(id, roles);
        assertNotNull(response); // created via UserResponseModel.fromEntity(updatedUser)
    }
}
