package com.mana.openhand_backend.identity.presentationlayer;

import com.mana.openhand_backend.identity.dataaccesslayer.User;
import com.mana.openhand_backend.identity.dataaccesslayer.UserRepository;
import com.mana.openhand_backend.identity.domainclientlayer.UserResponseModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserEmployeeControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserEmployeeController userEmployeeController;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("user@example.com");
    }

    @Test
    void searchUsers_withValidEmail_shouldReturnUser() {
        // Arrange
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

        // Act
        List<UserResponseModel> results = userEmployeeController.searchUsers("user@example.com");

        // Assert
        assertEquals(1, results.size());
        assertEquals("user@example.com", results.get(0).getEmail());
        verify(userRepository).findByEmail("user@example.com");
    }

    @Test
    void searchUsers_withInvalidEmail_shouldReturnEmptyList() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        List<UserResponseModel> results = userEmployeeController.searchUsers("nonexistent@example.com");

        // Assert
        assertEquals(0, results.size());
        verify(userRepository).findByEmail("nonexistent@example.com");
    }

    @Test
    void searchUsers_withValidPhone_shouldReturnUser() {
        // Arrange
        String normalizedPhone = "5141234567";
        when(userRepository.findByPhoneNumber(normalizedPhone)).thenReturn(Optional.of(testUser));

        // Act
        List<UserResponseModel> results = userEmployeeController.searchUsers("514-123-4567");

        // Assert
        assertEquals(1, results.size());
        assertEquals("user@example.com", results.get(0).getEmail());
        verify(userRepository).findByPhoneNumber(normalizedPhone);
    }

    @Test
    void searchUsers_withInvalidPhone_shouldReturnEmptyList() {
        // Arrange
        when(userRepository.findByPhoneNumber("9999999999")).thenReturn(Optional.empty());

        // Act
        List<UserResponseModel> results = userEmployeeController.searchUsers("999-999-9999");

        // Assert
        assertEquals(0, results.size());
        verify(userRepository).findByPhoneNumber("9999999999");
    }

    @Test
    void searchUsers_withPhoneContainingCountryCode_shouldNormalizeAndSearch() {
        // Arrange
        String normalizedPhone = "+15141234567";
        when(userRepository.findByPhoneNumber(normalizedPhone)).thenReturn(Optional.of(testUser));

        // Act
        List<UserResponseModel> results = userEmployeeController.searchUsers("+1 (514) 123-4567");

        // Assert
        assertEquals(1, results.size());
        verify(userRepository).findByPhoneNumber(normalizedPhone);
    }

    @Test
    void searchUsers_withEmptyQuery_shouldReturnEmptyList() {
        // Arrange
        String query = "";

        // Act
        List<UserResponseModel> results = userEmployeeController.searchUsers(query);

        // Assert
        assertEquals(0, results.size());
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).findByPhoneNumber(anyString());
    }

    @Test
    void searchUsers_withWhitespaceOnlyQuery_shouldReturnEmptyList() {
        // Arrange
        String query = "   ";

        // Act
        List<UserResponseModel> results = userEmployeeController.searchUsers(query);

        // Assert
        assertEquals(0, results.size());
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).findByPhoneNumber(anyString());
    }

    @Test
    void searchUsers_withNullQuery_shouldReturnEmptyList() {
        // Arrange
        String query = null;

        // Act
        List<UserResponseModel> results = userEmployeeController.searchUsers(query);

        // Assert
        assertEquals(0, results.size());
        verify(userRepository, never()).findByEmail(anyString());
        verify(userRepository, never()).findByPhoneNumber(anyString());
    }

    @Test
    void searchUsers_withEmailContainingAtSign_shouldTreatAsEmail() {
        // Arrange
        when(userRepository.findByEmail("user+tag@example.com")).thenReturn(Optional.of(testUser));

        // Act
        List<UserResponseModel> results = userEmployeeController.searchUsers("user+tag@example.com");

        // Assert
        assertEquals(1, results.size());
        verify(userRepository).findByEmail("user+tag@example.com");
        verify(userRepository, never()).findByPhoneNumber(anyString());
    }

    @Test
    void searchUsers_withLeadingTrailingWhitespace_shouldTrimAndSearch() {
        // Arrange
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));

        // Act
        List<UserResponseModel> results = userEmployeeController.searchUsers("  user@example.com  ");

        // Assert
        assertEquals(1, results.size());
        verify(userRepository).findByEmail("user@example.com");
    }
}
