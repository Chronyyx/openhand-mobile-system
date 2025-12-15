export function getStatusLabel(status: string | undefined, t: (k: string) => string) {
    if (!status) return '';
    switch (status) {
        case 'OPEN':
            return t('events.status.OPEN');
        case 'NEARLY_FULL':
            return t('events.status.NEARLY_FULL');
        case 'FULL':
            return t('events.status.FULL');
        default:
            return status;
    }
}

export function getStatusColor(status: string | undefined): string {
    switch (status) {
        case 'OPEN':
            return '#E3F2FD'; // Light Blue
        case 'NEARLY_FULL':
            return '#F6B800'; // Yellow
        case 'FULL':
            return '#E0E0E0'; // Light Gray (Grayed out)
        default:
            return '#E3F2FD';
    }
}

export function getStatusTextColor(status: string | undefined): string {
    switch (status) {
        case 'OPEN':
            return '#0056A8'; // Blue Text
        case 'NEARLY_FULL':
            return '#333333'; // Dark Gray Text (Readable on Yellow)
        case 'FULL':
            return '#757575'; // Dark Gray Text (Disabled look)
        default:
            return '#0056A8';
    }
}
