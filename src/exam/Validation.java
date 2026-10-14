package exam;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Shared validation rules keep bad values out of both the UI and database. */
public final class Validation {
    private Validation() { }
    public static String text(String value, String label, int max) {
        if (value == null || value.trim().isEmpty()) throw new IllegalArgumentException(label + " is required.");
        String clean = value.trim();
        if (clean.length() > max) throw new IllegalArgumentException(label + " must be at most " + max + " characters.");
        return clean;
    }
    public static String username(String value) {
        String name = text(value, "Username", 50).toLowerCase(Locale.ROOT);
        if (!name.matches("[a-z0-9_]{3,50}"))
            throw new IllegalArgumentException("Username must contain 3-50 letters, digits or underscores.");
        return name;
    }
    public static void password(char[] value) {
        if (value == null || value.length < 8 || value.length > 128)
            throw new IllegalArgumentException("Password must contain 8-128 characters.");
    }
    public static int option(int n) {
        if (n < 1 || n > 4) throw new IllegalArgumentException("Choose an option from 1 to 4.");
        return n;
    }
    public static long positiveId(long id) {
        if (id <= 0) throw new IllegalArgumentException("ID must be positive.");
        return id;
    }
    public static List<Long> questionIds(String input) {
        String clean = text(input, "Question IDs", 2000);
        List<Long> result = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (String part : clean.split(",", -1)) {
            long id;
            try { id = Long.parseLong(part.trim()); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Enter comma-separated numeric IDs, such as 1,2."); }
            positiveId(id);
            if (!seen.add(id)) throw new IllegalArgumentException("Question IDs cannot repeat.");
            result.add(id);
        }
        if (result.size() > 100) throw new IllegalArgumentException("An exam can contain at most 100 questions.");
        return List.copyOf(result);
    }
}
