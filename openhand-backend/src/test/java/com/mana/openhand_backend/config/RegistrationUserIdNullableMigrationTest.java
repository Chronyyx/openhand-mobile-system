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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationUserIdNullableMigrationTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @Mock
    private Statement statement;

    @Test
    void migrate_skipsWhenNotPostgres() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("H2");

        RegistrationUserIdNullableMigration migration = new RegistrationUserIdNullableMigration(dataSource);
        migration.migrate();

        verify(connection, never()).prepareStatement(anyString());
        verify(connection, never()).createStatement();
    }

    @Test
    void migrate_appliesWhenNotNullable() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn("NO");
        when(connection.createStatement()).thenReturn(statement);

        RegistrationUserIdNullableMigration migration = new RegistrationUserIdNullableMigration(dataSource);
        migration.migrate();

        verify(statement).execute("ALTER TABLE registrations ALTER COLUMN user_id DROP NOT NULL");
    }

    @Test
    void migrate_noopWhenAlreadyNullable() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString(1)).thenReturn("YES");

        RegistrationUserIdNullableMigration migration = new RegistrationUserIdNullableMigration(dataSource);
        migration.migrate();

        verify(connection, never()).createStatement();
    }

    @Test
    void migrate_handlesSQLException() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("fail"));
        RegistrationUserIdNullableMigration migration = new RegistrationUserIdNullableMigration(dataSource);

        assertDoesNotThrow(migration::migrate);
    }
}
