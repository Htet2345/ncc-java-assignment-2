# How the code works

The console collects input. UserAuth handles accounts. ExamManager stores and retrieves
records through Database. ExamTaker presents questions and requests grading. MySQL is
the authoritative data store; there is no text-file or in-memory database substitute.

## Classes and methods

### OnlineExamApp
- main: loads database configuration, checks connectivity and starts the application.
- run: registration/login/exit loop, with friendly input and database errors.
- register: gathers account fields; wipes the temporary password array afterwards.
- login: checks credentials and opens the appropriate role menu.
- session: dispatches admin or student operations until logout.
- addQuestion: gathers text, four options and the correct option.
- createExam: shows available questions, parses chosen IDs and saves an exam.
- listQuestions / listExams: display database records.
- databaseError: reports common database failures without printing credentials.

### Database
- constructor: reads config.properties, with EXAM_DB_URL, EXAM_DB_USER,
  EXAM_DB_PASSWORD and EXAM_ADMIN_KEY environment overrides.
- config: selects environment value, file value or default.
- connect: opens a MySQL JDBC connection.
- checkConnection: verifies access to the initialized users table.
- allowsAdminRegistration: rejects an unset/default key and checks the configured key.

### UserAuth
- register: validates fields, authorizes admin creation, hashes the password and inserts a user.
- login: looks up the user and verifies the stored password hash.
- requireRole: rechecks the database role before protected operations.
- constructor: creates a dummy password hash used when a username is not found,
  reducing a basic timing difference between missing users and wrong passwords.

### PasswordHasher
- hash: random 16-byte salt, PBKDF2-HMAC-SHA256, 600,000 iterations, 256-bit output.
- verify: validates the stored format and compares derived bytes in constant time.
- derive: computes PBKDF2 and clears the PBEKeySpec password.
- encode: converts binary salt/hash to Base64 for database storage.
No raw password is stored. IDE fallback input may be visible; a real System.console
uses readPassword. Password Strings created by Scanner cannot be wiped from JVM memory.

### ExamManager
- addQuestion: validates four options and persists a question, returning its ID.
- listQuestions: retrieves the question bank for an admin.
- createExam: validates all chosen IDs in one query; stores exam and links in a transaction.
- listExams: retrieves exams and counts in one grouped query.
- examQuestions: returns a student's selected exam questions in defined order.
- submit: re-fetches correct answers, calculates score and stores attempt/answers atomically.
- results: returns only the logged-in student's result history.
- questionsFor / question: SQL query and row-to-model helpers.
- generatedId: obtains the MySQL-generated ID.
- rollback: reverses an incomplete transaction, preserving a rollback error as suppressed.

### ExamTaker
- take: lists exams, collects one answer per question, submits and displays the result.

### ConsoleInput
- read: reads complete lines; handles end of input.
- choice: repeats until a numeric choice is within the requested range.
- id: repeats until a positive numeric ID is entered.
- password: hidden console input when supported; visible IDE fallback with a notice.
- EndOfInput: clean termination when input closes.

### Validation
- text / username / password / option / positiveId: shared field validation.
- questionIds: parses comma-separated IDs, rejects invalid/duplicate IDs and preserves order.

### Models
Immutable User, Question, Exam and Result records. Question copies its option list.
User.isAdmin selects the menu; Result.percentage calculates the displayed percentage.
Server-side roles are checked separately by protected database operations.

## MySQL schema

- users: unique username, password hash, role.
- questions: question text, four options, correct answer and creator.
- exams: unique name and creator.
- exam_questions: many-to-many links with an explicit question position.
- attempts: student, exam, final score, question count and submission timestamp.
- attempt_answers: each submitted answer and its correctness snapshot.

Foreign keys prevent orphan records. CHECK constraints enforce valid answer ranges,
positive positions and possible scores. The user/result index speeds student history.
Both exam creation and result submission use transactions; failed validation leaves
no partial exam or result. PreparedStatement parameterizes user-provided values.
The variable IN clause contains only generated ? placeholders, never pasted input.
try-with-resources closes JDBC connections, statements and result sets.

## Review decisions and limits

A small console assignment needs simple per-operation connections, not a connection
pool or web framework. Batched link/answer inserts and joined queries avoid repeated
per-row reads. The application provides no question-edit/delete feature, matching the
demo; this also keeps existing exam questions stable under normal application use.
If someone manually changes questions in MySQL during an exam, grading uses the current
stored answers at submission. Full question/option versioning would be a further feature.

This is a local educational console app, not a secure remotely hosted examination
service. A person controlling its machine/configuration can access its MySQL credentials.
No HTTP API, proctoring, time limit, login rate limit, account recovery or exam scheduling
was demonstrated, so those features are outside this implementation.
Admin registration key is an improvement over unrestricted admin self-registration.
Do not distribute that private key to students. For remote MySQL, configure verified
TLS (sslMode=VERIFY_IDENTITY with a trusted certificate) rather than the local default.
