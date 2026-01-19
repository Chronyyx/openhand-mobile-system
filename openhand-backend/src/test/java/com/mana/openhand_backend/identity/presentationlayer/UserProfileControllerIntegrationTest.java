package com.mana.openhand_backend.identity.presentationlayer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@TestPropertySource(locations = "classpath:application-test.properties")
class UserProfileControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Value("${openhand.app.profile-images.dir}")
    private String profileImagesDir;

    private User testUser;

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        Path storageDir = Paths.get(profileImagesDir).toAbsolutePath().normalize();
        if (Files.exists(storageDir)) {
            try (var walk = Files.walk(storageDir)) {
                walk.sorted((a, b) -> b.compareTo(a))
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (Exception ignored) {
                            }
                        });
            }
        }
        Files.createDirectories(storageDir);

        testUser = new User();
        testUser.setEmail("member@example.com");
        testUser.setPasswordHash("hashedPassword");
        testUser.setRoles(java.util.Set.of("ROLE_MEMBER"));
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "MEMBER")
    void uploadProfilePicture_withValidImage_updatesUserAndStoresFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[512]);

        String responseBody = mockMvc.perform(multipart("/api/users/me/profile-picture").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profileImageUrl").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(responseBody);
        String imageUrl = json.get("profileImageUrl").asText();
        assertThat(imageUrl).startsWith("/uploads/profile-pictures/");

        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getProfileImageUrl()).isEqualTo(imageUrl);

        String filename = imageUrl.substring("/uploads/profile-pictures/".length());
        Path stored = Paths.get(profileImagesDir, filename).toAbsolutePath().normalize();
        assertThat(Files.exists(stored)).isTrue();
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "MEMBER")
    void uploadProfilePicture_withInvalidType_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                new byte[128]);

        mockMvc.perform(multipart("/api/users/me/profile-picture").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid image type."));
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "MEMBER")
    void uploadProfilePicture_withOversizeFile_returnsBadRequest() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.jpg",
                "image/jpeg",
                new byte[2048]);

        mockMvc.perform(multipart("/api/users/me/profile-picture").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("File too large."));
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "MEMBER")
    void uploadProfilePicture_whenInvalidDoesNotReplaceExistingImage() throws Exception {
        MockMultipartFile goodFile = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[512]);

        String responseBody = mockMvc.perform(multipart("/api/users/me/profile-picture").file(goodFile))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String originalUrl = objectMapper.readTree(responseBody).get("profileImageUrl").asText();

        MockMultipartFile badFile = new MockMultipartFile(
                "file",
                "note.txt",
                "text/plain",
                new byte[128]);

        mockMvc.perform(multipart("/api/users/me/profile-picture").file(badFile))
                .andExpect(status().isBadRequest());

        User updated = userRepository.findById(testUser.getId()).orElseThrow();
        assertThat(updated.getProfileImageUrl()).isEqualTo(originalUrl);

        String filename = originalUrl.substring("/uploads/profile-pictures/".length());
        Path stored = Paths.get(profileImagesDir, filename).toAbsolutePath().normalize();
        assertThat(Files.exists(stored)).isTrue();
    }

    @Test
    void uploadProfilePicture_withoutAuthentication_returnsUnauthorized() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[64]);

        mockMvc.perform(multipart("/api/users/me/profile-picture").file(file))
                .andExpect(status().isUnauthorized());
    }
}
