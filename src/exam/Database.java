package exam;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Opens JDBC connections using local configuration or environment overrides. */
public final class Database {
    private final String url, username, password, adminKey;
    public Database() throws IOException {
        Properties p = new Properties();
        Path file = Path.of("config.properties");
        if (Files.exists(file)) {
            try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) { p.load(r); }
        }
        url = config(p, "db.url", "EXAM_DB_URL", "jdbc:mysql://localhost:3306/online_exam?sslMode=PREFERRED&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true&connectTimeout=5000&socketTimeout=15000");
        username = config(p, "db.user", "EXAM_DB_USER", "exam_app");
        password = config(p, "db.password", "EXAM_DB_PASSWORD", "");
        adminKey = config(p, "admin.registration.key", "EXAM_ADMIN_KEY", "");
        if (!url.startsWith("jdbc:mysql:")) throw new IllegalArgumentException("A MySQL JDBC URL is required.");
    }
    private String config(Properties p, String key, String env, String fallback) {
        String value = System.getenv(env);
        return value != null ? value : p.getProperty(key, fallback);
    }
    public Connection connect() throws SQLException { return DriverManager.getConnection(url, username, password); }
    public void checkConnection() throws SQLException {
        try (Connection c = connect(); var s = c.prepareStatement("SELECT id FROM users LIMIT 1")) { s.executeQuery().close(); }
    }
    public boolean allowsAdminRegistration(String supplied) {
        if (adminKey.isBlank() || "CHANGE_ME_ADMIN_KEY".equals(adminKey)) return false;
        return java.security.MessageDigest.isEqual(adminKey.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
    }
}
