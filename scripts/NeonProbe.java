import java.sql.*;
import java.util.Properties;

// Read-only catalog inspection plus a connection-local, rolled-back SQL fixture.
class NeonProbe {
    public static void main(String[] args) throws Exception {
        Class.forName("org.postgresql.Driver");
        for (String prefix : new String[]{"DB", "SPRING_FLYWAY"}) {
            var properties = new Properties();
            properties.setProperty("user", System.getenv(prefix.equals("DB") ? "DB_USERNAME" : "SPRING_FLYWAY_USER"));
            properties.setProperty("password", System.getenv(prefix + "_PASSWORD"));
            properties.setProperty("connectTimeout", "12");
            properties.setProperty("socketTimeout", "15");
            try (var connection = DriverManager.getConnection(System.getenv(prefix + "_URL"), properties)) {
                connection.setSchema("couple_private_space");
                connection.setAutoCommit(false);
                try (var statement = connection.createStatement()) {
                    // Neon terminates client TLS at its proxy; pg_stat_ssl describes
                    // the backend hop. The env adapter requires JDBC sslmode>=require.
                    System.out.println(prefix + " JDBC: connected (TLS required by JDBC)");
                    try (var rs = statement.executeQuery("select current_schema(), current_setting('search_path')")) {
                        rs.next(); System.out.println("Resolved schema: " + rs.getString(1) + "; search_path: " + rs.getString(2));
                    }
                    try (var rs = statement.executeQuery("select to_regclass('call_session') is not null")) {
                        rs.next(); if (!rs.getBoolean(1)) throw new IllegalStateException("App schema not resolved on this connection");
                    }
                    try (var rs = statement.executeQuery("select table_schema,table_name from information_schema.tables where table_schema in ('public','couple_private_space') order by table_schema,table_name")) {
                        while (rs.next()) System.out.println("Existing table: " + rs.getString(1) + "." + rs.getString(2));
                    }
                    try (var rs = statement.executeQuery("select to_regclass('couple_private_space.flyway_schema_history') is not null")) {
                        rs.next();
                        if (rs.getBoolean(1)) {
                            try (var history = connection.createStatement(); var applied = history.executeQuery("select count(*), max(version::integer) from couple_private_space.flyway_schema_history where success and version is not null")) {
                                applied.next();
                                System.out.println("Applied versioned migrations: " + applied.getInt(1) + "; latest V" + applied.getInt(2));
                            }
                            try(var inventory=connection.createStatement();var rows=inventory.executeQuery("select count(*) from couple_private_space.memory_asset")){
                                rows.next();System.out.println("Referenced memory assets: "+rows.getInt(1));
                            }
                        }
                    }
                    statement.execute("create temporary table neon_connection_probe(value text) on commit drop");
                    try (var insert = connection.prepareStatement("insert into neon_connection_probe values (?)")) {
                        insert.setString(1, "မင်္ဂလာပါ 💕"); insert.executeUpdate();
                    }
                    try (var rs = statement.executeQuery("select value from neon_connection_probe")) {
                        if (!rs.next() || !"မင်္ဂလာပါ 💕".equals(rs.getString(1))) throw new IllegalStateException("Readback failed");
                    }
                    connection.rollback();
                    System.out.println(prefix + " Unicode write/read/rollback: PASS (no persistent fixture)");
                }
            } catch (SQLException error) {
                // Do not expose a connection URI or driver error carrying credentials.
                System.err.println(prefix + " JDBC: FAILED; SQLState=" + error.getSQLState());
                System.exit(1);
            }
        }
    }
}
