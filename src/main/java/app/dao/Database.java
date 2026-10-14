package app.dao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Owns the JDBC connection factory and schema bootstrap.
 * Uses an embedded SQLite file (clinic.db in the working directory) so the
 * app runs with zero external database setup. All SQL elsewhere goes through
 * PreparedStatement — never string concatenation.
 */
public final class Database {

    private static final String DEFAULT_URL = "jdbc:sqlite:clinic.db";
    private static String url = DEFAULT_URL;

    private Database() {
    }

    /** Point the app at another database (used by tests with in-memory DBs). */
    public static void useUrl(String jdbcUrl) {
        url = jdbcUrl;
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(url);
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            // Wait up to 5s for a competing writer instead of failing at once.
            st.execute("PRAGMA busy_timeout = 5000");
        }
        return conn;
    }

    /** Creates tables if they do not exist yet (db/schema.sql). Safe to call on every launch. */
    public static void initialise() throws SQLException {
        runScript("/db/schema.sql");
    }

    /** Loads db/seed.sql only when the doctors table is empty, so restarts never duplicate seed rows. */
    public static void seedIfEmpty() throws SQLException {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             java.sql.ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM doctors")) {
            rs.next();
            if (rs.getInt(1) > 0) {
                return;
            }
        }
        runScript("/db/seed.sql");
    }

    private static void runScript(String resource) throws SQLException {
        String sql = loadResource(resource);
        // Strip -- comment lines: SQLite-JDBC cannot prepare comment-only statements.
        sql = sql.replaceAll("(?m)^\\s*--.*$", "");
        try (Connection conn = getConnection()) {
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    // Fresh Statement per command (avoids SQLite-JDBC "statement finalized" errors).
                    try (Statement st = conn.createStatement()) {
                        st.execute(trimmed);
                    }
                }
            }
        }
    }

    private static String loadResource(String path) {
        try (InputStream in = Database.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException(path + " missing from classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}
