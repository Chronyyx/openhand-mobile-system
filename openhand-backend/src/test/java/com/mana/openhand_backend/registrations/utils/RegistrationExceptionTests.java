package com.mana.openhand_backend.registrations.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationExceptionTests {

    @Test
    void alreadyRegisteredException_withUserAndEventId_shouldCreateException() {
        // Arrange

        // Act
        AlreadyRegisteredException exception = new AlreadyRegisteredException(1L, 10L);

        // Assert
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("1"));
        assertTrue(exception.getMessage().contains("10"));
    }

    @Test
    void alreadyRegisteredException_isRuntimeException_shouldExtendRuntimeException() {
        // Arrange

        // Act
        AlreadyRegisteredException exception = new AlreadyRegisteredException(1L, 10L);

        // Assert
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void alreadyRegisteredException_canBeThrown_shouldCatchAsRuntimeException() {
        // Arrange

        // Act & Assert
        assertThrows(AlreadyRegisteredException.class, () -> {
            throw new AlreadyRegisteredException(1L, 10L);
        });
    }

    @Test
    void alreadyRegisteredException_withDifferentIds_shouldCreateDifferentMessages() {
        // Arrange

        // Act
        AlreadyRegisteredException ex1 = new AlreadyRegisteredException(1L, 10L);
        AlreadyRegisteredException ex2 = new AlreadyRegisteredException(2L, 20L);

        // Assert
        assertNotEquals(ex1.getMessage(), ex2.getMessage());
        assertTrue(ex1.getMessage().contains("1"));
        assertTrue(ex2.getMessage().contains("2"));
    }

    @Test
    void alreadyRegisteredException_withLargeIds_shouldCreateException() {
        // Arrange

        // Act
        AlreadyRegisteredException exception = new AlreadyRegisteredException(999999L, 888888L);

        // Assert
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("999999"));
        assertTrue(exception.getMessage().contains("888888"));
    }

    @Test
    void eventFullException_withEventId_shouldCreateException() {
        // Arrange

        // Act
        EventFullException exception = new EventFullException(10L);

        // Assert
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("10"));
        assertTrue(exception.getMessage().toLowerCase().contains("full"));
    }

    @Test
    void eventFullException_isRuntimeException_shouldExtendRuntimeException() {
        // Arrange

        // Act
        EventFullException exception = new EventFullException(10L);

        // Assert
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void eventFullException_canBeThrown_shouldCatchAsRuntimeException() {
        // Arrange

        // Act & Assert
        assertThrows(EventFullException.class, () -> {
            throw new EventFullException(10L);
        });
    }

    @Test
    void eventFullException_withDifferentIds_shouldCreateDifferentMessages() {
        // Arrange

        // Act
        EventFullException ex1 = new EventFullException(10L);
        EventFullException ex2 = new EventFullException(20L);

        // Assert
        assertNotEquals(ex1.getMessage(), ex2.getMessage());
        assertTrue(ex1.getMessage().contains("10"));
        assertTrue(ex2.getMessage().contains("20"));
    }

    @Test
    void eventFullException_withLargeId_shouldCreateException() {
        // Arrange

        // Act
        EventFullException exception = new EventFullException(999999L);

        // Assert
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("999999"));
    }

    @Test
    void registrationNotFoundException_withRegistrationId_shouldCreateException() {
        // Arrange

        // Act
        RegistrationNotFoundException exception = new RegistrationNotFoundException(100L);

        // Assert
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("100"));
        assertTrue(exception.getMessage().toLowerCase().contains("not found"));
    }

    @Test
    void registrationNotFoundException_isRuntimeException_shouldExtendRuntimeException() {
        // Arrange

        // Act
        RegistrationNotFoundException exception = new RegistrationNotFoundException(100L);

        // Assert
        assertInstanceOf(RuntimeException.class, exception);
    }

    @Test
    void registrationNotFoundException_canBeThrown_shouldCatchAsRuntimeException() {
        // Arrange

        // Act & Assert
        assertThrows(RegistrationNotFoundException.class, () -> {
            throw new RegistrationNotFoundException(100L);
        });
    }

    @Test
    void registrationNotFoundException_withDifferentIds_shouldCreateDifferentMessages() {
        // Arrange

        // Act
        RegistrationNotFoundException ex1 = new RegistrationNotFoundException(100L);
        RegistrationNotFoundException ex2 = new RegistrationNotFoundException(200L);

        // Assert
        assertNotEquals(ex1.getMessage(), ex2.getMessage());
        assertTrue(ex1.getMessage().contains("100"));
        assertTrue(ex2.getMessage().contains("200"));
    }

    @Test
    void registrationNotFoundException_withLargeId_shouldCreateException() {
        // Arrange

        // Act
        RegistrationNotFoundException exception = new RegistrationNotFoundException(999999L);

        // Assert
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("999999"));
    }

    @Test
    void allExceptions_canBeCaughtAsRuntimeException_shouldWork() {
        // Arrange

        // Act & Assert
        assertThrows(RuntimeException.class, () -> {
            throw new AlreadyRegisteredException(1L, 10L);
        });
        assertThrows(RuntimeException.class, () -> {
            throw new EventFullException(10L);
        });
        assertThrows(RuntimeException.class, () -> {
            throw new RegistrationNotFoundException(100L);
        });
    }

    @Test
    void alreadyRegisteredException_messageFormat_shouldIncludeUserAndEventId() {
        // Arrange

        // Act
        AlreadyRegisteredException exception = new AlreadyRegisteredException(123L, 456L);

        // Assert
        String message = exception.getMessage();
        assertTrue(message.contains("123"));
        assertTrue(message.contains("456"));
        assertTrue(message.contains("User"));
        assertTrue(message.contains("registered"));
    }

    @Test
    void eventFullException_messageFormat_shouldIncludeEventId() {
        // Arrange

        // Act
        EventFullException exception = new EventFullException(789L);

        // Assert
        String message = exception.getMessage();
        assertTrue(message.contains("789"));
        assertTrue(message.contains("full"));
        assertTrue(message.contains("waitlist"));
    }

    @Test
    void registrationNotFoundException_messageFormat_shouldIncludeRegistrationId() {
        // Arrange

        // Act
        RegistrationNotFoundException exception = new RegistrationNotFoundException(555L);

        // Assert
        String message = exception.getMessage();
        assertTrue(message.contains("555"));
        assertTrue(message.contains("not found"));
    }

    @Test
    void exceptions_toString_shouldIncludeClassAndMessage() {
        // Arrange
        AlreadyRegisteredException ex1 = new AlreadyRegisteredException(1L, 10L);
        EventFullException ex2 = new EventFullException(10L);
        RegistrationNotFoundException ex3 = new RegistrationNotFoundException(100L);

        // Act & Assert
        assertTrue(ex1.toString().contains("AlreadyRegisteredException"));
        assertTrue(ex2.toString().contains("EventFullException"));
        assertTrue(ex3.toString().contains("RegistrationNotFoundException"));
    }

    @Test
    void alreadyRegisteredException_multipleInstances_shouldHaveIndependentMessages() {
        // Arrange

        // Act
        AlreadyRegisteredException ex1 = new AlreadyRegisteredException(1L, 10L);
        AlreadyRegisteredException ex2 = new AlreadyRegisteredException(2L, 20L);
        AlreadyRegisteredException ex3 = new AlreadyRegisteredException(3L, 30L);

        // Assert
        assertNotEquals(ex1.getMessage(), ex2.getMessage());
        assertNotEquals(ex2.getMessage(), ex3.getMessage());
        assertNotEquals(ex1.getMessage(), ex3.getMessage());
    }

    @Test
    void eventFullException_withZeroId_shouldCreateException() {
        // Arrange

        // Act
        EventFullException exception = new EventFullException(0L);

        // Assert
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("0"));
    }

    @Test
    void registrationNotFoundException_withZeroId_shouldCreateException() {
        // Arrange

        // Act
        RegistrationNotFoundException exception = new RegistrationNotFoundException(0L);

        // Assert
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("0"));
    }

    @Test
    void alreadyRegisteredException_withZeroIds_shouldCreateException() {
        // Arrange

        // Act
        AlreadyRegisteredException exception = new AlreadyRegisteredException(0L, 0L);

        // Assert
        assertNotNull(exception);
        assertTrue(exception.getMessage().contains("0"));
    }

    @Test
    void exceptions_asRuntimeException_shouldBeCatchable() {
        // Arrange

        // Act & Assert
        try {
            throw new AlreadyRegisteredException(1L, 10L);
        } catch (RuntimeException e) {
            assertTrue(e instanceof AlreadyRegisteredException);
        }

        try {
            throw new EventFullException(10L);
        } catch (RuntimeException e) {
            assertTrue(e instanceof EventFullException);
        }

        try {
            throw new RegistrationNotFoundException(100L);
        } catch (RuntimeException e) {
            assertTrue(e instanceof RegistrationNotFoundException);
        }
    }
}
