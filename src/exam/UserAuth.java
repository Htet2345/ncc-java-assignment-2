package exam;
import exam.Models.User;
import java.sql.*;
import java.util.Locale;

/** Registers and authenticates accounts against MySQL. */
public final class UserAuth {
    private final Database db;
    private final String dummyHash = PasswordHasher.hash("unused-dummy-password".toCharArray());
    public UserAuth(Database db) { this.db = db; }
    public User register(String username, char[] password, String role, String adminKey) throws SQLException {
        username = Validation.username(username);
        Validation.password(password);
        role = Validation.text(role, "Role", 10).toLowerCase(Locale.ROOT);
        if (!role.equals("admin") && !role.equals("student")) throw new IllegalArgumentException("Role must be admin or student.");
        if (role.equals("admin") && !db.allowsAdminRegistration(adminKey))
            throw new IllegalArgumentException("Admin registration key is incorrect or not configured.");
        String hash = PasswordHasher.hash(password);
        try (Connection c = db.connect(); PreparedStatement p = c.prepareStatement(
                "INSERT INTO users(username,password_hash,role) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1,username); p.setString(2,hash); p.setString(3,role); p.executeUpdate();
            try (ResultSet r=p.getGeneratedKeys()) {
                if (!r.next()) throw new SQLException("No account ID returned.");
                return new User(r.getLong(1),username,role);
            }
        } catch (SQLException e) {
            if (e.getErrorCode()==1062) throw new IllegalArgumentException("Username already exists.");
            throw e;
        }
    }
    public User login(String username, char[] password) throws SQLException {
        username = Validation.username(username);
        try (Connection c=db.connect(); PreparedStatement p=c.prepareStatement(
                "SELECT id,username,role,password_hash FROM users WHERE username=?")) {
            p.setString(1,username);
            try (ResultSet r=p.executeQuery()) {
                if (!r.next()) { PasswordHasher.verify(password,dummyHash); return null; }
                if (!PasswordHasher.verify(password,r.getString("password_hash"))) return null;
                return new User(r.getLong("id"),r.getString("username"),r.getString("role"));
            }
        }
    }
    /** Re-checks the database role; the caller's supplied role is not trusted. */
    public static void requireRole(Connection c, User user, String role) throws SQLException {
        if (user==null) throw new IllegalArgumentException("Login is required.");
        try (PreparedStatement p=c.prepareStatement("SELECT role FROM users WHERE id=?")) {
            p.setLong(1,user.id());
            try (ResultSet r=p.executeQuery()) {
                if (!r.next() || !role.equals(r.getString(1))) throw new IllegalArgumentException("This action requires the " + role + " role.");
            }
        }
    }
}
