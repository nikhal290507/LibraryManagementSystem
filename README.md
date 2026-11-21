# Advanced Library Management System (Java)
## University Project — Based on Lectures 10–60

A complete, production-grade, university-level Library Management System built strictly adhering to the university syllabus (Lectures 10–60). The project demonstrates practical, robust implementation of **Object-Oriented Programming (OOP), Custom Exception Handling, Java Collections Framework, Multithreading, Thread Synchronization, Inter-Thread Communication (wait/notify), JDBC Database Connectivity, PreparedStatement, Database Transactions (commit/rollback), Lambda Expressions, Stream API, Generics, and File Handling (CSV, Binary, StreamTokenizer)**.

---

## 1. Technology Stack

- **Language:** Java 17 / Java 21 / Java 25 (Standard Edition)
- **Database:** MySQL 8.0+ (Relational Database)
- **Database Driver:** MySQL Connector/J (`com.mysql:mysql-connector-j:8.3.0`)
- **Zero-Downtime Fallback:** Embedded H2 in MySQL Compatibility Mode (`com.h2database:h2:2.2.224`) ensures seamless execution in offline grading environments.
- **Build Tool:** Apache Maven 3.9+
- **Testing:** JUnit 5 (`junit-jupiter-api`, `junit-jupiter-engine`)
- **Interface:** Interactive Console Application driven by `java.util.Scanner`
- **Architectural Pattern:** Layered Architecture (Model-DAO-Service-Utils-Threads-Database)

---

## 2. Project Architecture & Directory Structure

```
LibraryManagementSystem/
│
├── src/main/java/
│   ├── model/
│   │   ├── Book.java                # Entity implementing Comparable<Book> and Serializable
│   │   ├── Member.java              # Entity representing registered library members
│   │   ├── Author.java              # Entity for book authors
│   │   ├── Category.java            # Entity for book categories
│   │   └── IssueRecord.java         # Entity for book loan transactions and fines
│   │
│   ├── dao/
│   │   ├── BookDAO.java             # PreparedStatement CRUD & atomic stock updates
│   │   ├── MemberDAO.java           # Member CRUD and active loan validation
│   │   ├── AuthorDAO.java           # Author CRUD with foreign-key referential integrity
│   │   ├── CategoryDAO.java         # Category CRUD with referential checks
│   │   └── IssueDAO.java            # Transaction creation, returns, fines, history
│   │
│   ├── service/
│   │   ├── BookService.java         # Collections (TreeSet, TreeMap, HashMap), Comparable/Comparator
│   │   ├── MemberService.java       # Member rules, LinkedList history, email/phone validation
│   │   └── LibraryService.java      # Transactions, commit/rollback, PriorityQueue, Deque
│   │
│   ├── database/
│   │   └── DBConnection.java        # JDBC connection factory with MySQL & embedded fallback
│   │
│   ├── exceptions/
│   │   ├── BookNotFoundException.java       # Custom unchecked exception
│   │   ├── MemberNotFoundException.java     # Custom unchecked exception
│   │   ├── BookUnavailableException.java   # Custom unchecked exception (out of stock)
│   │   ├── InvalidInputException.java       # Custom unchecked exception (validation failure)
│   │   ├── DuplicateISBNException.java      # Custom unchecked exception (duplicate catalog)
│   │   └── DatabaseException.java           # Custom checked exception (SQLException wrapping)
│   │
│   ├── threads/
│   │   ├── NotificationQueue.java   # Bounded queue with guarded wait() and notifyAll()
│   │   ├── NotificationProducer.java# Implements Runnable; generates system notifications
│   │   ├── NotificationConsumer.java# Implements Runnable; consumes system notifications
│   │   ├── LibraryBackgroundTask.java# Extends Thread; daemon checking overdue loans
│   │   ├── ThreadLifecycleDemo.java # Demonstrates all 6 thread states and priorities
│   │   └── ConcurrencyIssueDemo.java# Race condition vs synchronized book issuing demo
│   │
│   ├── utils/
│   │   ├── InputValidator.java      # Regex email, phone, ISBN, and range validation
│   │   ├── FileManager.java         # CSV I/O, binary I/O, StreamTokenizer, activity logs
│   │   └── ReportGenerator.java     # Stream API analytics (filter, map, sorted, groupingBy)
│   │
│   ├── generics/
│   │   ├── LibraryRepository.java   # Generic bounded class <T extends Serializable>
│   │   └── GenericUtils.java        # Generic methods, bounded type parameters, wildcards
│   │
│   └── Main.java                    # Menu-driven console interface with Scanner
│
├── src/main/resources/
│   ├── database.sql                 # MySQL schema DDL & seed data script
│   ├── db.properties                # Database credentials & business parameters
│   └── sample_books.csv             # Sample CSV data for batch import demonstration
│
├── src/test/java/
│   ├── BookServiceTest.java         # JUnit 5 tests for Book CRUD & validation
│   ├── MemberServiceTest.java       # JUnit 5 tests for Member registration & validation
│   ├── IssueReturnTest.java         # JUnit 5 tests for transactions & fine calculations
│   ├── ConcurrencyTest.java         # JUnit 5 tests for concurrent race condition protection
│   ├── CollectionsAndStreamTest.java# JUnit 5 tests for Comparable, Comparator, & Streams
│   └── FileHandlingTest.java        # JUnit 5 tests for CSV & binary file I/O
│
├── exports/                         # Directory for exported CSV, reports, and binary files
├── pom.xml                          # Maven dependencies & build configuration
└── README.md                        # Documentation and Viva preparation guide
```

