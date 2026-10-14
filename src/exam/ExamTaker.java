package exam;
import exam.Models.*;
import java.sql.SQLException;
import java.util.*;

/** Shows an exam, collects answers and requests a database-calculated score. */
public final class ExamTaker {
    private final ExamManager manager;
    private final ConsoleInput input;
    public ExamTaker(ExamManager manager,ConsoleInput input) { this.manager=manager; this.input=input; }
    public void take(User student) throws SQLException {
        List<Exam> exams=manager.listExams();
        if(exams.isEmpty()) { System.out.println("No exams available. Ask an admin to create one."); return; }
        System.out.println("Available Exams:");
        for(Exam e:exams) System.out.println(e.id()+". "+e.name()+" ("+e.questionCount()+" questions)");
        long examId=input.id("Enter exam ID to take the exam: ");
        List<Question> questions=manager.examQuestions(student,examId);
        if(questions.isEmpty()) throw new IllegalArgumentException("Exam not found.");
        Map<Long,Integer> answers=new LinkedHashMap<>();
        int number=1;
        for(Question q:questions) {
            System.out.println("\nQuestion "+number++ +" of "+questions.size()+": "+q.text());
            for(int i=0;i<4;i++) System.out.println((i+1)+". "+q.options().get(i));
            answers.put(q.id(),input.choice("Your answer (1-4): ",1,4));
        }
        Result result=manager.submit(student,examId,answers);
        System.out.printf(Locale.ROOT,"Your score: %d/%d (%.1f%%)%n",result.score(),result.total(),result.percentage());
        System.out.println("Result saved. Attempt ID: "+result.attemptId());
    }
}
