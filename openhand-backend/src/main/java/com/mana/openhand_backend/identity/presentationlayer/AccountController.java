package com.mana.openhand_backend.identity.presentationlayer;

import com.mana.openhand_backend.identity.businesslayer.UserMemberService;
import com.mana.openhand_backend.identity.domainclientlayer.UserResponseModel;
import com.mana.openhand_backend.security.services.UserDetailsImpl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/account")
@PreAuthorize("hasRole('ROLE_MEMBER')")
public class AccountController {

    private final UserMemberService userMemberService;

    public AccountController(UserMemberService userMemberService) {
        this.userMemberService = userMemberService;
    }

    @PostMapping("/deactivate")
    public ResponseEntity<UserResponseModel> deactivateAccount() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetailsImpl principal = (UserDetailsImpl) authentication.getPrincipal();

        var updated = userMemberService.deactivateAccount(principal.getId());
        return ResponseEntity.ok(UserResponseModel.fromEntity(updated));
    }
}