---

## 3. Database Design & Schema

The database `library_management` contains 5 relational tables normalized to Third Normal Form (3NF):

### ER Relationships:
1. `authors (1) <----> (M) books` (Foreign Key `author_id` with `ON DELETE SET NULL`)
2. `categories (1) <----> (M) books` (Foreign Key `category_id` with `ON DELETE SET NULL`)
3. `members (1) <----> (M) issue_records` (Foreign Key `member_id` with referential check)
4. `books (1) <----> (M) issue_records` (Foreign Key `book_id` with referential check)

### Database Setup Instructions (MySQL):
1. Open MySQL Workbench, MySQL Shell, or command line:
   ```bash
   mysql -u root -p < src/main/resources/database.sql
   ```
2. Verify credentials in `src/main/resources/db.properties`:
   ```properties
   db.url=jdbc:mysql://localhost:3306/library_management?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
   db.user=root
   db.password=root
   ```
3. *Zero-Downtime Guarantee:* If MySQL service is offline, `DBConnection` automatically utilizes an embedded MySQL-compatible engine (`./library_management`), ensuring zero setup failures during viva grading.

---

## 4. How to Compile and Run the Application

### Option A: Using Maven (Recommended)
```bash
# 1. Compile project
mvn clean compile

# 2. Run unit and integration tests
mvn test

# 3. Launch interactive console application
mvn exec:java -Dexec.mainClass="Main"
```

### Option B: Build and Run Standalone JAR
```bash
mvn clean package
java -jar target/LibraryManagementSystem-1.0.0.jar
```

---

## 5. Core Modules & Business Logic

### 1. Book Management Module (CRUD)
- **Create:** Adds new books. Generates auto-increment ID. Enforces unique ISBN format validation.
- **Read:** Formatted tabular display. Searches by Book ID, Title, or Author.
- **Update:** Updates title, author, publisher, price, and total stock quantity.
- **Delete:** Strictly prevents deletion if any copies of the book are currently issued to members.

### 2. Member Management Module (CRUD)
- **Register:** Validates full name, email format (`user@domain.com`), and phone number (7-15 digits). Prevents duplicate emails.
- **Account Status:** Allows activating and deactivating accounts.
- **Borrowing Limit:** Inactive members cannot borrow books. Members with active loans at maximum threshold (default: 3 books) cannot borrow additional books.

### 3. Book Issue & Return Transactions (ACID Guaranteed)
- **Issue:**
  1. Validates member is active and below borrowing limit.
  2. Validates member does not already possess an active copy of the same book.
  3. Verifies available copies > 0.
  4. Starts SQL transaction (`conn.setAutoCommit(false)`).
  5. Atomically decrements available copies:
     `UPDATE books SET available_copies = available_copies - 1 WHERE book_id = ? AND available_copies > 0`
  6. Creates issue record with issue date (`LocalDate.now()`) and due date (`plusDays(14)`).
  7. Commits transaction (`conn.commit()`).
  8. If any failure occurs, executes `conn.rollback()`.
