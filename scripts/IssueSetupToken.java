import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.util.*;

/** AUTH-03: local operator only; no public issuance or raw code in logs. */
class IssueSetupToken {
    public static void main(String[] args) throws Exception {
        byte[] bytes = new byte[32]; new SecureRandom().nextBytes(bytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(code.getBytes(StandardCharsets.UTF_8)));
        try (var db = DriverManager.getConnection(System.getenv("DB_URL"), System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"))) {
            db.setAutoCommit(false);
            try (var sql = db.createStatement()) {
                sql.executeQuery("SELECT id FROM registration_gate WHERE id=1 FOR UPDATE").close();
                try (var rows = sql.executeQuery("SELECT count(*) FROM app_user")) {
                    rows.next(); if (rows.getInt(1) != 0) throw new IllegalStateException("Setup is closed: an account already exists. Sign in instead.");
                }
                sql.executeUpdate("UPDATE registration_token SET revoked_at=CURRENT_TIMESTAMP WHERE kind='SETUP' AND consumed_at IS NULL AND revoked_at IS NULL");
                try (var insert = db.prepareStatement("INSERT INTO registration_token(id,kind,token_hash,expires_at) VALUES (?,'SETUP',?,CURRENT_TIMESTAMP+INTERVAL '24 hours')")) {
                    insert.setObject(1, UUID.randomUUID()); insert.setString(2, hash); insert.executeUpdate();
                }
                Files.writeString(Path.of(".local/setup-token.txt"), code, StandardCharsets.US_ASCII);
                db.commit();
            } catch (Exception error) { db.rollback(); throw error; }
        }
        System.out.println("Setup code saved privately to .local/setup-token.txt (valid 24 hours). Paste it into Create account.");
    }
}
