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
public class UserBiometricsEnabledMigration {
    private static final Logger log = LoggerFactory.getLogger(UserBiometricsEnabledMigration.class);
    private final DataSource dataSource;

    public UserBiometricsEnabledMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("postgresql")) {
                return;
            }

            if (!columnExists(connection)) {
                try (Statement statement = connection.createStatement()) {
                    statement.execute("ALTER TABLE users ADD COLUMN biometrics_enabled BOOLEAN DEFAULT FALSE");
                }
                log.info("Migration applied: users.biometrics_enabled column created.");
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("UPDATE users SET biometrics_enabled = FALSE WHERE biometrics_enabled IS NULL");
                statement.execute("ALTER TABLE users ALTER COLUMN biometrics_enabled SET DEFAULT FALSE");
                statement.execute("ALTER TABLE users ALTER COLUMN biometrics_enabled SET NOT NULL");
            }
        } catch (SQLException ex) {
            log.warn("Skipping users.biometrics_enabled migration due to database error.", ex);
        }
    }

    private boolean columnExists(Connection connection) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'biometrics_enabled'")) {
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next();
        }
    }
}
