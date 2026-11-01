import java.sql.*;

/** Local startup verification only: never creates/drops databases or requires admin credentials. */
class VerifyLocalDatabase {
    public static void main(String[] args) throws Exception {
        for (String variable : new String[]{"DB_URL", "TEST_DB_URL"}) {
            String url = System.getenv(variable);
            if (url == null) throw new IllegalStateException("Missing database configuration: " + variable);
            try (var db = DriverManager.getConnection(url, System.getenv("DB_USERNAME"), System.getenv("DB_PASSWORD"));
                 var statement = db.createStatement();
                 var row = statement.executeQuery("SELECT current_database(),current_setting('server_version'),current_setting('port'),current_setting('TimeZone'),rolsuper,rolcreatedb,rolcreaterole FROM pg_roles WHERE rolname=current_user")) {
                if (!row.next() || row.getBoolean(5) || row.getBoolean(6) || row.getBoolean(7))
                    throw new IllegalStateException("Expected a non-administrative application role");
                if (variable.equals("TEST_DB_URL") && !row.getString(1).endsWith("_test"))
                    throw new IllegalStateException("A dedicated _test database is required");
                System.out.printf("Verified %s / PostgreSQL %s / port %s / timezone %s / non-admin app role%n",
                    row.getString(1), row.getString(2), row.getString(3), row.getString(4));
            }
        }
    }
}
