package com.mana.openhand_backend.identity.presentationlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.HashSet;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = true)
@TestPropertySource(locations = "classpath:application-test.properties")
class UserEmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    @Transactional
    void setUp() {
        userRepository.deleteAll();

        testUser = new User();
        testUser.setEmail("employee@example.com");
        testUser.setPhoneNumber("5141234567");
        testUser.setPasswordHash("hashedPassword");
        testUser.setRoles(new HashSet<>());
        testUser = userRepository.save(testUser);
    }

    @Test
    @WithMockUser(username = "actor@example.com", roles = "EMPLOYEE")
    void searchUsers_withValidEmail_shouldReturn200WithUser() throws Exception {
        // Arrange
        String query = "employee@example.com";

        // Act & Assert
        mockMvc.perform(get("/api/employee/users/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", equalTo(testUser.getId().intValue())))
                .andExpect(jsonPath("$[0].email", equalTo("employee@example.com")));
    }

    @Test
    @WithMockUser(username = "actor@example.com", roles = "EMPLOYEE")
    void searchUsers_withInvalidEmail_shouldReturn200WithEmptyList() throws Exception {
        // Arrange
        String query = "nonexistent@example.com";

        // Act & Assert
        mockMvc.perform(get("/api/employee/users/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "actor@example.com", roles = "EMPLOYEE")
    void searchUsers_withValidPhone_shouldReturn200WithUser() throws Exception {
        // Arrange
        String query = "514-123-4567";

        // Act & Assert
        mockMvc.perform(get("/api/employee/users/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email", equalTo("employee@example.com")));
    }

    @Test
    @WithMockUser(username = "actor@example.com", roles = "EMPLOYEE")
    void searchUsers_withPhoneContainingCountryCode_shouldReturn200WithUser() throws Exception {
        // Arrange
        String query = "514-123-4567";

        // Act & Assert
        mockMvc.perform(get("/api/employee/users/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email", equalTo("employee@example.com")));
    }

    @Test
    @WithMockUser(username = "actor@example.com", roles = "EMPLOYEE")
    void searchUsers_withInvalidPhone_shouldReturn200WithEmptyList() throws Exception {
        // Arrange
        String query = "999-999-9999";

        // Act & Assert
        mockMvc.perform(get("/api/employee/users/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "actor@example.com", roles = "EMPLOYEE")
    void searchUsers_withEmptyQuery_shouldReturn200WithEmptyList() throws Exception {
        // Arrange
        String query = "";

        // Act & Assert
        mockMvc.perform(get("/api/employee/users/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "actor@example.com", roles = "EMPLOYEE")
    void searchUsers_withWhitespaceOnlyQuery_shouldReturn200WithEmptyList() throws Exception {
        // Arrange
        String query = "   ";

        // Act & Assert
        mockMvc.perform(get("/api/employee/users/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void searchUsers_withoutAuthentication_shouldReturn403Forbidden() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/employee/users/search")
                        .param("query", "employee@example.com"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "member@example.com", roles = "MEMBER")
    void searchUsers_withNonEmployeeRole_shouldReturn403Forbidden() throws Exception {
        // Arrange
        String query = "employee@example.com";

        // Act & Assert
        mockMvc.perform(get("/api/employee/users/search")
                        .param("query", query))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "actor@example.com", roles = "ADMIN")
    void searchUsers_withAdminRole_shouldSucceed() throws Exception {
        // Arrange
        String query = "employee@example.com";

        // Act & Assert
        mockMvc.perform(get("/api/employee/users/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email", equalTo("employee@example.com")));
    }

    @Test
    @WithMockUser(username = "actor@example.com", roles = "EMPLOYEE")
    void searchUsers_withEmailHavingLeadingTrailingWhitespace_shouldTrimAndFind() throws Exception {
        // Arrange
        String query = "  employee@example.com  ";

        // Act & Assert
        mockMvc.perform(get("/api/employee/users/search")
                        .param("query", query))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email", equalTo("employee@example.com")));
    }
}
