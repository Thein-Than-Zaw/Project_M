import java.sql.*;

/** DEV-01 local-only provisioning. No product accounts or private content are seeded. */
class ProvisionLocalDatabase {
    public static void main(String[] args) throws Exception {
        String appPassword = System.getenv("LOCAL_PG_APP_PASSWORD");
        if (appPassword == null || !appPassword.matches("[A-Za-z0-9+/=]+")) {
            throw new IllegalArgumentException("Expected locally generated base64 password");
        }
        try (Connection db = DriverManager.getConnection("jdbc:postgresql://127.0.0.1:54329/postgres",
                "local_operator", System.getenv("LOCAL_PG_ADMIN_PASSWORD"))) {
            try (var query = db.prepareStatement("SELECT 1 FROM pg_roles WHERE rolname = 'couple_app'")) {
                if (!query.executeQuery().next()) {
                    try (var sql = db.createStatement()) {
                        sql.execute("CREATE ROLE couple_app LOGIN NOSUPERUSER NOCREATEDB NOCREATEROLE PASSWORD '" + appPassword + "'");
                    }
                }
            }
            for (String name : new String[]{"couple_private_space", "couple_private_space_test"}) {
                try (var query = db.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?")) {
                    query.setString(1, name);
                    if (!query.executeQuery().next()) {
                        try (var sql = db.createStatement()) {
                            sql.execute("CREATE DATABASE " + name + " OWNER couple_app ENCODING 'UTF8' TEMPLATE template0");
                        }
                    }
                }
            }
        }
        System.out.println("Application role and isolated development/test databases verified.");
    }
}
