package com.example.forevernote.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.example.forevernote.data.database.SQLiteDB;

class SQLiteDBIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void openConnectionEnablesForeignKeysPragma() throws Exception {
        Path dbFile = tempDir.resolve("sqlite-hardening-test.db");

        resetSQLiteDbSingleton();
        SQLiteDB.configure(dbFile.toString());
        SQLiteDB db = SQLiteDB.getInstance();

        Connection connection = db.openConnection();
        assertNotNull(connection);

        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("PRAGMA foreign_keys")) {
            assertEquals(1, resultSet.getInt(1));
        } finally {
            db.closeConnection(connection);
            Files.deleteIfExists(dbFile);
            resetSQLiteDbSingleton();
        }
    }

    private void resetSQLiteDbSingleton() throws Exception {
        Field instanceField = SQLiteDB.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
}
