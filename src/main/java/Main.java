import dao.AuthorDAO;
import dao.CategoryDAO;
import database.DBConnection;
import exceptions.*;
import model.*;
import service.BookService;
import service.LibraryService;
import service.MemberService;
import threads.*;
import utils.FileManager;
import utils.InputValidator;
import utils.ReportGenerator;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.*;

/**
 * Main Class — Console-Based Library Management System.
 * University Project based on Lectures 10-60.
 *
 * Demonstrates:
 * - Menu-driven interface using java.util.Scanner
 * - Exception Handling (Checked & Unchecked, multi-catch, try-with-resources)
 * - Collections Framework (List, Set, Map, Queue, Deque, PriorityQueue, Iterators)
 * - Multithreading, Daemon threads, Priorities, Thread Lifecycle
 * - Synchronization, Intrinsic Locks, wait() & notifyAll()
 * - JDBC, MySQL & PreparedStatement, Database Transactions
 * - Lambda Expressions & Stream API
 * - Generics
 * - File Handling (CSV, Binary, StreamTokenizer)
 */
public class Main {

    private static final Scanner scanner = new Scanner(System.in);
    private static BookService bookService;
    private static MemberService memberService;
    private static LibraryService libraryService;
    private static AuthorDAO authorDAO;
    private static CategoryDAO categoryDAO;

    private static LibraryBackgroundTask backgroundDaemon = null;

