package exam;
import exam.Models.*;
import java.sql.*;
import java.util.*;

/** Parameterized SQL operations for question banks, exams and saved attempts. */
public final class ExamManager {
    private final Database db;
    public ExamManager(Database db) { this.db=db; }

    public long addQuestion(User admin, String text, List<String> options, int correct) throws SQLException {
        text=Validation.text(text,"Question text",1000);
        if(options.size()!=4) throw new IllegalArgumentException("Exactly four options are required.");
        List<String> cleaned=new ArrayList<>();
        for(String option:options) cleaned.add(Validation.text(option,"Answer option",255));
        Validation.option(correct);
        try(Connection c=db.connect()) {
            UserAuth.requireRole(c,admin,"admin");
            try(PreparedStatement p=c.prepareStatement("INSERT INTO questions(question_text,option1,option2,option3,option4,correct_option,created_by) VALUES(?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
                p.setString(1,text);
                for(int i=0;i<4;i++) p.setString(i+2,cleaned.get(i));
                p.setInt(6,correct); p.setLong(7,admin.id()); p.executeUpdate();
                return generatedId(p);
            }
        }
    }
    public List<Question> listQuestions(User admin) throws SQLException {
        try(Connection c=db.connect()) {
            UserAuth.requireRole(c,admin,"admin");
            try(PreparedStatement p=c.prepareStatement("SELECT * FROM questions ORDER BY id"); ResultSet r=p.executeQuery()) {
                List<Question> result=new ArrayList<>(); while(r.next()) result.add(question(r)); return result;
            }
        }
    }
    public long createExam(User admin, String name, List<Long> ids) throws SQLException {
        name=Validation.text(name,"Exam name",100);
        if(ids.isEmpty() || ids.size()>100 || new HashSet<>(ids).size()!=ids.size())
            throw new IllegalArgumentException("Choose 1-100 different question IDs.");
        for(long id:ids) Validation.positiveId(id);
        try(Connection c=db.connect()) {
            c.setAutoCommit(false);
            try {
                UserAuth.requireRole(c,admin,"admin");
                // One query validates all IDs, rather than issuing one query per question.
                String marks=String.join(",",Collections.nCopies(ids.size(),"?"));
                try(PreparedStatement p=c.prepareStatement("SELECT id FROM questions WHERE id IN ("+marks+")")) {
                    for(int i=0;i<ids.size();i++) p.setLong(i+1,ids.get(i));
                    try(ResultSet r=p.executeQuery()) {
                        Set<Long> found=new HashSet<>(); while(r.next()) found.add(r.getLong(1));
                        for(long id:ids) if(!found.contains(id)) throw new IllegalArgumentException("Question ID "+id+" does not exist.");
                    }
                }
                long examId;
                try(PreparedStatement p=c.prepareStatement("INSERT INTO exams(name,created_by) VALUES(?,?)",Statement.RETURN_GENERATED_KEYS)) {
                    p.setString(1,name); p.setLong(2,admin.id()); p.executeUpdate(); examId=generatedId(p);
                }
                try(PreparedStatement p=c.prepareStatement("INSERT INTO exam_questions(exam_id,question_id,position) VALUES(?,?,?)")) {
                    for(int i=0;i<ids.size();i++) {
                        p.setLong(1,examId); p.setLong(2,ids.get(i)); p.setInt(3,i+1); p.addBatch();
                    }
                    p.executeBatch();
                }
                c.commit(); return examId;
            } catch(SQLException | RuntimeException e) {
                rollback(c,e);
                if(e instanceof SQLException sql && sql.getErrorCode()==1062) throw new IllegalArgumentException("An exam with that name already exists.");
                throw e;
            }
        }
    }
    public List<Exam> listExams() throws SQLException {
        try(Connection c=db.connect(); PreparedStatement p=c.prepareStatement(
                "SELECT e.id,e.name,COUNT(eq.question_id) AS question_count FROM exams e JOIN exam_questions eq ON eq.exam_id=e.id GROUP BY e.id,e.name ORDER BY e.id"); ResultSet r=p.executeQuery()) {
            List<Exam> result=new ArrayList<>();
            while(r.next()) result.add(new Exam(r.getLong("id"),r.getString("name"),r.getInt("question_count")));
            return result;
        }
    }
    public List<Question> examQuestions(User student,long examId) throws SQLException {
        Validation.positiveId(examId);
        try(Connection c=db.connect()) {
            UserAuth.requireRole(c,student,"student"); return questionsFor(c,examId);
        }
    }
    /** Calculates scores from stored correct answers and commits the attempt and answers together. */
    public Result submit(User student,long examId,Map<Long,Integer> answers) throws SQLException {
        try(Connection c=db.connect()) {
            c.setAutoCommit(false);
            try {
                UserAuth.requireRole(c,student,"student");
                List<Question> questions=questionsFor(c,examId);
                if(questions.isEmpty()) throw new IllegalArgumentException("Exam does not exist or has no questions.");
                if(answers.size()!=questions.size()) throw new IllegalArgumentException("Answer every question exactly once.");
                int score=0;
                for(Question q:questions) {
                    Integer answer=answers.get(q.id());
                    if(answer==null) throw new IllegalArgumentException("An answer is missing.");
                    Validation.option(answer);
                    if(answer==q.correctOption()) score++;
                }
                long attemptId;
                try(PreparedStatement p=c.prepareStatement("INSERT INTO attempts(user_id,exam_id,score,total_questions) VALUES(?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
                    p.setLong(1,student.id()); p.setLong(2,examId); p.setInt(3,score); p.setInt(4,questions.size());
                    p.executeUpdate(); attemptId=generatedId(p);
                }
                try(PreparedStatement p=c.prepareStatement("INSERT INTO attempt_answers(attempt_id,question_id,selected_option,is_correct) VALUES(?,?,?,?)")) {
                    for(Question q:questions) {
                        int answer=answers.get(q.id()); p.setLong(1,attemptId); p.setLong(2,q.id());
                        p.setInt(3,answer); p.setBoolean(4,answer==q.correctOption()); p.addBatch();
                    }
                    p.executeBatch();
                }
                Result result;
                try(PreparedStatement p=c.prepareStatement("SELECT e.name,a.submitted_at FROM attempts a JOIN exams e ON e.id=a.exam_id WHERE a.id=?")) {
                    p.setLong(1,attemptId);
                    try(ResultSet r=p.executeQuery()) {
                        if(!r.next()) throw new SQLException("Saved attempt was not found.");
                        result=new Result(attemptId,r.getString(1),score,questions.size(),r.getString(2));
                    }
                }
                c.commit(); return result;
            } catch(SQLException | RuntimeException e) { rollback(c,e); throw e; }
        }
    }
    public List<Result> results(User student) throws SQLException {
        try(Connection c=db.connect()) {
            UserAuth.requireRole(c,student,"student");
            try(PreparedStatement p=c.prepareStatement("SELECT a.id,e.name,a.score,a.total_questions,a.submitted_at FROM attempts a JOIN exams e ON e.id=a.exam_id WHERE a.user_id=? ORDER BY a.id DESC")) {
                p.setLong(1,student.id());
                try(ResultSet r=p.executeQuery()) {
                    List<Result> results=new ArrayList<>();
                    while(r.next()) results.add(new Result(r.getLong(1),r.getString(2),r.getInt(3),r.getInt(4),r.getString(5)));
                    return results;
                }
            }
        }
    }
    private List<Question> questionsFor(Connection c,long examId) throws SQLException {
        try(PreparedStatement p=c.prepareStatement("SELECT q.* FROM exam_questions eq JOIN questions q ON q.id=eq.question_id WHERE eq.exam_id=? ORDER BY eq.position")) {
            p.setLong(1,examId);
            try(ResultSet r=p.executeQuery()) {
                List<Question> result=new ArrayList<>(); while(r.next()) result.add(question(r)); return result;
            }
        }
    }
    private Question question(ResultSet r) throws SQLException {
        return new Question(r.getLong("id"),r.getString("question_text"),List.of(r.getString("option1"),r.getString("option2"),r.getString("option3"),r.getString("option4")),r.getInt("correct_option"));
    }
    private long generatedId(PreparedStatement p) throws SQLException {
        try(ResultSet r=p.getGeneratedKeys()) { if(!r.next()) throw new SQLException("No generated ID returned."); return r.getLong(1); }
    }
    private void rollback(Connection c,Exception original) {
        try { c.rollback(); } catch(SQLException e) { original.addSuppressed(e); }
    }
}