- **Return:**
  1. Validates active loan status (prevents returning an already returned book).
  2. Calculates overdue days via `ChronoUnit.DAYS.between(dueDate, returnDate)`.
  3. Calculates late fine (`overdueDays * finePerDay`).
  4. Starts SQL transaction, restores book inventory, records return date and fine status, and commits.

### 4. Fine Management
- Automatic daily fine calculation ($5.00/day overdue).
- Tracks pending vs paid fines.
- Allows immediate payment during book return or later fine clearance.

---

## 6. Syllabus Concepts to Code Mapping (Lectures 10–60)

| Lecture Range | University Syllabus Topic | Concrete Implementation in Project | Exact Class & Method |
|---|---|---|---|
| **Lectures 10–12** | Checked & Unchecked Exceptions | `DatabaseException` (Checked, extends `Exception`); `BookNotFoundException`, `MemberNotFoundException`, `BookUnavailableException`, `InvalidInputException` (Unchecked, extends `RuntimeException`) | `exceptions.*` |
| **Lectures 13–15** | Exception Handling Keywords | `try`, `catch`, `finally`, `throw`, `throws`, Multi-Catch (`catch (InvalidInputException \| DuplicateISBNException e)`), Try-with-resources auto-closing JDBC & File streams | `Main.java`, `DBConnection.java`, `BookService.java`, `FileManager.java` |
| **Lectures 16–18** | Collections: List & Set | `ArrayList` (tabular books/members), `LinkedList` (registration history), `HashSet` (CSV batch duplicate check), `LinkedHashSet` (recent search history in order), `TreeSet` (unique alphabetical titles) | `model.Book`, `BookService.java`, `MemberService.java`, `FileManager.java` |
| **Lectures 19–21** | Collections: Maps | `HashMap` (O(1) fast ID cache), `LinkedHashMap` (query history in insertion order), `TreeMap` (books sorted by ID) | `BookService.java` (`getBookById`, `getBooksSortedById`) |
| **Lectures 22–23** | Queue, Deque, PriorityQueue | `Queue` (Notification queue), `Deque` (recent double-ended user actions), `PriorityQueue` (overdue loans prioritized by longest overdue days) | `threads.NotificationQueue`, `LibraryService.java` (`getOverdueLoansPriorityQueue`) |
| **Lectures 24–25** | Comparable & Comparator | Natural ordering by title (`Book implements Comparable<Book>`); Custom sort by price, year, author (`Comparator.comparing(...)`); `Collections.sort`, `Collections.max`, `Collections.reverse` | `model.Book.compareTo()`, `service.BookService.sortBooksBy()` |
| **Lectures 26–29** | Multithreading Fundamentals | `Thread` class inheritance (`LibraryBackgroundTask extends Thread`), `Runnable` interface implementation (`NotificationProducer`, `NotificationConsumer`), `start()` vs `run()`, Daemon threads (`setDaemon(true)`), Thread priorities, Thread states (NEW, RUNNABLE, WAITING, BLOCKED, TIMED_WAITING, TERMINATED) | `threads.LibraryBackgroundTask`, `threads.NotificationProducer`, `threads.ThreadLifecycleDemo` |
| **Lectures 30–33** | Synchronization & Concurrency | Critical sections, Intrinsic locks, `synchronized` methods/blocks, Guarded blocks with `wait()` & `notifyAll()` in `while` loop, Race condition prevention on issuing last book copy | `threads.NotificationQueue.put/take`, `threads.ConcurrencyIssueDemo`, `dao.BookDAO.atomicDecrementAvailableCopies` |
| **Lectures 34–42** | JDBC & MySQL | `DriverManager`, `Connection`, `Statement` (static counts), `PreparedStatement` (SQL injection prevention), `ResultSet` cursor iteration, Typed getters, Database transactions (`setAutoCommit(false)`, `commit()`, `rollback()`) | `database.DBConnection`, `dao.BookDAO`, `dao.IssueDAO`, `service.LibraryService` |
| **Lectures 43–46** | Lambda Expressions & Stream API | Functional interfaces (`Predicate`, `Function`, `Comparator`), Method references (`Book::getTitle`), `filter()`, `map()`, `sorted()`, `distinct()`, `limit()`, `collect(Collectors.toList())`, `groupingBy` | `utils.ReportGenerator`, `service.BookService` |
| **Lectures 47–48** | Generics | Generic bounded class `LibraryRepository<T extends Serializable>`, Type safety, Diamond operator, Generic methods (`<R> List<R> transform(...)`), Upper-bounded wildcards (`List<? extends T>`) | `generics.LibraryRepository`, `generics.GenericUtils` |
| **Lectures 49–54** | File Handling | `FileWriter` & `BufferedWriter` (CSV export), `FileReader` & `BufferedReader` (CSV import), `FileOutputStream` & `FileInputStream` (binary backup), `StreamTokenizer` (token parsing) | `utils.FileManager` (`exportBooksToCSV`, `importBooksFromCSV`, `saveBinaryBackup`, `analyzeTextFileWithTokenizer`) |
| **Lectures 59–60** | Unit Testing & Debugging | Automated unit & integration test suites using JUnit 5 assertions (`assertEquals`, `assertThrows`, `assertTrue`) | `src/test/java/*` |

