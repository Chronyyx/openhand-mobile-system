package com.mana.openhand_backend.identity.presentationlayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.domainclientlayer.UserResponseModel;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/employee/users")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
public class UserEmployeeController {

    private final UserRepository userRepository;

    public UserEmployeeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Simple search endpoint for employees to look up participants by email or phone number.
     * If the query contains '@', treat it as an email; otherwise attempt a phone number match
     * (normalized to digits and '+'). Returns 0 or 1 result for exact match to keep behavior predictable.
     */
    @GetMapping("/search")
    public List<UserResponseModel> searchUsers(@RequestParam("query") String query) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String trimmed = query.trim();
        Optional<User> found;

        if (trimmed.contains("@")) {
            found = userRepository.findByEmail(trimmed);
        } else {
            String normalizedPhone = trimmed.replaceAll("[^0-9+]", "");
            found = userRepository.findByPhoneNumber(normalizedPhone);
        }

        return found.map(user -> List.of(UserResponseModel.fromEntity(user)))
                .orElseGet(List::of);
    }
}
