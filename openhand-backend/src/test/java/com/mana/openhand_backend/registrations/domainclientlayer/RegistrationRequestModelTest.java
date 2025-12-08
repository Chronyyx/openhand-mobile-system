package com.mana.openhand_backend.registrations.domainclientlayer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistrationRequestModelTest {

    @Test
    void constructor_withValidEventId_shouldCreateModel() {
        RegistrationRequestModel model = new RegistrationRequestModel(1L);
        assertNotNull(model);
        assertEquals(1L, model.getEventId());
    }

    @Test
    void constructor_withNullEventId_shouldCreateModel() {
        RegistrationRequestModel model = new RegistrationRequestModel(null);
        assertNotNull(model);
        assertNull(model.getEventId());
    }

    @Test
    void setEventId_shouldUpdateValue() {
        RegistrationRequestModel model = new RegistrationRequestModel(1L);
        model.setEventId(2L);
        assertEquals(2L, model.getEventId());
    }

    @Test
    void setEventId_withNull_shouldSetToNull() {
        RegistrationRequestModel model = new RegistrationRequestModel(1L);
        model.setEventId(null);
        assertNull(model.getEventId());
    }

    @Test
    void constructor_withDifferentEventIds_shouldHaveDifferentValues() {
        RegistrationRequestModel model1 = new RegistrationRequestModel(1L);
        RegistrationRequestModel model2 = new RegistrationRequestModel(2L);
        assertNotEquals(model1.getEventId(), model2.getEventId());
        assertEquals(1L, model1.getEventId());
        assertEquals(2L, model2.getEventId());
    }

    @Test
    void setEventId_withLargeId_shouldAcceptIt() {
        RegistrationRequestModel model = new RegistrationRequestModel(1L);
        Long largeId = 999999999999L;
        model.setEventId(largeId);
        assertEquals(largeId, model.getEventId());
    }

    @Test
    void constructor_multipleInstances_shouldBeIndependent() {
        RegistrationRequestModel model1 = new RegistrationRequestModel(10L);
        RegistrationRequestModel model2 = new RegistrationRequestModel(20L);
        model1.setEventId(15L);
        assertEquals(15L, model1.getEventId());
        assertEquals(20L, model2.getEventId());
    }
}
