package com.mana.openhand_backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DonationGuestDonorMigrationTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private PreparedStatement donorNameStatement;

    @Mock
    private PreparedStatement donorEmailStatement;

    @Mock
    private PreparedStatement userIdStatement;

    @Mock
    private ResultSet donorNameResult;

    @Mock
    private ResultSet donorEmailResult;

    @Mock
    private ResultSet userIdResult;

    @Mock
    private Statement alterDonorNameStatement;

    @Mock
    private Statement alterDonorEmailStatement;

    @Mock
    private Statement alterUserIdStatement;

    @Test
    void migrate_skipsWhenNotPostgres() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        DonationGuestDonorMigration migration = new DonationGuestDonorMigration(dataSource);
        migration.migrate();

        verify(connection, never()).prepareStatement(anyString());
        verify(connection, never()).createStatement();
    }

    @Test
    void migrate_addsColumnsAndMakesUserNullableWhenNeeded() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.prepareStatement(anyString()))
                .thenReturn(donorNameStatement, donorEmailStatement, userIdStatement);
        when(donorNameStatement.executeQuery()).thenReturn(donorNameResult);
        when(donorEmailStatement.executeQuery()).thenReturn(donorEmailResult);
        when(userIdStatement.executeQuery()).thenReturn(userIdResult);
        when(donorNameResult.next()).thenReturn(false);
        when(donorEmailResult.next()).thenReturn(false);
        when(userIdResult.next()).thenReturn(true);
        when(userIdResult.getString(1)).thenReturn("NO");
        when(connection.createStatement())
                .thenReturn(alterDonorNameStatement, alterDonorEmailStatement, alterUserIdStatement);

        DonationGuestDonorMigration migration = new DonationGuestDonorMigration(dataSource);
        migration.migrate();

        verify(alterDonorNameStatement).execute("ALTER TABLE donations ADD COLUMN donor_name VARCHAR(255)");
        verify(alterDonorEmailStatement).execute("ALTER TABLE donations ADD COLUMN donor_email VARCHAR(255)");
        verify(alterUserIdStatement).execute("ALTER TABLE donations ALTER COLUMN user_id DROP NOT NULL");
    }

    @Test
    void migrate_noopWhenColumnsExistAndUserAlreadyNullable() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.prepareStatement(anyString()))
                .thenReturn(donorNameStatement, donorEmailStatement, userIdStatement);
        when(donorNameStatement.executeQuery()).thenReturn(donorNameResult);
        when(donorEmailStatement.executeQuery()).thenReturn(donorEmailResult);
        when(userIdStatement.executeQuery()).thenReturn(userIdResult);
        when(donorNameResult.next()).thenReturn(true);
        when(donorEmailResult.next()).thenReturn(true);
        when(userIdResult.next()).thenReturn(true);
        when(userIdResult.getString(1)).thenReturn("YES");

        DonationGuestDonorMigration migration = new DonationGuestDonorMigration(dataSource);
        migration.migrate();

        verify(connection, never()).createStatement();
    }

    @Test
    void migrate_handlesSQLException() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("fail"));
        DonationGuestDonorMigration migration = new DonationGuestDonorMigration(dataSource);

        assertDoesNotThrow(migration::migrate);
    }
}
