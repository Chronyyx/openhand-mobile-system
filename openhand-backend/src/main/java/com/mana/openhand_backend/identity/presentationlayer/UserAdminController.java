package com.mana.openhand_backend.identity.presentationlayer;

import com.mana.openhand_backend.identity.businesslayer.UserAdminService;
import com.mana.openhand_backend.identity.domainclientlayer.UserResponseModel;
import com.mana.openhand_backend.identity.presentationlayer.payload.UpdateUserRolesRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {

    private final UserAdminService userAdminService;

    public UserAdminController(UserAdminService userAdminService) {
        this.userAdminService = userAdminService;
    }

    @GetMapping
    public List<UserResponseModel> getAllUsers() {
        return userAdminService.getAllUsers().stream()
                .map(UserResponseModel::fromEntity)
                .collect(Collectors.toList());
    }

    @GetMapping("/roles")
    public List<String> getAvailableRoles() {
        return userAdminService.getAvailableRoles();
    }

    @PutMapping("/{id}/roles")
    public UserResponseModel updateUserRoles(@PathVariable Long id,
                                             @Valid @RequestBody UpdateUserRolesRequest request) {
        return UserResponseModel.fromEntity(
                userAdminService.updateUserRoles(id, request.getRoles())
        );
    }
}
