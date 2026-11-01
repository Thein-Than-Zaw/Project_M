import java.sql.*;

/** DEV-01 local-only provisioning. No product accounts or private content are seeded. */
class ProvisionLocalDatabase {
    public static void main(String[] args) throws Exception {
        String appPassword = System.getenv("LOCAL_PG_APP_PASSWORD");
        if (appPassword == null || !appPassword.matches("[A-Za-z0-9+/=]+")) {
            throw new IllegalArgumentException("Expected locally generated base64 password");
        }
        String adminUrl = System.getenv().getOrDefault("LOCAL_PG_ADMIN_URL", "jdbc:postgresql://127.0.0.1:54329/postgres");
        String adminUser = System.getenv().getOrDefault("LOCAL_PG_ADMIN_USERNAME", "local_operator");
        try (Connection db = DriverManager.getConnection(adminUrl, adminUser, System.getenv("LOCAL_PG_ADMIN_PASSWORD"))) {
            try (var query = db.prepareStatement("SELECT rolsuper,rolcreatedb,rolcreaterole,rolcanlogin FROM pg_roles WHERE rolname = 'couple_app'")) {
                var role = query.executeQuery();
                if (!role.next()) {
                    try (var sql = db.createStatement()) {
                        sql.execute("CREATE ROLE couple_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '" + appPassword + "'");
                    }
                } else if (role.getBoolean(1) || role.getBoolean(2) || role.getBoolean(3) || !role.getBoolean(4)) {
                    throw new IllegalStateException("Existing couple_app has unexpected privileges; no role was altered.");
                }
            }
            for (String name : new String[]{"couple_private_space", "couple_private_space_test"}) {
                try (var query = db.prepareStatement("SELECT pg_get_userbyid(datdba) FROM pg_database WHERE datname = ?")) {
                    query.setString(1, name);
                    var database = query.executeQuery();
                    if (!database.next()) {
                        try (var sql = db.createStatement()) {
                            sql.execute("CREATE DATABASE " + name + " OWNER couple_app ENCODING 'UTF8' TEMPLATE template0 LC_COLLATE 'C' LC_CTYPE 'C' LOCALE_PROVIDER libc");
                        }
                    } else if (!"couple_app".equals(database.getString(1))) {
                        throw new IllegalStateException("Existing project database has another owner; no ownership was changed.");
                    }
                }
                try (var sql = db.createStatement()) {
                    sql.execute("ALTER DATABASE " + name + " SET timezone = 'UTC'");
                }
            }
        }
        System.out.println("Application role and isolated development/test databases verified.");
    }
}
