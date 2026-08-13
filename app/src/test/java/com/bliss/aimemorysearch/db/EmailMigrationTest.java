package com.bliss.aimemorysearch.db;

import org.junit.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmailMigrationTest {
    @Test public void migrationPreservesExistingFilesChunksAndTokens() throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             Statement sql = connection.createStatement()) {
            sql.execute("CREATE TABLE files_index(path TEXT PRIMARY KEY, name TEXT)");
            sql.execute("CREATE TABLE chunks(id INTEGER PRIMARY KEY, filePath TEXT, chunkText TEXT)");
            sql.execute("CREATE TABLE token_index(id INTEGER PRIMARY KEY, token TEXT)");
            sql.execute("INSERT INTO files_index VALUES('/docs/old.pdf', 'old.pdf')");
            sql.execute("INSERT INTO chunks VALUES(7, '/docs/old.pdf', 'existing text')");
            sql.execute("INSERT INTO token_index VALUES(11, 'existing')");

            EmailMigrations.applyV9(statement -> {
                try {
                    sql.execute(statement);
                } catch (java.sql.SQLException error) {
                    throw new IllegalStateException(error);
                }
            });

            assertEquals(1, count(sql, "files_index"));
            assertEquals(1, count(sql, "chunks"));
            assertEquals(1, count(sql, "token_index"));
            assertTrue(hasColumn(sql, "chunks", "sourceType"));
            assertTrue(hasColumn(sql, "chunks", "sourceId"));
            assertEquals(0, count(sql, "emails"));
        }
    }

    @Test public void migrationContainsNoDestructiveStatement() {
        java.util.List<String> statements = new java.util.ArrayList<>();
        EmailMigrations.applyV9(statements::add);
        assertFalse(statements.isEmpty());
        for (String statement : statements) {
            String normalized = statement.toUpperCase(java.util.Locale.ROOT);
            assertFalse(normalized.contains("DROP TABLE"));
            assertFalse(normalized.contains("DELETE FROM"));
        }
    }

    private static int count(Statement sql, String table) throws Exception {
        try (ResultSet result = sql.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return result.next() ? result.getInt(1) : 0;
        }
    }

    private static boolean hasColumn(Statement sql, String table, String column)
            throws Exception {
        try (ResultSet result = sql.executeQuery("PRAGMA table_info(" + table + ")")) {
            while (result.next()) if (column.equals(result.getString("name"))) return true;
            return false;
        }
    }
}