---

## 7. Sample Test Cases

| Test Case ID | Feature / Module | Input Data | Expected Output | Status |
|---|---|---|---|---|
| **TC-01** | Add Book Validation | Title: `""` (Empty), Author: `"Joshua Bloch"`, ISBN: `"978-0134685991"` | `InvalidInputException: Book Title cannot be empty` | PASS |
| **TC-02** | Duplicate ISBN Check | Add book with existing ISBN `"978-0134685991"` | `DuplicateISBNException: A book with ISBN already exists` | PASS |
| **TC-03** | Member Email Format | Name: `"Bob"`, Email: `"invalid-email"`, Phone: `"9876543210"` | `InvalidInputException: Invalid email format` | PASS |
| **TC-04** | Duplicate Email | Register member with existing email `"alice.j@university.edu"` | `InvalidInputException: A member with email is already registered` | PASS |
| **TC-05** | Issue Available Book | Member ID: `1`, Book ID: `1` (Copies: 5) | Issue record created, Book available copies becomes `4` | PASS |
| **TC-06** | Out-of-Stock Issue | Member ID: `2`, Book ID with `available_copies = 0` | `BookUnavailableException: Book is currently OUT OF STOCK` | PASS |
| **TC-07** | Inactive Member Loan | Member ID: `5` (`is_active = FALSE`), Book ID: `2` | `InvalidInputException: Member is INACTIVE and cannot borrow books` | PASS |
| **TC-08** | Max Borrowing Limit | Member has 3 active loans, attempts 4th | `InvalidInputException: Borrowing limit exceeded (Maximum: 3)` | PASS |
| **TC-09** | Double Return Check | Return Issue ID that has already been returned | `InvalidInputException: Book has already been returned` | PASS |
| **TC-10** | Delete Book with Active Loan | Delete Book ID referenced by active loan | `DatabaseException: Copies of this book are currently issued` | PASS |
| **TC-11** | Overdue Fine Calculation | Return 5 days past due date (Rate: $5.00/day) | Fine calculated as `$25.00` | PASS |
| **TC-12** | Concurrent Race Condition | 2 threads simultaneously issuing the last 1 copy | Thread 1 succeeds; Thread 2 fails; Final stock is `0` (never `-1`) | PASS |
| **TC-13** | Producer-Consumer Queue | Producer fills queue of capacity 3; Consumer reads | Producer waits when full; Consumer waits when empty; No deadlock | PASS |
| **TC-14** | CSV Batch Import | Import `sample_books.csv` with valid rows and 1 duplicate | Valid rows inserted; duplicate safely skipped without crash | PASS |

---

## 8. University Viva Questions & Answers

### Q1: What is the difference between Checked and Unchecked Exceptions in Java?
**Answer:**
- **Checked Exceptions** inherit from `java.lang.Exception` (excluding `RuntimeException`). The Java compiler forces them to be either caught using `try-catch` or declared in the method signature using `throws`. In our project, `DatabaseException` is a checked exception wrapping SQL errors.
- **Unchecked Exceptions** inherit from `java.lang.RuntimeException`. The compiler does not force explicit handling. They represent programming errors or business rule violations. In our project, `BookNotFoundException`, `MemberNotFoundException`, `BookUnavailableException`, and `InvalidInputException` are unchecked exceptions.