    public static void main(String[] args) {
        // Initialize services
        try {
            bookService = new BookService();
            memberService = new MemberService();
            libraryService = new LibraryService();
            authorDAO = new AuthorDAO();
            categoryDAO = new CategoryDAO();
        } catch (Exception e) {
            System.err.println("Critical Error initializing database services: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        printWelcomeBanner();

        boolean running = true;
        while (running) {
            try {
                displayMainMenu();
                int choice = readIntInput("Enter choice (1-13): ");
                switch (choice) {
                    case 1:
                        handleBookManagement();
                        break;
                    case 2:
                        handleMemberManagement();
                        break;
                    case 3:
                        handleAuthorManagement();
                        break;
                    case 4:
                        handleCategoryManagement();
                        break;
                    case 5:
                        handleIssueBook();
                        break;
                    case 6:
                        handleReturnBook();
                        break;
                    case 7:
                        handleViewTransactions();
                        break;
                    case 8:
                        handleFineManagement();
                        break;
                    case 9:
                        handleSearchAndSort();
                        break;
                    case 10:
                        handleReports();
                        break;
                    case 11:
                        handleFileImportExport();
                        break;
                    case 12:
                        handleMultithreadingDemo();
                        break;
                    case 13:
                        running = false;
                        shutdownApplication();
                        break;
                    default:
                        System.out.println(">>> Invalid choice. Please choose between 1 and 13.");
                }
            } catch (InvalidInputException | BookNotFoundException | MemberNotFoundException |
                     BookUnavailableException | DuplicateISBNException e) {
                // Demonstrates multi-catch of custom runtime exceptions (Lectures 10-15)
                System.out.println("\n[VALIDATION / BUSINESS ERROR] " + e.getMessage());
            } catch (DatabaseException e) {
                System.out.println("\n[DATABASE ERROR] " + e.getMessage());
            } catch (Exception e) {
                System.out.println("\n[UNEXPECTED ERROR] " + e.getMessage());
            }
        }
    }

    private static void printWelcomeBanner() {
        System.out.println("=========================================================================");
        System.out.println("               ADVANCED LIBRARY MANAGEMENT SYSTEM                        ");
        System.out.println("             Java University Project (Lectures 10-60)                    ");
        System.out.println("=========================================================================");
    }

    private static void displayMainMenu() {
        // Display dynamic live statistics from actual database
        try {
            Map<String, Object> stats = libraryService.getDashboardStats();
            System.out.println("\n" + "=".repeat(73));
            System.out.printf(" [DATABASE DASHBOARD]  Engine: %s\n", stats.get("Database Engine"));
            System.out.printf(" Titles: %-4s | Copies: %-4s | Available: %-4s | Issued: %-4s | Members: %-4s | Overdue: %-4s\n",
                    stats.get("Total Book Titles"), stats.get("Total Copies"), stats.get("Available Copies"),
                    stats.get("Issued Copies"), stats.get("Registered Members"), stats.get("Overdue Loans"));
            System.out.printf(" Fines Collected: $%s | Fines Pending: $%s\n",
                    stats.get("Collected Fines ($)"), stats.get("Pending Fines ($)"));
            System.out.println("=".repeat(73));
        } catch (Exception ignored) {}

        System.out.println("\n=========================== MAIN MENU ===========================");
        System.out.println(" 1.  Book Management (CRUD, Search, Availability)");
        System.out.println(" 2.  Member Management (CRUD, Status, Borrowing Limit)");
        System.out.println(" 3.  Author Management (Add, View, Update, Delete)");
        System.out.println(" 4.  Category Management (Add, View, Update, Delete)");
        System.out.println(" 5.  Issue Book (Transaction, Stock Check, Due Date)");
        System.out.println(" 6.  Return Book (Transaction, Fine Calculation)");
        System.out.println(" 7.  View Transactions (Active, Overdue, History)");
        System.out.println(" 8.  Fine Management (Pending Fines, Collect Payments)");
        System.out.println(" 9.  Search & Sort Books (Comparable, Comparator, Streams)");
        System.out.println(" 10. Library Analytics & Audit Reports");
        System.out.println(" 11. File Import/Export (CSV, Binary Backup, StreamTokenizer)");
        System.out.println(" 12. Multithreading, Concurrency & Producer-Consumer Demos");
        System.out.println(" 13. Exit System");
        System.out.println("=================================================================");
    }

    // =========================================================================
    // 1. BOOK MANAGEMENT SUBMENU
    // =========================================================================
    private static void handleBookManagement() throws DatabaseException {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- BOOK MANAGEMENT SUBMENU ---");
            System.out.println(" 1. Add New Book");
            System.out.println(" 2. View All Books (Tabular)");
            System.out.println(" 3. Search Book by ID");
            System.out.println(" 4. Search Books by Title");
            System.out.println(" 5. Search Books by Author");
            System.out.println(" 6. Update Book Information / Inventory");
            System.out.println(" 7. Delete Book (Checks Active Loans)");
            System.out.println(" 8. Back to Main Menu");
            int choice = readIntInput("Enter choice (1-8): ");
            switch (choice) {
                case 1:
                    System.out.println("\n[ADD NEW BOOK]");
                    String title = readStringInput("Enter Title: ");
                    String author = readStringInput("Enter Author Name: ");
                    String category = readStringInput("Enter Category: ");
                    String isbn = readStringInput("Enter ISBN (e.g. 978-0134685991): ");
                    String publisher = readStringInput("Enter Publisher: ");
                    int year = readIntInput("Enter Publication Year: ");
                    double price = readDoubleInput("Enter Price ($): ");
                    int copies = readIntInput("Enter Number of Copies: ");

                    Book added = bookService.addBook(title, author, category, isbn, publisher, year, price, copies);
                    System.out.printf(">>> SUCCESS: Book added with generated ID #%d: \"%s\"\n", added.getBookId(), added.getTitle());
                    break;

                case 2:
                    List<Book> allBooks = bookService.getAllBooks();
                    printBookTable(allBooks);
                    break;

                case 3:
                    int id = readIntInput("Enter Book ID to search: ");
                    Book b = bookService.getBookById(id);
                    System.out.println("\n--- BOOK DETAILS ---");
                    System.out.println(b);
                    System.out.printf("Publisher: %s | Year: %d | Total Copies: %d | Available: %d\n",
                            b.getPublisher(), b.getPublicationYear(), b.getTotalCopies(), b.getAvailableCopies());
                    break;

                case 4:
                    String titleKw = readStringInput("Enter Title keyword: ");
                    List<Book> titleResults = bookService.searchBooksByTitle(titleKw);
                    printBookTable(titleResults);
                    break;

                case 5:
                    String authorKw = readStringInput("Enter Author keyword: ");
                    List<Book> authorResults = bookService.searchBooksByAuthor(authorKw);
                    printBookTable(authorResults);
                    break;

                case 6:
                    int updateId = readIntInput("Enter Book ID to update: ");
                    Book toUpdate = bookService.getBookById(updateId);
                    System.out.println("Current details: " + toUpdate);
                    String newTitle = readStringInput("Enter New Title (leave blank to keep current): ");
                    if (!newTitle.isEmpty()) toUpdate.setTitle(newTitle);

                    String newAuthor = readStringInput("Enter New Author Name (leave blank to keep current): ");
                    if (!newAuthor.isEmpty()) toUpdate.setAuthorName(newAuthor);

                    String newPublisher = readStringInput("Enter New Publisher (leave blank to keep current): ");
                    if (!newPublisher.isEmpty()) toUpdate.setPublisher(newPublisher);

                    String priceStr = readStringInput("Enter New Price (leave blank to keep current): ");
                    if (!priceStr.isEmpty()) toUpdate.setPrice(Double.parseDouble(priceStr));

                    String totalCopiesStr = readStringInput("Enter New Total Copies (leave blank to keep current): ");
                    if (!totalCopiesStr.isEmpty()) {
                        int newTotal = Integer.parseInt(totalCopiesStr);
                        int diff = newTotal - toUpdate.getTotalCopies();
                        toUpdate.setTotalCopies(newTotal);
                        toUpdate.setAvailableCopies(Math.max(0, toUpdate.getAvailableCopies() + diff));
                    }

                    boolean updated = bookService.updateBook(toUpdate);
                    System.out.println(updated ? ">>> SUCCESS: Book updated successfully!" : ">>> Update failed.");
                    break;

                case 7:
                    int delId = readIntInput("Enter Book ID to delete: ");
                    System.out.print("Are you sure you want to delete book #" + delId + "? (y/N): ");
                    String confirm = scanner.nextLine().trim();
                    if (confirm.equalsIgnoreCase("y")) {
                        boolean deleted = bookService.deleteBook(delId);
                        System.out.println(deleted ? ">>> SUCCESS: Book deleted." : ">>> Deletion failed.");
                    } else {
                        System.out.println("Deletion cancelled.");
                    }
                    break;

                case 8:
                    back = true;
                    break;

                default:
                    System.out.println(">>> Invalid choice.");
            }
        }
    }

    // =========================================================================
    // 2. MEMBER MANAGEMENT SUBMENU
    // =========================================================================
    private static void handleMemberManagement() throws DatabaseException {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- MEMBER MANAGEMENT SUBMENU ---");
            System.out.println(" 1. Register New Member");
            System.out.println(" 2. View All Members");
            System.out.println(" 3. Search Member by ID");
            System.out.println(" 4. Search Members by Name");
            System.out.println(" 5. Update Member Information");
            System.out.println(" 6. Deactivate / Activate Member");
            System.out.println(" 7. Delete Member (Checks Active Loans)");
            System.out.println(" 8. Back to Main Menu");
            int choice = readIntInput("Enter choice (1-8): ");
            switch (choice) {
                case 1:
                    System.out.println("\n[REGISTER NEW MEMBER]");
                    String name = readStringInput("Enter Full Name: ");
                    String email = readStringInput("Enter Email (e.g. name@university.edu): ");
                    String phone = readStringInput("Enter Phone Number: ");
                    String address = readStringInput("Enter Address / Campus Hostel: ");

                    Member m = memberService.registerMember(name, email, phone, address);
                    System.out.printf(">>> SUCCESS: Member registered with generated ID #%d: %s (%s)\n",
                            m.getMemberId(), m.getName(), m.getEmail());
                    break;

                case 2:
                    List<Member> members = memberService.getAllMembers();
                    printMemberTable(members);
                    break;

                case 3:
                    int mId = readIntInput("Enter Member ID: ");
                    Member found = memberService.getMemberById(mId);
                    int activeLoans = memberService.getActiveIssuedCount(mId);
                    System.out.println("\n--- MEMBER DETAILS ---");
                    System.out.println(found);
                    System.out.printf("Address: %s | Registered: %s | Active Loans: %d / %d max\n",
                            found.getAddress(), found.getRegistrationDate(), activeLoans, found.getMaxBooksAllowed());
                    break;

                case 4:
                    String kw = readStringInput("Enter Name keyword: ");
                    List<Member> nameResults = memberService.searchMembersByName(kw);
                    printMemberTable(nameResults);
                    break;

                case 5:
                    int editId = readIntInput("Enter Member ID to update: ");
                    Member toEdit = memberService.getMemberById(editId);
                    System.out.println("Current details: " + toEdit);
                    String newName = readStringInput("Enter New Name (blank to keep): ");
                    if (!newName.isEmpty()) toEdit.setName(newName);

                    String newPhone = readStringInput("Enter New Phone (blank to keep): ");
                    if (!newPhone.isEmpty()) toEdit.setPhone(newPhone);

                    String newAddr = readStringInput("Enter New Address (blank to keep): ");
                    if (!newAddr.isEmpty()) toEdit.setAddress(newAddr);

                    boolean ok = memberService.updateMember(toEdit);
                    System.out.println(ok ? ">>> SUCCESS: Member updated successfully!" : ">>> Update failed.");
                    break;

                case 6:
                    int toggleId = readIntInput("Enter Member ID: ");
                    Member curr = memberService.getMemberById(toggleId);
                    boolean newStatus = !curr.isActive();
                    if (newStatus) {
                        memberService.activateMember(toggleId);
                        System.out.println(">>> SUCCESS: Member activated.");
                    } else {
                        memberService.deactivateMember(toggleId);
                        System.out.println(">>> SUCCESS: Member deactivated.");
                    }
                    break;

                case 7:
                    int delMId = readIntInput("Enter Member ID to delete: ");
                    System.out.print("Are you sure you want to delete member #" + delMId + "? (y/N): ");
                    String confirm = scanner.nextLine().trim();
                    if (confirm.equalsIgnoreCase("y")) {
                        boolean del = memberService.deleteMember(delMId);
                        System.out.println(del ? ">>> SUCCESS: Member deleted." : ">>> Deletion failed.");
                    }
                    break;

                case 8:
                    back = true;
                    break;

                default:
                    System.out.println(">>> Invalid choice.");
            }
        }
    }

