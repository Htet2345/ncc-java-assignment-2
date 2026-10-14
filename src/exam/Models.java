package exam;
import java.util.List;

/** Immutable records used between the console and database layer. */
public final class Models {
    private Models() { }
    public record User(long id, String username, String role) {
        public boolean isAdmin() { return "admin".equals(role); }
    }
    public record Question(long id, String text, List<String> options, int correctOption) {
        public Question { options = List.copyOf(options); }
    }
    public record Exam(long id, String name, int questionCount) { }
    public record Result(long attemptId, String examName, int score, int total, String submittedAt) {
        public double percentage() { return 100.0 * score / total; }
    }
}
