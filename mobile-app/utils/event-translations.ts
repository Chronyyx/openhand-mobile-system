import type { EventSummary } from '../services/events.service';

type Translator = (key: string, options?: any) => string;

export function getTranslatedEventTitle(event: Pick<EventSummary, 'title'>, t: Translator): string {
    const translationKey = `events.names.${event.title}`;
    const fallback = event.title.replace(/_/g, ' ');
    return t(translationKey, { defaultValue: fallback });
}

export function getTranslatedEventDescription(
    event: Pick<EventSummary, 'description'>,
    t: Translator,
): string {
    const descriptionKey = event.description.replace('_description', '');
    const translationKey = `events.descriptions.${descriptionKey}`;
    const fallback = event.description.replace(/_/g, ' ');
    return t(translationKey, { defaultValue: fallback });
}
