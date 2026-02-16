package com.mana.openhand_backend.donations.utils;

import com.mana.openhand_backend.donations.domainclientlayer.DonationSummaryResponseModel;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class DonationCsvExportUtil {

    private static final String HEADER = "Donation ID,Donor Name,Donor Email,User ID,Event ID,Amount,Currency,Frequency,Status,Received At";
    private static final DateTimeFormatter OUTPUT_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private DonationCsvExportUtil() {
    }

    public static String toDonationReportCsv(List<DonationSummaryResponseModel> reportRows) {
        StringBuilder csv = new StringBuilder();
        csv.append(HEADER).append('\n');

        for (DonationSummaryResponseModel row : reportRows) {
            String receivedAt = formatReceivedAt(row.getCreatedAt());

            csv.append(nullableNumberToString(row.getId()))
                    .append(',')
                    .append(escapeCsv(row.getDonorName()))
                    .append(',')
                    .append(escapeCsv(row.getDonorEmail()))
                    .append(',')
                    .append(nullableNumberToString(row.getUserId()))
                    .append(',')
                    .append(nullableNumberToString(row.getEventId()))
                    .append(',')
                    .append(row.getAmount() == null ? "" : row.getAmount())
                    .append(',')
                    .append(escapeCsv(row.getCurrency()))
                    .append(',')
                    .append(escapeCsv(row.getFrequency()))
                    .append(',')
                    .append(escapeCsv(row.getStatus()))
                    .append(',')
                    .append(escapeCsv(receivedAt))
                    .append('\n');
        }

        return csv.toString();
    }

    private static String formatReceivedAt(String createdAt) {
        if (createdAt == null || createdAt.isBlank()) {
            return "";
        }

        try {
            return LocalDateTime.parse(createdAt).format(OUTPUT_DATE_FORMATTER);
        } catch (RuntimeException exception) {
            return createdAt;
        }
    }

    private static String nullableNumberToString(Number value) {
        return value == null ? "" : value.toString();
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        boolean shouldQuote = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return shouldQuote ? "\"" + escaped + "\"" : escaped;
    }
}