### Q2: Why did you use `PreparedStatement` instead of `Statement`?
**Answer:**
1. **Security (SQL Injection Prevention):** `PreparedStatement` pre-compiles SQL queries and parameterizes input with typed placeholders (`?`), treating input strictly as data rather than executable SQL code.
2. **Performance:** Pre-compiled SQL statements can be cached and reused by the database query execution planner.
3. **Type Handling:** Automatic type conversions for complex types such as `java.sql.Date`, `Integer`, and `Double`.

### Q3: How do you guarantee data integrity when issuing a book?
**Answer:**
We use **Database Transactions** and **Atomic Conditional Updates**:
1. We disable auto-commit (`conn.setAutoCommit(false)`).
2. We execute an atomic update:
   `UPDATE books SET available_copies = available_copies - 1 WHERE book_id = ? AND available_copies > 0;`
   If 0 rows are affected, it means another thread issued the last copy; we roll back and throw `BookUnavailableException`.
3. We insert the loan record into `issue_records`.
4. We call `conn.commit()`.
5. If any `SQLException` occurs, `conn.rollback()` executes inside the `catch` block to revert any partial changes.

### Q4: Explain the difference between `Thread.sleep()` and `Object.wait()`.
**Answer:**
- `sleep()` is a static method of `java.lang.Thread`. It pauses execution for a specified duration, but **does NOT release intrinsic monitor locks** that the thread currently holds.
- `wait()` is an instance method of `java.lang.Object`. It can only be called from within a `synchronized` context. When invoked, it **releases the monitor lock** and places the thread in the `WAITING` state until another thread invokes `notify()` or `notifyAll()` on that same object.

### Q5: Why is `wait()` called inside a `while` loop rather than an `if` statement?
**Answer:**
To guard against **spurious wakeups** (where a thread wakes up without an explicit notification) and **race conditions** between multiple waiting threads. In our `NotificationQueue`:
```java
while (queue.size() >= capacity) {
    wait();
}
```
When notified, the thread re-evaluates the condition. If another consumer has already filled or emptied the queue in the interim, the thread safely re-enters the waiting state.

### Q6: What is the difference between `Comparable` and `Comparator`?
**Answer:**
- `Comparable<T>` defines the **natural ordering** of an entity by overriding `compareTo(T other)`. Our `Book` class implements `Comparable<Book>` to naturally sort books alphabetically by title.
- `Comparator<T>` defines **external, custom sorting strategies** by implementing `compare(T o1, T o2)` or using lambda expressions. Our project uses `Comparator` to sort books dynamically by publication year, price, author, or available copies.

### Q7: Why did you use a `PriorityQueue` for overdue loans?
**Answer:**
A standard queue processes elements FIFO (First-In, First-Out). A `PriorityQueue` orders elements according to a `Comparator`. In our `LibraryService`, we configure the `PriorityQueue` to sort overdue loans by `calculateOverdueDays()` descending, ensuring that the most critically overdue loans with the highest accrued fines are processed and displayed first.

### Q8: What are Daemon threads and how are they used in this project?
**Answer:**
A daemon thread is a low-priority background support thread that does not prevent the Java Virtual Machine (JVM) from exiting when all user threads finish execution. In our project, `LibraryBackgroundTask` is configured as a daemon thread via `setDaemon(true)`. It runs in the background checking for overdue loans without blocking application shutdown when the user selects exit.

### Q9: How does the Stream API improve code quality compared to traditional loops?
**Answer:**
The Stream API allows **declarative, functional data processing**. In `ReportGenerator.java`, instead of multiple nested loops with mutable state variables, we use stream pipelines (`filter`, `map`, `sorted`, `collect`, `groupingBy`) to filter available books, calculate revenue sums, extract distinct categories, and find the top 5 most borrowed books concisely, readably, and with less error-prone code.

### Q10: How does the application handle zero-setup grading?
**Answer:**
`DBConnection` implements an adaptive connection strategy:
1. It attempts to connect to the configured MySQL instance on `localhost:3306`.
2. If MySQL is offline (e.g. during rapid evaluation on a professor's laptop), it automatically falls back to an embedded H2 engine running in complete `MODE=MySQL` compatibility.
3. It auto-executes `database.sql` to initialize tables and seed data if not present. This guarantees that all CRUD, transaction, and concurrency features run immediately without database setup errors.
