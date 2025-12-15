import { type ImageSourcePropType } from 'react-native';
import { type EventSummary } from '../services/events.service';

export const eventImages: Record<string, ImageSourcePropType> = {
    'gala': require('../assets/mana/Gala_image_Mana.png'),
    'distribution_mardi': require('../assets/mana/boutiqueSolidaire_Mana.png'),
    'formation_mediateur': require('../assets/mana/Interculturelle_Mana.png'),
    'panier_noel': require('../assets/mana/PanierNoel_Mana.png'),
};

export function getEventImage(event: EventSummary | null): ImageSourcePropType | undefined {
    if (!event) return undefined;
    return eventImages[event.title];
}