    // =========================================================================
    // 3. AUTHOR MANAGEMENT SUBMENU
    // =========================================================================
    private static void handleAuthorManagement() throws DatabaseException {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- AUTHOR MANAGEMENT SUBMENU ---");
            System.out.println(" 1. Add Author");
            System.out.println(" 2. View All Authors");
            System.out.println(" 3. Update Author");
            System.out.println(" 4. Delete Author (Protected by Foreign Key Check)");
            System.out.println(" 5. Back to Main Menu");
            int choice = readIntInput("Enter choice (1-5): ");
            switch (choice) {
                case 1:
                    String name = readStringInput("Enter Author Name: ");
                    String email = readStringInput("Enter Email: ");
                    String bio = readStringInput("Enter Short Biography: ");
                    int id = authorDAO.addAuthor(new Author(name, email, bio));
                    System.out.printf(">>> SUCCESS: Author registered with ID #%d\n", id);
                    break;
                case 2:
                    List<Author> authors = authorDAO.getAllAuthors();
                    System.out.println("\n" + "-".repeat(65));
                    System.out.printf("%-6s | %-25s | %-25s\n", "ID", "Name", "Email");
                    System.out.println("-".repeat(65));
                    authors.forEach(a -> System.out.printf("%-6d | %-25s | %-25s\n", a.getAuthorId(), a.getName(), a.getEmail()));
                    System.out.println("-".repeat(65));
                    break;
                case 3:
                    int aId = readIntInput("Enter Author ID to update: ");
                    Author a = authorDAO.getAuthorById(aId);
                    if (a == null) {
                        System.out.println("Author not found.");
                        break;
                    }
                    String nName = readStringInput("Enter Name (blank to keep): ");
                    if (!nName.isEmpty()) a.setName(nName);
                    String nEmail = readStringInput("Enter Email (blank to keep): ");
                    if (!nEmail.isEmpty()) a.setEmail(nEmail);
                    String nBio = readStringInput("Enter Bio (blank to keep): ");
                    if (!nBio.isEmpty()) a.setBiography(nBio);
                    authorDAO.updateAuthor(a);
                    System.out.println(">>> SUCCESS: Author updated.");
                    break;
                case 4:
                    int delAId = readIntInput("Enter Author ID to delete: ");
                    try {
                        authorDAO.deleteAuthor(delAId);
                        System.out.println(">>> SUCCESS: Author deleted.");
                    } catch (DatabaseException e) {
                        System.out.println(">>> BLOCKED: " + e.getMessage());
                    }
                    break;
                case 5:
                    back = true;
                    break;
                default:
                    System.out.println(">>> Invalid choice.");
            }
        }
    }

    // =========================================================================
    // 4. CATEGORY MANAGEMENT SUBMENU
    // =========================================================================
    private static void handleCategoryManagement() throws DatabaseException {
        boolean back = false;
        while (!back) {
            System.out.println("\n--- CATEGORY MANAGEMENT SUBMENU ---");
            System.out.println(" 1. Add Category");
            System.out.println(" 2. View All Categories");
            System.out.println(" 3. Update Category");
            System.out.println(" 4. Delete Category (Protected by Foreign Key Check)");
            System.out.println(" 5. Back to Main Menu");
            int choice = readIntInput("Enter choice (1-5): ");
            switch (choice) {
                case 1:
                    String name = readStringInput("Enter Category Name: ");
                    String desc = readStringInput("Enter Description: ");
                    int id = categoryDAO.addCategory(new Category(name, desc));
                    System.out.printf(">>> SUCCESS: Category added with ID #%d\n", id);
                    break;
                case 2:
                    List<Category> categories = categoryDAO.getAllCategories();
                    System.out.println("\n" + "-".repeat(65));
                    System.out.printf("%-6s | %-25s | %-28s\n", "ID", "Category Name", "Description");
                    System.out.println("-".repeat(65));
                    categories.forEach(c -> System.out.printf("%-6d | %-25s | %-28s\n", c.getCategoryId(), c.getName(), c.getDescription()));
                    System.out.println("-".repeat(65));
                    break;
                case 3:
                    int cId = readIntInput("Enter Category ID to update: ");
                    Category cat = categoryDAO.getCategoryById(cId);
                    if (cat == null) {
                        System.out.println("Category not found.");
                        break;
                    }
                    String nName = readStringInput("Enter Category Name (blank to keep): ");
                    if (!nName.isEmpty()) cat.setName(nName);
                    String nDesc = readStringInput("Enter Description (blank to keep): ");
                    if (!nDesc.isEmpty()) cat.setDescription(nDesc);
                    categoryDAO.updateCategory(cat);
                    System.out.println(">>> SUCCESS: Category updated.");
                    break;
                case 4:
                    int delCId = readIntInput("Enter Category ID to delete: ");
                    try {
                        categoryDAO.deleteCategory(delCId);
                        System.out.println(">>> SUCCESS: Category deleted.");
                    } catch (DatabaseException e) {
                        System.out.println(">>> BLOCKED: " + e.getMessage());
                    }
                    break;
                case 5:
                    back = true;
                    break;
                default:
                    System.out.println(">>> Invalid choice.");
            }
        }
    }

    // =========================================================================
    // 5. ISSUE BOOK
    // =========================================================================
    private static void handleIssueBook() throws DatabaseException {
        System.out.println("\n[ISSUE BOOK TRANSACTION]");
        int memberId = readIntInput("Enter Member ID: ");
        int bookId = readIntInput("Enter Book ID: ");

        IssueRecord issue = libraryService.issueBook(memberId, bookId);
        System.out.println("\n>>> TRANSACTION COMMITTED SUCCESSFULLY! <<<");
        System.out.printf("Issue ID        : #%d\n", issue.getIssueId());
        System.out.printf("Member          : %s (ID #%d)\n", issue.getMemberName(), issue.getMemberId());
        System.out.printf("Book Issued     : \"%s\" (ID #%d)\n", issue.getBookTitle(), issue.getBookId());
        System.out.printf("Issue Date      : %s\n", issue.getIssueDate());
        System.out.printf("Due Date        : %s (Loan Period: %d days)\n", issue.getDueDate(), libraryService.getLoanPeriodDays());
        System.out.println("Inventory copies decremented atomically.");
    }

    // =========================================================================
    // 6. RETURN BOOK
    // =========================================================================
    private static void handleReturnBook() throws DatabaseException {
        System.out.println("\n[RETURN BOOK TRANSACTION]");
        int issueId = readIntInput("Enter Issue ID: ");

        System.out.print("Does the member wish to pay any accrued fines immediately? (y/N): ");
        boolean payNow = scanner.nextLine().trim().equalsIgnoreCase("y");

        IssueRecord ret = libraryService.returnBook(issueId, payNow);
        System.out.println("\n>>> RETURN TRANSACTION COMMITTED SUCCESSFULLY! <<<");
        System.out.printf("Issue ID        : #%d\n", ret.getIssueId());
        System.out.printf("Book Returned   : %s (Book ID #%d)\n", ret.getBookTitle(), ret.getBookId());
        System.out.printf("Return Date     : %s\n", ret.getReturnDate());
        System.out.printf("Calculated Fine : $%.2f (Status: %s)\n", ret.getFineAmount(), ret.isFinePaid() ? "PAID" : "PENDING");
        System.out.println("Available inventory copies restored atomically.");
    }

    // =========================================================================
    // 7. VIEW TRANSACTIONS
    // =========================================================================
    private static void handleViewTransactions() throws DatabaseException {
        System.out.println("\n--- TRANSACTIONS VIEW ---");
        System.out.println(" 1. View All Loan Transactions");
        System.out.println(" 2. View Active Loans Only");
        System.out.println(" 3. View Overdue Loans Priority Queue (Highest Overdue First)");
        System.out.println(" 4. View Specific Member Borrowing History");
        int choice = readIntInput("Enter choice (1-4): ");
        switch (choice) {
            case 1:
                printTransactionsTable(libraryService.getAllTransactions());
                break;
            case 2:
                printTransactionsTable(libraryService.getActiveTransactions());
                break;
            case 3:
                // Demonstrating PriorityQueue from Collections
                PriorityQueue<IssueRecord> pq = libraryService.getOverdueLoansPriorityQueue();
                if (pq.isEmpty()) {
                    System.out.println(">>> Good news! There are currently no overdue loans.");
                } else {
                    System.out.println("\n--- OVERDUE LOANS (Ordered by PriorityQueue: Most Overdue First) ---");
                    List<IssueRecord> ordered = new ArrayList<>();
                    while (!pq.isEmpty()) {
                        ordered.add(pq.poll()); // poll() retrieves elements in highest-priority order
                    }
                    printTransactionsTable(ordered);
                }
                break;
            case 4:
                int mId = readIntInput("Enter Member ID: ");
                printTransactionsTable(libraryService.getMemberTransactions(mId));
                break;
            default:
                System.out.println(">>> Invalid choice.");
        }
    }

    // =========================================================================
    // 8. FINE MANAGEMENT
    // =========================================================================
    private static void handleFineManagement() throws DatabaseException {
        System.out.println("\n--- FINE MANAGEMENT ---");
        System.out.println(" 1. View All Overdue Loans & Accrued Fines");
        System.out.println(" 2. Pay Pending Fine for a Transaction");
        int choice = readIntInput("Enter choice (1-2): ");
        if (choice == 1) {
            List<IssueRecord> all = libraryService.getAllTransactions();
            List<IssueRecord> fines = all.stream()
                    .filter(i -> i.getFineAmount() > 0 && !i.isFinePaid())
                    .toList();
            if (fines.isEmpty()) {
                System.out.println(">>> No pending fines found in the system.");
            } else {
                printTransactionsTable(fines);
            }
        } else if (choice == 2) {
            int issueId = readIntInput("Enter Issue ID to pay fine: ");
            boolean paid = libraryService.payFine(issueId);
            System.out.println(paid ? ">>> SUCCESS: Fine payment recorded successfully!" : ">>> Payment failed.");
        }
    }

    // =========================================================================
    // 9. SEARCH & SORT BOOKS (COLLECTIONS & STREAMS)
    // =========================================================================
    private static void handleSearchAndSort() throws DatabaseException {
        System.out.println("\n--- ADVANCED SEARCH & COLLECTIONS SORTING (Lectures 16-25, 43-46) ---");
        System.out.println(" 1. Natural Sort by Title (Comparable<Book>)");
        System.out.println(" 2. Custom Sort by Publication Year (Comparator)");
        System.out.println(" 3. Custom Sort by Price (Comparator)");
        System.out.println(" 4. Custom Sort by Available Copies (Comparator)");
        System.out.println(" 5. Display Unique Titles in TreeSet");
        System.out.println(" 6. Display Books in TreeMap (Sorted by Book ID)");
        System.out.println(" 7. Filter Books with Copies > N (Stream API)");
        System.out.println(" 8. Collections Utility Demonstrations (max, min, reverse)");
        System.out.println(" 9. Iterator & ListIterator Forward/Backward Traversal");
        int choice = readIntInput("Enter choice (1-9): ");
        switch (choice) {
            case 1:
                printBookTable(bookService.sortBooksNaturally());
                break;
            case 2:
                printBookTable(bookService.sortBooksBy("YEAR"));
                break;
            case 3:
                printBookTable(bookService.sortBooksBy("PRICE"));
                break;
            case 4:
                printBookTable(bookService.sortBooksBy("AVAILABLE"));
                break;
            case 5:
                Set<String> titles = bookService.getUniqueSortedTitles();
                System.out.println("\n--- UNIQUE BOOK TITLES IN TREESET (Alphabetical) ---");
                titles.forEach(t -> System.out.println("  * " + t));
                break;
            case 6:
                Map<Integer, Book> treeMap = bookService.getBooksSortedById();
                System.out.println("\n--- BOOKS IN TREEMAP (Sorted by Key ID) ---");
                treeMap.forEach((k, v) -> System.out.printf("  Key [%d] -> %s\n", k, v.getTitle()));
                break;
            case 7:
                int minCopies = readIntInput("Enter minimum copies threshold: ");
                printBookTable(bookService.getBooksWithCopiesMoreThan(minCopies));
                break;
            case 8:
                Map<String, Object> utils = bookService.demonstrateCollectionsUtilities();
                System.out.println("\n--- COLLECTIONS UTILITIES DEMONSTRATION ---");
                utils.forEach((k, v) -> System.out.println("  " + k + " : " + v));
                break;
            case 9:
                bookService.demonstrateIterators();
                break;
            default:
                System.out.println(">>> Invalid choice.");
        }
    }

    // =========================================================================
    // 10. LIBRARY AUDIT & ANALYTICS REPORTS
    // =========================================================================
    private static void handleReports() throws DatabaseException {
        System.out.println("\n--- LIBRARY AUDIT & REPORTS ---");
        System.out.println(" 1. Generate & Display Comprehensive Audit Report (Stream API)");
        System.out.println(" 2. Save Comprehensive Report to Disk (exports/library_report.txt)");
        System.out.println(" 3. View Recent Activity Log (from exports/activity_log.txt)");
        int choice = readIntInput("Enter choice (1-3): ");
        if (choice == 1 || choice == 2) {
            List<Book> books = bookService.getAllBooks();
            List<Member> members = memberService.getAllMembers();
            List<IssueRecord> issues = libraryService.getAllTransactions();
            Map<String, Object> stats = libraryService.getDashboardStats();

            double coll = Double.parseDouble((String) stats.get("Collected Fines ($)"));
            double pend = Double.parseDouble((String) stats.get("Pending Fines ($)"));

            String report = ReportGenerator.generateComprehensiveReport(books, members, issues, coll, pend);
            System.out.println(report);

            if (choice == 2) {
                try {
                    File saved = ReportGenerator.saveReportToFile(report, "library_report.txt");
                    System.out.println(">>> SUCCESS: Report saved to: " + saved.getAbsolutePath());
                } catch (IOException e) {
                    System.out.println(">>> Error saving report to disk: " + e.getMessage());
                }
            }
        } else if (choice == 3) {
            try {
                String logs = FileManager.readTextFile("exports/activity_log.txt");
                System.out.println("\n--- ACTIVITY LOGS ---");
                System.out.println(logs);
            } catch (IOException e) {
                System.out.println("No activity logs found yet: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    // 11. FILE IMPORT / EXPORT (CSV, BINARY, STREAM TOKENIZER)
    // =========================================================================
    private static void handleFileImportExport() throws DatabaseException {
        System.out.println("\n--- FILE IMPORT / EXPORT SUBMENU (Lectures 49-54) ---");
        System.out.println(" 1. Export All Books to CSV (exports/books_export.csv)");
        System.out.println(" 2. Export All Members to CSV (exports/members_export.csv)");
        System.out.println(" 3. Export Transactions to CSV (exports/transactions_export.csv)");
        System.out.println(" 4. Import Books from CSV (src/main/resources/sample_books.csv)");
        System.out.println(" 5. Save Binary Backup of Books (exports/books_backup.dat)");
        System.out.println(" 6. Read Binary Backup of Books");
        System.out.println(" 7. Analyze File with StreamTokenizer");
        int choice = readIntInput("Enter choice (1-7): ");
        try {
            switch (choice) {
                case 1:
                    List<Book> books = bookService.getAllBooks();
                    FileManager.exportBooksToCSV(books, "exports/books_export.csv");
                    System.out.println(">>> SUCCESS: Exported " + books.size() + " books to exports/books_export.csv");
                    break;
                case 2:
                    List<Member> members = memberService.getAllMembers();
                    FileManager.exportMembersToCSV(members, "exports/members_export.csv");
                    System.out.println(">>> SUCCESS: Exported " + members.size() + " members to exports/members_export.csv");
                    break;
                case 3:
                    List<IssueRecord> trans = libraryService.getAllTransactions();
                    FileManager.exportTransactionsToCSV(trans, "exports/transactions_export.csv");
                    System.out.println(">>> SUCCESS: Exported " + trans.size() + " records to exports/transactions_export.csv");
                    break;
                case 4:
                    String path = readStringInput("Enter CSV file path (or press Enter for default 'src/main/resources/sample_books.csv'): ");
                    if (path.isEmpty()) path = "src/main/resources/sample_books.csv";

                    List<Book> imported = FileManager.importBooksFromCSV(path);
                    System.out.printf("Read %d valid book rows from CSV. Now inserting into database...\n", imported.size());
                    int insertedCount = 0;
                    for (Book b : imported) {
                        try {
                            bookService.addBook(b.getTitle(), b.getAuthorName(), b.getCategoryName(),
                                    b.getIsbn(), b.getPublisher(), b.getPublicationYear(), b.getPrice(), b.getTotalCopies());
                            insertedCount++;
                        } catch (DuplicateISBNException dup) {
                            System.out.println("  [Skipped Existing ISBN]: " + b.getIsbn() + " - " + b.getTitle());
                        }
                    }
                    System.out.printf(">>> SUCCESS: Imported and saved %d new books to database!\n", insertedCount);
                    break;
                case 5:
                    List<Book> backupList = bookService.getAllBooks();
                    FileManager.saveBinaryBackup((Serializable) backupList, "exports/books_backup.dat");
                    System.out.println(">>> SUCCESS: Binary backup saved to exports/books_backup.dat using FileOutputStream & ObjectOutputStream.");
                    break;
                case 6:
                    List<Book> restored = FileManager.loadBinaryBackup("exports/books_backup.dat");
                    System.out.printf(">>> SUCCESS: Read %d books from binary backup using FileInputStream & ObjectInputStream.\n", restored.size());
                    printBookTable(restored);
                    break;
                case 7:
                    String targetFile = readStringInput("Enter text file to analyze (or press Enter for 'src/main/resources/database.sql'): ");
                    if (targetFile.isEmpty()) targetFile = "src/main/resources/database.sql";
                    String analysis = FileManager.analyzeTextFileWithTokenizer(targetFile);
                    System.out.println("\n>>> " + analysis);
                    break;
                default:
                    System.out.println(">>> Invalid choice.");
            }
        } catch (IOException | ClassNotFoundException e) {
            System.out.println(">>> FILE OPERATION ERROR: " + e.getMessage());
        }
    }

    // =========================================================================
    // 12. MULTITHREADING & CONCURRENCY DEMONSTRATIONS
    // =========================================================================
    private static void handleMultithreadingDemo() {
        System.out.println("\n--- MULTITHREADING & SYNCHRONIZATION (Lectures 26-33) ---");
        System.out.println(" 1. Producer-Consumer Demo (wait(), notifyAll(), NotificationQueue)");
        System.out.println(" 2. Thread Lifecycle States Demo (NEW, RUNNABLE, WAITING, BLOCKED, etc.)");
        System.out.println(" 3. Concurrency & Race Condition Demo (Two threads issuing last copy)");
        System.out.println(" 4. Start/Stop Background Overdue Checker Daemon Thread");
        int choice = readIntInput("Enter choice (1-4): ");
        switch (choice) {
            case 1:
                runProducerConsumerDemo();
                break;
            case 2:
                ThreadLifecycleDemo.runDemo();
                break;
            case 3:
                ConcurrencyIssueDemo.runComparison();
                break;
            case 4:
                toggleBackgroundDaemon();
                break;
            default:
                System.out.println(">>> Invalid choice.");
        }
    }

    private static void runProducerConsumerDemo() {
        System.out.println("\n--- PRODUCER-CONSUMER DEMONSTRATION WITH WAIT() & NOTIFYALL() ---");
        System.out.println("Capacity: 3 items. Producer will wait() when full; Consumer will wait() when empty.");

        NotificationQueue queue = new NotificationQueue(3);
        String[] sampleAlerts = {
                "Alert 1: Book 'Effective Java' borrowed by Alice",
                "Alert 2: Book 'Clean Code' returned by Bob",
                "Alert 3: Overdue fine calculated for Issue #4",
                "Alert 4: New member 'Charlie' registered",
                "Alert 5: Book 'Refactoring' reserved"
        };

        Thread producerThread = new Thread(new NotificationProducer(queue, sampleAlerts, 200), "ProducerThread-1");
        Thread consumerThread = new Thread(new NotificationConsumer(queue, sampleAlerts.length, 350), "ConsumerThread-1");

        producerThread.start();
        consumerThread.start();

        try {
            producerThread.join();
            consumerThread.join();
        } catch (InterruptedException e) {
            System.out.println("Interrupted during demo.");
        }
        System.out.println(">>> Producer-Consumer demo finished cleanly!\n");
    }

    private static void toggleBackgroundDaemon() {
        if (backgroundDaemon == null || !backgroundDaemon.isAlive()) {
            backgroundDaemon = new LibraryBackgroundTask(new dao.IssueDAO(), 15);
            backgroundDaemon.start();
            System.out.println(">>> Background Overdue Checker Daemon STARTED (Checks every 15s in background).");
        } else {
            backgroundDaemon.stopTask();
            backgroundDaemon = null;
            System.out.println(">>> Background Overdue Checker Daemon STOPPED.");
        }
    }

    private static void shutdownApplication() {
        if (backgroundDaemon != null && backgroundDaemon.isAlive()) {
            backgroundDaemon.stopTask();
        }
        System.out.println("\n=========================================================================");
        System.out.println("       Thank you for using the Library Management System! Goodbye!       ");
        System.out.println("=========================================================================");
    }

    // =========================================================================
    // HELPER PRINTING & INPUT METHODS
    // =========================================================================
    private static void printBookTable(List<Book> books) {
        if (books.isEmpty()) {
            System.out.println(">>> No books found.");
            return;
        }
        System.out.println("\n" + "=".repeat(110));
        System.out.printf("%-5s | %-30s | %-20s | %-15s | %-6s | %-8s | %-8s\n",
                "ID", "Title", "Author", "ISBN", "Year", "Price", "Available");
        System.out.println("=".repeat(110));
        for (Book b : books) {
            System.out.printf("%-5d | %-30s | %-20s | %-15s | %-6d | $%-7.2f | %d / %d\n",
                    b.getBookId(),
                    truncate(b.getTitle(), 30),
                    truncate(b.getAuthorName(), 20),
                    truncate(b.getIsbn(), 15),
                    b.getPublicationYear(),
                    b.getPrice(),
                    b.getAvailableCopies(),
                    b.getTotalCopies());
        }
        System.out.println("=".repeat(110));
    }

    private static void printMemberTable(List<Member> members) {
        if (members.isEmpty()) {
            System.out.println(">>> No members found.");
            return;
        }
        System.out.println("\n" + "=".repeat(100));
        System.out.printf("%-5s | %-22s | %-25s | %-12s | %-10s | %-8s\n",
                "ID", "Name", "Email", "Phone", "Registered", "Status");
        System.out.println("=".repeat(100));
        for (Member m : members) {
            System.out.printf("%-5d | %-22s | %-25s | %-12s | %-10s | %-8s\n",
                    m.getMemberId(),
                    truncate(m.getName(), 22),
                    truncate(m.getEmail(), 25),
                    truncate(m.getPhone(), 12),
                    m.getRegistrationDate(),
                    m.isActive() ? "ACTIVE" : "INACTIVE");
        }
        System.out.println("=".repeat(100));
    }

    private static void printTransactionsTable(List<IssueRecord> list) {
        if (list.isEmpty()) {
            System.out.println(">>> No transaction records found.");
            return;
        }
        System.out.println("\n" + "=".repeat(110));
        System.out.printf("%-6s | %-18s | %-25s | %-10s | %-10s | %-10s | %-9s | %-5s\n",
                "Issue#", "Member", "Book Title", "Issued", "Due Date", "Returned", "Fine ($)", "Paid");
        System.out.println("=".repeat(110));
        for (IssueRecord r : list) {
            System.out.printf("%-6d | %-18s | %-25s | %-10s | %-10s | %-10s | $%-8.2f | %-5s\n",
                    r.getIssueId(),
                    truncate(r.getMemberName(), 18),
                    truncate(r.getBookTitle(), 25),
                    r.getIssueDate(),
                    r.getDueDate(),
                    r.getReturnDate() != null ? r.getReturnDate().toString() : "ACTIVE",
                    r.getFineAmount(),
                    r.isFinePaid() ? "YES" : "NO");
        }
        System.out.println("=".repeat(110));
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "N/A";
        return (s.length() > maxLen) ? s.substring(0, maxLen - 3) + "..." : s;
    }

    private static int readIntInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println(">>> Invalid integer. Please enter a valid whole number.");
            }
        }
    }

    private static double readDoubleInput(String prompt) {
        while (true) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            try {
                return Double.parseDouble(input);
            } catch (NumberFormatException e) {
                System.out.println(">>> Invalid number. Please enter a valid decimal number (e.g. 45.50).");
            }
        }
    }

    private static String readStringInput(String prompt) {
        System.out.print(prompt);
        return scanner.nextLine().trim();
    }
}
