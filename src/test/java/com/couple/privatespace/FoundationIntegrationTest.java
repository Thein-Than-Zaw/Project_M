package com.couple.privatespace;

import java.net.URI;
import java.net.http.*;
import java.sql.*;
import java.util.UUID;
import java.util.concurrent.*;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.junit.jupiter.api.Assertions.*;

/** DEV-01: real PostgreSQL proof; this does not certify future signup/pairing APIs. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
class FoundationIntegrationTest {
    @Autowired DataSource dataSource;
    private final HttpClient http = HttpClient.newHttpClient();

    @BeforeEach void requireIsolatedTestDatabase() throws Exception {
        try (var db = dataSource.getConnection(); var row = db.createStatement().executeQuery("SELECT current_database()")) {
            assertTrue(row.next());
            assertTrue(row.getString(1).endsWith("_test"), "Refusing fixture writes outside a dedicated _test database");
        }
    }

    @Test void healthAndPrivateAccessBoundary() throws Exception {
        var health = get("/actuator/health/readiness");
        assertEquals(200, health.statusCode());
        assertEquals("{\"status\":\"UP\"}", health.body());
        assertFalse(health.body().contains("db"));
        var home = get("/");
        assertEquals(200, home.statusCode());
        assertTrue(home.headers().firstValue("Content-Security-Policy").orElseThrow().contains("frame-ancestors 'none'"));
        assertTrue(home.headers().firstValue("Cache-Control").orElseThrow().contains("no-store"));
        for (var path : new String[]{"/api/me", "/api/messages", "/api/assets/guessed/content", "/ws", "/actuator/env"}) {
            assertEquals(401, get(path).statusCode(), path);
        }
        var write = http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:18080/api/memories"))
            .POST(HttpRequest.BodyPublishers.ofString("{}")).header("Content-Type", "application/json").build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(403, write.statusCode()); // default CSRF rejects untrusted writes
    }

    @Test void migrationsAndNativeConstraints() throws Exception {
        try (var db = dataSource.getConnection()) {
            db.setAutoCommit(false);
            try {
                try (var rows = db.createStatement().executeQuery("SELECT count(*) FROM flyway_schema_history WHERE success AND version IS NOT NULL")) {
                    assertTrue(rows.next()); assertEquals(19, rows.getInt(1)); // DEV-07 adds V16–V19; prior migrations remain unchanged.
                }
                insertUser(db, UUID.randomUUID(), 1, "A@example.invalid", "2020-01-01");
                insertUser(db, UUID.randomUUID(), 2, "B@example.invalid", "2020-01-02");
                try (var row = db.createStatement().executeQuery("SELECT display_name FROM app_user WHERE account_slot=1")) {
                    assertTrue(row.next()); assertEquals("စမ်းသပ် 🌸", row.getString(1));
                }
                expectSqlState(db, "23514", () -> insertUser(db, UUID.randomUUID(), 3, "C@example.invalid", "2020-01-01"));
                expectSqlState(db, "23505", () -> insertUser(db, UUID.randomUUID(), 2, "D@example.invalid", "2020-01-01"));
                expectSqlState(db, "23502", () -> insertUser(db, UUID.randomUUID(), 1, "E@example.invalid", null));
                UUID space = UUID.randomUUID();
                try (var sql = db.prepareStatement("INSERT INTO couple_space(id,deployment_slot,relationship_date,timezone) VALUES (?,1,DATE '2020-01-01','Asia/Rangoon')")) {
                    sql.setObject(1, space); sql.executeUpdate();
                }
                expectSqlState(db, "23505", () -> db.createStatement().executeUpdate("INSERT INTO couple_space(id,deployment_slot,relationship_date,timezone) VALUES ('" + UUID.randomUUID() + "',1,DATE '2020-01-01','UTC')"));
                expectSqlState(db, "23503", () -> db.createStatement().executeUpdate("INSERT INTO couple_membership(space_id,user_id,member_slot) VALUES ('" + space + "','" + UUID.randomUUID() + "',1)"));
                expectSqlState(db, "23514", () -> db.createStatement().executeUpdate("INSERT INTO couple_membership(space_id,user_id,member_slot) VALUES ('" + space + "','" + UUID.randomUUID() + "',3)"));
            } finally { db.rollback(); }
        }
    }

    @Test void concurrentAccountSlotCannotBeClaimedTwice() throws Exception {
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        var pool = Executors.newSingleThreadExecutor();
        try (var a = dataSource.getConnection(); var b = dataSource.getConnection()) {
            a.setAutoCommit(false); b.setAutoCommit(false);
            try (var sql = b.createStatement()) { sql.execute("SET lock_timeout = '5s'"); }
            int waitingPid;
            try (var row = b.createStatement().executeQuery("SELECT pg_backend_pid()")) {
                row.next(); waitingPid = row.getInt(1);
            }
            insertUser(a, first, 1, "race-a@example.invalid", "2020-01-01");
            var attempt = pool.submit(() -> {
                try { insertUser(b, second, 1, "race-b@example.invalid", "2020-01-01"); b.commit(); return "COMMITTED"; }
                catch (SQLException error) { b.rollback(); return error.getSQLState(); }
            });
            boolean observedLockWait = false;
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
            try (var observer = dataSource.getConnection(); var query = observer.prepareStatement(
                    "SELECT wait_event_type FROM pg_stat_activity WHERE pid = ?")) {
                query.setInt(1, waitingPid);
                while (System.nanoTime() < deadline) {
                    try (var row = query.executeQuery()) {
                        if (row.next() && "Lock".equals(row.getString(1))) { observedLockWait = true; break; }
                    }
                    Thread.sleep(25);
                }
            }
            assertTrue(observedLockWait, "Second independent transaction must contend for the uncommitted slot");
            a.commit();
            assertEquals("23505", attempt.get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
            try (var db = dataSource.getConnection(); var delete = db.prepareStatement("DELETE FROM app_user WHERE id IN (?,?)")) {
                delete.setObject(1, first); delete.setObject(2, second); delete.executeUpdate();
            }
        }
    }

    private HttpResponse<String> get(String path) throws Exception {
        return http.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:18080" + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
    private void insertUser(Connection db, UUID id, int slot, String email, String date) throws SQLException {
        try (var sql = db.prepareStatement("INSERT INTO app_user(id,account_slot,display_name,email,password_hash,signup_relationship_date) VALUES (?,?,'စမ်းသပ် 🌸',?,'test-hash-placeholder',?::date)")) {
            sql.setObject(1, id); sql.setInt(2, slot); sql.setString(3, email); sql.setString(4, date); sql.executeUpdate();
        }
    }
    private void expectSqlState(Connection db, String state, SqlAction action) throws Exception {
        Savepoint point = db.setSavepoint();
        try { assertEquals(state, assertThrows(SQLException.class, action::run).getSQLState()); }
        finally { db.rollback(point); db.releaseSavepoint(point); }
    }
    @FunctionalInterface interface SqlAction { void run() throws SQLException; }
}
