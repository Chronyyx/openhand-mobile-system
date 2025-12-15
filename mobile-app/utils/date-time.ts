export function formatIsoDate(iso: string): string {
    const match = iso.match(/^(\d{4}-\d{2}-\d{2})/);
    if (match) return match[1];

    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return iso;
    return date.toISOString().slice(0, 10);
}

export function formatIsoTime(iso: string): string {
    const match = iso.match(/T(\d{2}:\d{2})/);
    if (match) return match[1];

    const date = new Date(iso);
    if (Number.isNaN(date.getTime())) return iso;
    return date.toISOString().slice(11, 16);
}

export function formatIsoTimeRange(startIso: string, endIso: string | null): string {
    const startStr = formatIsoTime(startIso);
    const endStr = endIso ? formatIsoTime(endIso) : '';
    return endStr ? `${startStr} - ${endStr}` : startStr;
}

export function formatIsoDateTime(iso: string): string {
    return `${formatIsoDate(iso)} ${formatIsoTime(iso)}`;
}

