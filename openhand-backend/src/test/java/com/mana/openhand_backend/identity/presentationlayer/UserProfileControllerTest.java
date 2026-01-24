package com.mana.openhand_backend.identity.presentationlayer;

import com.mana.openhand_backend.identity.businesslayer.ProfilePictureService;
import com.mana.openhand_backend.identity.presentationlayer.payload.ProfilePictureResponse;
import com.mana.openhand_backend.security.services.UserDetailsImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProfilePictureService profilePictureService;

    @BeforeEach
    void setUpSecurityContext() {
        UserDetailsImpl userDetails = org.mockito.Mockito.mock(UserDetailsImpl.class);
        when(userDetails.getId()).thenReturn(5L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userDetails, null, java.util.List.of())
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getProfilePicture_returnsUrl() throws Exception {
        when(profilePictureService.getProfilePicture(eq(5L), any(String.class)))
                .thenReturn(new ProfilePictureResponse("http://localhost/uploads/profile-pictures/pic.png"));

        mockMvc.perform(get("/api/users/profile-picture"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://localhost/uploads/profile-pictures/pic.png"));
    }

    @Test
    void uploadProfilePicture_returnsResponse() throws Exception {
        when(profilePictureService.storeProfilePicture(eq(5L), any(MultipartFile.class), any(String.class)))
                .thenReturn(new ProfilePictureResponse("http://localhost/uploads/profile-pictures/new.png"));

        mockMvc.perform(multipart("/api/users/profile-picture")
                        .file("file", "content".getBytes()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("http://localhost/uploads/profile-pictures/new.png"));
    }

    @Test
    void uploadProfilePicture_whenInvalid_returnsBadRequest() throws Exception {
        when(profilePictureService.storeProfilePicture(eq(5L), any(MultipartFile.class), any(String.class)))
                .thenThrow(new IllegalArgumentException("bad"));

        mockMvc.perform(multipart("/api/users/profile-picture")
                        .file("file", "content".getBytes()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Error: bad"));
    }

    @Test
    void uploadProfilePicture_whenUnexpectedError_returnsServerError() throws Exception {
        when(profilePictureService.storeProfilePicture(eq(5L), any(MultipartFile.class), any(String.class)))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(multipart("/api/users/profile-picture")
                        .file("file", "content".getBytes()))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error: Unable to upload profile picture."));
    }
}
