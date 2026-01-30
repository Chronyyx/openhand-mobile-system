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
public class RegistrationUserIdNullableMigration {
    private static final Logger log = LoggerFactory.getLogger(RegistrationUserIdNullableMigration.class);
    private final DataSource dataSource;

    public RegistrationUserIdNullableMigration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void migrate() {
        try (Connection connection = dataSource.getConnection()) {
            String product = connection.getMetaData().getDatabaseProductName();
            if (product == null || !product.toLowerCase().contains("postgresql")) {
                return;
            }

            String nullable = null;
            try (var statement = connection.prepareStatement(
                    "SELECT is_nullable FROM information_schema.columns WHERE table_name = 'registrations' AND column_name = 'user_id'")) {
                ResultSet resultSet = statement.executeQuery();
                if (resultSet.next()) {
                    nullable = resultSet.getString(1);
                }
            }

            if ("NO".equalsIgnoreCase(nullable)) {
                try (Statement alter = connection.createStatement()) {
                    alter.execute("ALTER TABLE registrations ALTER COLUMN user_id DROP NOT NULL");
                }
                log.info("Migration applied: registrations.user_id set to nullable.");
            }
        } catch (SQLException ex) {
            log.warn("Skipping registrations.user_id nullable migration due to database error.", ex);
        }
    }
}
