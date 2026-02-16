package com.mana.openhand_backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class DonationGuestDonorMigration {
    private static final Logger log = LoggerFactory.getLogger(DonationGuestDonorMigration.class);
    private final DataSource dataSource;

    public DonationGuestDonorMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("postgresql")) {
                return;
            }

            if (!columnExists(connection, "donor_name")) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("ALTER TABLE donations ADD COLUMN donor_name VARCHAR(255)");
                }
                log.info("Migration applied: donations.donor_name column created.");
            }

            if (!columnExists(connection, "donor_email")) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("ALTER TABLE donations ADD COLUMN donor_email VARCHAR(255)");
                }
                log.info("Migration applied: donations.donor_email column created.");
            }

            String userIdNullable = getColumnNullability(connection, "user_id");
            if ("NO".equalsIgnoreCase(userIdNullable)) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("ALTER TABLE donations ALTER COLUMN user_id DROP NOT NULL");
                }
                log.info("Migration applied: donations.user_id set to nullable.");
            }
        } catch (SQLException ex) {
            log.warn("Skipping donations guest donor migration due to database error.", ex);
        }
    }

    private boolean columnExists(Connection connection, String columnName) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.columns WHERE table_name = 'donations' AND column_name = ?")) {
            statement.setString(1, columnName);
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        }
    }

    private String getColumnNullability(Connection connection, String columnName) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT is_nullable FROM information_schema.columns WHERE table_name = 'donations' AND column_name = ?")) {
            statement.setString(1, columnName);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        }
        return null;
    }
}
