/**
 * Utility to translate notification text based on notification type and language
 */

export function getTranslatedNotificationText(
    notificationType: string,
    eventTitle: string,
    language: string,
    eventStartDateTime?: string | null,
    participantName?: string | null
): string {
    switch (notificationType) {
        case 'REGISTRATION_CONFIRMATION':
            return getConfirmationText(eventTitle, language);
        case 'CANCELLATION':
            return getCancellationText(eventTitle, language);
        case 'REMINDER':
            return getReminderText(eventTitle, eventStartDateTime, language);
        case 'EMPLOYEE_REGISTERED_PARTICIPANT':
            return getEmployeeRegisteredText(eventTitle, participantName, language);
        default:
            return '';
    }
}

function getConfirmationText(eventTitle: string, language: string): string {
    switch (language) {
        case 'fr':
            return `Vous êtes confirmé(e) pour l'événement : ${eventTitle}. Merci de votre inscription !`;
        case 'es':
            return `Está confirmado para el evento: ${eventTitle}. ¡Gracias por su registro!`;
        default:
            return `You are confirmed for the event: ${eventTitle}. Thank you for registering!`;
    }
}

function getCancellationText(eventTitle: string, language: string): string {
    switch (language) {
        case 'fr':
            return `Votre inscription à l'événement ${eventTitle} a été annulée.`;
        case 'es':
            return `Su registro para el evento ${eventTitle} ha sido cancelado.`;
        default:
            return `Your registration for the event ${eventTitle} has been cancelled.`;
    }
}

function getReminderText(
    eventTitle: string,
    eventStartDateTime: string | null | undefined,
    language: string
): string {
    // Parse the date if provided
    let formattedDate = 'TBD';
    if (eventStartDateTime) {
        try {
            const date = new Date(eventStartDateTime);
            const dateTimeFormatter = new Intl.DateTimeFormat(language === 'fr' ? 'fr-FR' : language === 'es' ? 'es-ES' : 'en-US', {
                year: 'numeric',
                month: 'short',
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
            formattedDate = dateTimeFormatter.format(date);
        } catch {
            formattedDate = eventStartDateTime;
        }
    }

    switch (language) {
        case 'fr':
            return `Rappel : L'événement ${eventTitle} commence le ${formattedDate}. N'oubliez pas de vous présenter !`;
        case 'es':
            return `Recordatorio: El evento ${eventTitle} comienza el ${formattedDate}. ¡No olvide asistir!`;
        default:
            return `Reminder: The event ${eventTitle} starts on ${formattedDate}. Don't forget to attend!`;
    }
}

function getEmployeeRegisteredText(
    eventTitle: string,
    participantName: string | null | undefined,
    language: string
): string {
    const name = participantName || 'participant';
    
    switch (language) {
        case 'fr':
            return `Vous avez enregistré ${name} pour l'événement : ${eventTitle}.`;
        case 'es':
            return `Ha registrado a ${name} para el evento: ${eventTitle}.`;
        default:
            return `You have registered ${name} for the event: ${eventTitle}.`;
    }
}
