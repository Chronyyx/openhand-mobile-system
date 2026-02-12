package com.mana.openhand_backend.donations.presentationlayer;

import com.mana.openhand_backend.donations.businesslayer.DonationService;
import com.mana.openhand_backend.donations.domainclientlayer.DonationSummaryResponseModel;
import com.mana.openhand_backend.donations.domainclientlayer.ManualDonationRequestModel;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/employee/donations")
@PreAuthorize("hasRole('ROLE_EMPLOYEE') or hasRole('ROLE_ADMIN')")
public class DonationManagementController {

    private final DonationService donationService;
    private final UserRepository userRepository;

    public DonationManagementController(DonationService donationService, UserRepository userRepository) {
        this.donationService = donationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public List<DonationSummaryResponseModel> getDonationsForStaff() {
        return donationService.getDonationsForStaff();
    }

    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.CREATED)
    public DonationSummaryResponseModel createManualDonation(
            @Valid @RequestBody ManualDonationRequestModel request,
            @RequestParam Long donorId,
            Authentication authentication) {
        // For @WebMvcTest compatibility, try authentication parameter first, then SecurityContextHolder
        if (authentication == null) {
            authentication = SecurityContextHolder.getContext().getAuthentication();
        }
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not authenticated");
        }
        Long employeeId = extractUserIdFromAuth(authentication);
        return donationService.createManualDonation(employeeId, donorId, request);
    }

    private Long extractUserIdFromAuth(Authentication authentication) {
        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            return ((User) principal).getId();
        }

        if (principal instanceof UserDetails) {
            UserDetails userDetails = (UserDetails) principal;
            User user = userRepository.findByEmail(userDetails.getUsername())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "User not found with email: " + userDetails.getUsername()
                    ));
            return user.getId();
        }

        throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Unsupported authentication principal"
        );
    }
}
