package com.mana.openhand_backend.identity.presentationlayer.payload;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageResponseTest {

    @Test
    void constructor_setsMessage() {
        MessageResponse response = new MessageResponse("hello");
        assertEquals("hello", response.getMessage());
    }

    @Test
    void setter_updatesMessage() {
        MessageResponse response = new MessageResponse("initial");
        response.setMessage("updated");
        assertEquals("updated", response.getMessage());
    }
}
