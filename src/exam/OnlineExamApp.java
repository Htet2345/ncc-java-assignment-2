package exam;
import exam.Models.*;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/** Application entry point. Console UI is separated from JDBC business operations. */
public final class OnlineExamApp {
    private final ConsoleInput input=new ConsoleInput();
    private final UserAuth auth;
    private final ExamManager manager;
    private final ExamTaker taker;
    private OnlineExamApp(Database db) {
        auth=new UserAuth(db); manager=new ExamManager(db); taker=new ExamTaker(manager,input);
    }
    public static void main(String[] args) {
        try {
            Database db=new Database(); db.checkConnection();
            new OnlineExamApp(db).run();
        } catch(IOException e) { System.err.println("Cannot read config.properties: "+e.getMessage()); }
        catch(SQLException e) { databaseError(e); }
        catch(IllegalArgumentException e) { System.err.println(e.getMessage()); }
    }
    private void run() {
        System.out.println("Welcome to the Online Exam System!");
        try {
            while(true) {
                System.out.println("\n1. Register\n2. Login\n3. Exit");
                int choice=input.choice("Choose an option: ",1,3);
                if(choice==3) { System.out.println("Goodbye!"); return; }
                try { if(choice==1) register(); else login(); }
                catch(IllegalArgumentException e) { System.out.println(e.getMessage()); }
                catch(SQLException e) { databaseError(e); }
            }
        } catch(ConsoleInput.EndOfInput e) { System.out.println("\nInput closed. Any incomplete exam was not submitted."); }
    }
    private void register() throws SQLException {
        String name=input.read("Enter username: ");
        char[] password=input.password("Enter password (8-128 characters): ");
        try {
            String role=input.read("Enter role (admin/student): ");
            String key=role.equalsIgnoreCase("admin") ? input.read("Admin registration key: ") : "";
            auth.register(name,password,role,key);
            System.out.println("Registration successful!");
        } finally { Arrays.fill(password,'\0'); }
    }
    private void login() throws SQLException {
        String name=input.read("Enter username: ");
        char[] password=input.password("Enter password: ");
        User user;
        try { user=auth.login(name,password); }
        finally { Arrays.fill(password,'\0'); }
        if(user==null) { System.out.println("Invalid username or password."); return; }
        System.out.println("Login successful! Welcome, "+user.username()+".");
        session(user);
    }
    private void session(User user) {
        while(true) {
            System.out.println(user.isAdmin()
                    ? "\nAdmin menu\n1. Add Question\n2. Create Exam\n3. List Questions\n4. List Exams\n5. Logout"
                    : "\nStudent menu\n1. Take Exam\n2. View My Results\n3. Logout");
            int n=input.choice("Choose an option: ",1,user.isAdmin()?5:3);
            if(n==(user.isAdmin()?5:3)) { System.out.println("Logged out."); return; }
            try {
                if(user.isAdmin()) {
                    switch(n) {
                        case 1 -> addQuestion(user);
                        case 2 -> createExam(user);
                        case 3 -> listQuestions(user);
                        case 4 -> listExams();
                        default -> throw new IllegalStateException("Unexpected option.");
                    }
                } else if(n==1) taker.take(user);
                else {
                    List<Result> results=manager.results(user);
                    if(results.isEmpty()) System.out.println("No saved results yet.");
                    for(Result r:results) System.out.printf(Locale.ROOT,"Attempt %d | %s | %d/%d | %.1f%% | %s UTC%n",r.attemptId(),r.examName(),r.score(),r.total(),r.percentage(),r.submittedAt());
                }
            } catch(IllegalArgumentException e) { System.out.println(e.getMessage()); }
            catch(SQLException e) { databaseError(e); }
        }
    }
    private void addQuestion(User admin) throws SQLException {
        String text=input.read("Enter question text: ");
        List<String> options=new ArrayList<>();
        for(int i=1;i<=4;i++) options.add(input.read("Enter option "+i+": "));
        int correct=input.choice("Enter correct option (1-4): ",1,4);
        long id=manager.addQuestion(admin,text,options,correct);
        System.out.println("Question added successfully! ID: "+id);
    }
    private void createExam(User admin) throws SQLException {
        List<Question> questions=manager.listQuestions(admin);
        for(Question q:questions) System.out.println(q.id()+". "+q.text());
        if(questions.isEmpty()) { System.out.println("Add questions first."); return; }
        String name=input.read("Enter exam name: ");
        List<Long> ids=Validation.questionIds(input.read("Enter question IDs (comma-separated): "));
        long id=manager.createExam(admin,name,ids);
        System.out.println("Exam created successfully! ID: "+id);
    }
    private void listQuestions(User admin) throws SQLException {
        List<Question> questions=manager.listQuestions(admin);
        if(questions.isEmpty()) System.out.println("No questions available.");
        for(Question q:questions) System.out.println(q.id()+". "+q.text()+" [correct option: "+q.correctOption()+"]");
    }
    private void listExams() throws SQLException {
        List<Exam> exams=manager.listExams();
        if(exams.isEmpty()) System.out.println("No exams available.");
        for(Exam e:exams) System.out.println(e.id()+". "+e.name()+" ("+e.questionCount()+" questions)");
    }
    private static void databaseError(SQLException e) {
        // Do not print database credentials or raw driver URLs.
        String state=e.getSQLState();
        if(e.getMessage()!=null && e.getMessage().contains("No suitable driver")) System.err.println("MySQL JDBC driver missing. Run with Maven: mvn compile exec:java.");
        else if(e.getErrorCode()==1045) System.err.println("MySQL access denied. Check db.user and db.password in config.properties.");
        else if(e.getErrorCode()==1049 || e.getErrorCode()==1146) System.err.println("Database or tables missing. Run sql/schema.sql in MySQL Workbench first.");
        else if(state!=null && state.startsWith("08")) System.err.println("Cannot connect to MySQL. Check that the server is running and the host/port are correct.");
        else System.err.println("Database operation failed (SQL state "+state+", code "+e.getErrorCode()+"). No success was reported. Check TESTING.md and database configuration.");
    }
}
