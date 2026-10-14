# Assignment 2 - Online Exam System (Java + MySQL)

A console application based on Assignment2.mp4. All persistent project data uses
MySQL through JDBC. It is not a browser website: "Online Exam System" is the name
used in the demo, which runs in an IDE console.

## Demo features implemented

- Register and log in with admin/student roles.
- Admin: add a question with four options and a correct option (1-4).
- Admin: create a named exam using comma-separated question IDs.
- Student: list available exams, select an exam, answer its questions and see a score.
- Added: logout, question/exam listing, percentages and persisted result history.

## Setup and run

Requirements: JDK 17 or later, Maven, and MySQL Server 8.0.16 or later.

1. Connect to your MySQL server using Workbench or the MySQL command-line client.
2. Execute sql/schema.sql to create the online_exam database and its six tables.
3. Edit the placeholder password in sql/create-app-user.sql, then execute it.
   This creates a restricted application account. If exam_app already exists,
   set its password separately; CREATE USER IF NOT EXISTS does not reset it.
4. Copy config.example.properties to config.properties in the repository root.
   Set db.password to the password from step 3. Set admin.registration.key to a
   private nonempty key of your choice. Keep this local file out of Git.
5. Open a terminal in the repository root and run:

```text
mvn compile exec:java
```

Maven downloads Connector/J automatically. The first build needs Internet access.
The program reads config.properties from the current working directory.
In VS Code, open this repository as a Maven project with Extension Pack for Java,
then run exam.OnlineExamApp after configuring MySQL. Use the integrated Terminal.

## First demo walkthrough

1. Choose Register. Username admin, password AdminPass123!, role admin.
   Enter the admin registration key you set in config.properties. These sample
   passwords are for a local exercise only; choose your own if using real accounts.
2. Choose Login and enter that account. Admin option 1 adds a question:
   Text: Which of the following is a NoSQL database?
   Options: PostgreSQL / MongoDB / MySQL / SQLite. Correct option: 2.
3. Add the second question:
   Text: Which language is primarily used for web development?
   Options: Python / C++ / JavaScript / Java. Correct option: 3.
4. Note the returned IDs. Choose Create Exam, name exam1 and enter those IDs,
   for example 1,2 on a fresh database. Existing databases may assign different IDs.
5. Logout, register alex / StudentPass123! / student, then log in.
6. Choose Take Exam and enter exam1's displayed numeric ID.
7. Answer 3 for the first question and 3 for the second. Score is 1/2 (50%).
8. Choose View My Results. Restart the app and log in again: records remain in MySQL.

Passwords now require 8-128 characters, so the video's admin/admin credentials are
intentionally replaced by stronger examples. Admin registration also needs the key.
Usernames are case-normalized and allow 3-50 letters/digits/underscores.
Exams may contain 1-100 unique question IDs. Students may retake exams; each completed
attempt gets a separate saved result. Incomplete exams are not saved.

## Troubleshooting

- Missing JDK/Maven: install them and reopen the terminal; check javac -version and mvn -version.
- Access denied: check db.user, db.password and the MySQL account's host.
- Missing database/tables: execute sql/schema.sql on the server specified by db.url.
- Connection refused: start MySQL Server and check the host and port.
- JDBC driver missing: use the Maven command above so the dependency is loaded.
- Admin key rejected: replace CHANGE_ME_ADMIN_KEY in your local configuration.
- Public key retrieval error: configure TLS on the MySQL connection. For a trusted
  localhost-only exercise, &allowPublicKeyRetrieval=true is a possible workaround;
  remote connections should use verified TLS instead.

## Quality review and validation

The code uses private/immutable data models, shared validation, parameterized SQL,
salted PBKDF2 password hashes, role checks, foreign keys and database constraints.
Exam creation and result submission are transactional. Question/answer inserts are
batched; joined queries avoid repeated per-question lookups. JDBC resources are closed.

Java 17 compilation passed with all warnings treated as errors. Twenty unit checks,
real MySQL 8.4.11 integration checks, and a complete scripted console workflow passed.
Checks covered registration/login, roles, validation, exam order, rollback, scoring,
persisted answers/history, and startup failure messages. Windows/VS Code GUI execution
was not directly tested. Use the walkthrough above to reproduce the demonstration.

CODE_GUIDE.md explains the classes, methods and database design.
Private config.properties, compiled binaries and local database files are excluded.
