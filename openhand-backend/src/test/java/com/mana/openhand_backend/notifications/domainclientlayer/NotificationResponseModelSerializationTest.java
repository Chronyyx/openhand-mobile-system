package com.mana.openhand_backend.notifications.domainclientlayer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NotificationResponseModelSerializationTest {

    @Test
    public void testSerialization() throws Exception {
        NotificationResponseModel model = new NotificationResponseModel();
        model.setRead(true);
        // We want to see if it serializes as "isRead": true or "read": true

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(model);

        System.out.println("Serialized JSON: " + json);

        // It must contain "isRead"
        if (!json.contains("\"isRead\":true")) {
            throw new RuntimeException("Serialization failed to produce 'isRead' property. JSON: " + json);
        }

        // It should NOT contain "read"
        if (json.contains("\"read\":")) {
            throw new RuntimeException("Serialization produced duplicate 'read' property. JSON: " + json);
        }
    }
}
