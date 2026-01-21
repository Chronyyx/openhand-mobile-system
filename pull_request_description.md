## **Context**

Currently, the platform lacks a streamlined workflow for cancelling events and immediately informing participants. Administrators cannot cancel events directly through the system, and users do not receive real-time updates regarding schedule changes. This prevents timely communication and leads to poor user experience when events are modified. Use of a real-time notification system is necessary to keep users synchronized with the latest event states.

## **Acceptance Criteria**

- **Event Cancellation**: Administrators can cancel events via the admin interface, triggering a status update to `CANCELLED`.
- **Real-Time Notifications**: Integrating a WebSocket based system to push immediate notifications to users (both in-app and visual indicators) when an event is cancelled or updated.
- **Offline Queuing**: Notifications generated while a user is offline are queued and delivered automatically upon their next reconnection.
- **Visual Feedback**: The mobile application visually distinguishes cancelled events and filters/sorts them appropriately.
- **Localization**: Cancellation UI flows and messages are fully translated into English, French, and Spanish.
- **Audit/Notifications**: The system automatically generates `EVENT_UPDATE` notifications when schedule changes occur.

## **Changes**

### **Backend (`openhand-backend`)**

- **Infrastructure**:
    - **WebSockets**: Simplified `WebSocketConfig.java` to use native WebSockets (removed SockJS) for broader compatibility and performance.
    - **Data Models**: Added `CANCELLED` to `EventStatus` enum and `EVENT_UPDATE` to `NotificationType`.
- **Business Logic**:
    - **`EventAdminService`**: Implemented `cancelEvent` method which handles the state change and triggers the notification flow.
    - **`NotificationService`**: Integrated `SimpMessagingTemplate` to push real-time updates to connected clients. Added support for offline queuing handling.
    - **`NotificationTextGenerator`**: Added template logic for schedule change notifications.
- **API Layer**:
    - **`EventAdminController`**: Added a new POST endpoint to handle event cancellation requests.
- **Testing**:
    - Updated `NotificationServiceImplTest` to verify WebSocket messaging interactions.

### **Frontend (`mobile-app`)**

- **Infrastructure**:
    - **WebSockets**: Created `utils/websocket.ts` service managing automatic reconnection, subscriptions, and message dispatching.
    - **Dependencies**: Added `@stomp/stompjs` and `text-encoding` for WebSocket communication.
- **Features**:
    - **`useNotifications.ts`**: New hook to manage notification state and WebSocket listeners.
    - **Event Management**: Updated `EventCard.tsx` and `events/index.tsx` to display and filter cancelled events.
    - **Admin UI**: Added cancellation button and confirmation modal in `admin/events.tsx`.
    - **Navigation**: Added unread notification badges to the App Header and Navigation Menu.
- **Localization**:
    - Added translation keys for cancellation dialogs and status labels in `en`, `es`, and `fr`.

## **Does this add new communication between services?**

**Yes.**
- **WebSocket**: The Backend now maintains persistent WebSocket connections with Mobile clients to push `EVENT_UPDATE` and cancellation notifications in real-time.
- **REST API**: The Mobile app consumes the new cancellation endpoint exposed by the Backend.

---

## **Before and After Behavior**

### **Before**
- Events could not be cancelled by admins within the app; manual database intervention or separate communication was required.
- Users had to refresh or reload to see if an event's status had changed.
- No visual distinction existed for cancelled events in the list views.

### **After**
- **Admin Control**: Admins can instantly cancel events, which immediately propagate to all users.
- **Real-Time Awareness**: Users connected to the app receive a live notification badge and list update the moment an event changes.
- **Clarity**: Cancelled events are clearly marked with distinct UI styling (labels/colors), preventing confusion.
