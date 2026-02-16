import apiClient from './api.client';

export type DonationMetricBreakdown = {
    key: string;
    count: number;
    amount: number;
};

export type DonationMonthlyTrend = {
    period: string;
    count: number;
    amount: number;
};

export type DonationTopDonor = {
    userId: number | null;
    donorName: string | null;
    donorEmail: string | null;
    donationCount: number;
    totalAmount: number;
};

export type DonationReportRow = {
    id: number;
    userId: number | null;
    eventId: number | null;
    donorName: string | null;
    donorEmail: string | null;
    amount: number;
    currency: string;
    frequency: string;
    status: string;
    createdAt: string | null;
};

export type DonationMetrics = {
    currency: string;
    totalDonations: number;
    totalAmount: number;
    averageAmount: number;
    uniqueDonors: number;
    repeatDonors: number;
    firstTimeDonors: number;
    frequencyBreakdown: DonationMetricBreakdown[];
    statusBreakdown: DonationMetricBreakdown[];
    monthlyTrend: DonationMonthlyTrend[];
    topDonorsByAmount: DonationTopDonor[];
    topDonorsByCount: DonationTopDonor[];
    manualDonationsCount: number;
    manualDonationsAmount: number;
    externalDonationsCount: number;
    externalDonationsAmount: number;
    commentsCount: number;
    commentsUsageRate: number;
    donationNotificationsCreated: number;
    donationNotificationsRead: number;
    donationNotificationsUnread: number;
};

export async function getDonationMetrics(): Promise<DonationMetrics> {
    const response = await apiClient.get<DonationMetrics>('/admin/donations/metrics');
    return response.data;
}

export async function getDonationReport(
    startDate: string,
    endDate: string,
): Promise<DonationReportRow[]> {
    const response = await apiClient.get<DonationReportRow[]>('/admin/donations/reports', {
        params: { startDate, endDate },
    });
    return response.data;
}

export async function exportDonationReportCsv(
    startDate: string,
    endDate: string,
): Promise<string> {
    const response = await apiClient.get<string>('/admin/donations/reports/export', {
        params: { startDate, endDate },
        responseType: 'text',
        headers: {
            Accept: 'text/csv',
        },
    });
    return response.data;
}
