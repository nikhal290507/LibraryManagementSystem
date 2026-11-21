package service;

import dao.BookDAO;
import dao.IssueDAO;
import dao.MemberDAO;
import database.DBConnection;
import exceptions.BookNotFoundException;
import exceptions.BookUnavailableException;
import exceptions.DatabaseException;
import exceptions.InvalidInputException;
import exceptions.MemberNotFoundException;
import model.Book;
import model.IssueRecord;
import model.Member;
import utils.FileManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.*;

/**
 * Service class for Library Transactions, Fines, and Dashboard Analytics.
 * Demonstrates:
 * - Strict Database Transactions (commit, rollback, setAutoCommit(false))
 * - Concurrent stock integrity via atomic conditional update
 * - PriorityQueue (processing overdue loans ordered by overdue days)
 * - Deque (maintaining recent activity log)
 * - Java Date/Time API (LocalDate, ChronoUnit)
 */
public class LibraryService {

    private final BookDAO bookDAO;
    private final MemberDAO memberDAO;
    private final IssueDAO issueDAO;

    // Collections demonstrations (Lectures 16-25)
    // Deque for recent action history (double-ended queue)
    private final Deque<String> recentActions = new ArrayDeque<>(20);

    // Configurable business constants
    private final int loanPeriodDays = 14;
    private final double finePerDay = 5.00;

    public LibraryService() {
        this.bookDAO = new BookDAO();
        this.memberDAO = new MemberDAO();
        this.issueDAO = new IssueDAO();
    }

    public LibraryService(BookDAO bookDAO, MemberDAO memberDAO, IssueDAO issueDAO) {
        this.bookDAO = bookDAO;
        this.memberDAO = memberDAO;
        this.issueDAO = issueDAO;
    }

    /**
     * Issues a book to a member using a strict Database Transaction (commit/rollback).
     * Enforces:
     * - Member exists and is active
     * - Member has not reached max borrowing limit
     * - Member does not already have an active issue for this exact book
     * - Book exists and has available copies
     * - Atomic decrement of inventory to prevent race conditions
     */
    public IssueRecord issueBook(int memberId, int bookId) throws DatabaseException {
        // 1. Verify Member
        Member member = memberDAO.getMemberById(memberId);
        if (member == null) {
            throw new MemberNotFoundException("Member #" + memberId + " not found.");
        }
        if (!member.isActive()) {
            throw new InvalidInputException("Member #" + memberId + " (" + member.getName() + ") is INACTIVE and cannot borrow books.");
        }

        // 2. Check borrowing limit
        int activeLoans = memberDAO.getActiveIssuedCount(memberId);
        if (activeLoans >= member.getMaxBooksAllowed()) {
            throw new InvalidInputException("Borrowing limit exceeded: Member #" + memberId
                    + " already has " + activeLoans + " active book(s) (Maximum allowed: " + member.getMaxBooksAllowed() + ").");
        }

        // 3. Prevent duplicate active loan for same book
        IssueRecord activeSameBook = issueDAO.getActiveIssue(memberId, bookId);
        if (activeSameBook != null) {
            throw new InvalidInputException("Member #" + memberId + " already has an active loan for this book (Issue #" + activeSameBook.getIssueId() + ").");
        }

        // 4. Verify Book
        Book book = bookDAO.getBookById(bookId);
        if (book == null) {
            throw new BookNotFoundException("Book #" + bookId + " not found.");
        }
        if (book.getAvailableCopies() <= 0) {
            throw new BookUnavailableException("Book \"" + book.getTitle() + "\" [ID #" + bookId + "] is currently OUT OF STOCK (0 available copies).");
        }

        // 5. Database Transaction Execution
        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(loanPeriodDays);
        IssueRecord record = new IssueRecord(memberId, bookId, issueDate, dueDate);
        record.setMemberName(member.getName());
        record.setBookTitle(book.getTitle());

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin Database Transaction (Lectures 34-42)

            // Atomic conditional update on inventory
            boolean decremented = bookDAO.atomicDecrementAvailableCopies(bookId, conn);
            if (!decremented) {
                conn.rollback();
                throw new BookUnavailableException("Concurrent conflict: The last copy of \"" + book.getTitle() + "\" was just issued by another transaction!");
            }

            // Insert issue record
            int issueId = issueDAO.createIssue(record, conn);
            record.setIssueId(issueId);

            // Commit transaction
            conn.commit();
            conn.setAutoCommit(true);

            // Record action in Deque & Activity File
            String actionMsg = String.format("ISSUED: Book #%d (\"%s\") to Member #%d (%s) - Due: %s",
                    bookId, book.getTitle(), memberId, member.getName(), dueDate);
            recordAction(actionMsg);

            return record;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback transaction on failure
                } catch (SQLException rollbackEx) {
                    System.err.println("Transaction rollback failed: " + rollbackEx.getMessage());
                }
            }
            throw new DatabaseException("Database transaction failed during book issue: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Returns an issued book using a strict Database Transaction.
     * Calculates overdue days and late fine using LocalDate and ChronoUnit.
     */
    public IssueRecord returnBook(int issueId, boolean payFineNow) throws DatabaseException {
        IssueRecord record = issueDAO.getIssueById(issueId);
        if (record == null) {
            throw new InvalidInputException("Issue record #" + issueId + " does not exist.");
        }
        if ("RETURNED".equalsIgnoreCase(record.getStatus())) {
            throw new InvalidInputException("Book has already been returned on " + record.getReturnDate() + " for Issue #" + issueId);
        }

        LocalDate returnDate = LocalDate.now();
        double fine = record.calculateFine(finePerDay, returnDate);
        boolean finePaid = (fine == 0.0) || payFineNow;

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            // Atomically restore available copies
            bookDAO.atomicIncrementAvailableCopies(record.getBookId(), conn);

            // Update issue record
            issueDAO.returnBook(issueId, returnDate, fine, finePaid, conn);

            conn.commit(); // Commit Transaction
            conn.setAutoCommit(true);

            record.setReturnDate(returnDate);
            record.setFineAmount(fine);
            record.setStatus("RETURNED");
            record.setFinePaid(finePaid);

            String actionMsg = String.format("RETURNED: Issue #%d (Book #%d) | Days Overdue: %d | Fine: $%.2f (Paid: %s)",
                    issueId, record.getBookId(), record.calculateOverdueDays(returnDate), fine, finePaid ? "YES" : "NO");
            recordAction(actionMsg);

            return record;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    System.err.println("Return rollback failed: " + rollbackEx.getMessage());
                }
            }
            throw new DatabaseException("Database transaction failed during book return: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * Pays pending fine for a transaction.
     */
    public boolean payFine(int issueId) throws DatabaseException {
        IssueRecord record = issueDAO.getIssueById(issueId);
        if (record == null) {
            throw new InvalidInputException("Issue record #" + issueId + " not found.");
        }
        if (record.isFinePaid()) {
            throw new InvalidInputException("Fine for Issue #" + issueId + " has already been paid!");
        }
        if (record.getFineAmount() <= 0.0) {
            throw new InvalidInputException("Issue #" + issueId + " has no accrued fine to pay.");
        }
        boolean paid = issueDAO.payFine(issueId);
        if (paid) {
            recordAction("FINE PAID: Issue #" + issueId + " ($" + record.getFineAmount() + ")");
        }
        return paid;
    }

    /**
     * Demonstrates PriorityQueue (Lectures 16-25 Collections).
     * Retrieves all overdue loans and processes them in order of highest overdue days.
     */
    public PriorityQueue<IssueRecord> getOverdueLoansPriorityQueue() throws DatabaseException {
        LocalDate today = LocalDate.now();
        List<IssueRecord> overdueList = issueDAO.getOverdueIssues(today);

        // PriorityQueue orders by longest overdue days descending
        PriorityQueue<IssueRecord> pq = new PriorityQueue<>(
                (r1, r2) -> Long.compare(r2.calculateOverdueDays(today), r1.calculateOverdueDays(today))
        );
        pq.addAll(overdueList);
        return pq;
    }

    public List<IssueRecord> getAllTransactions() throws DatabaseException {
        return issueDAO.getAllIssues();
    }

    public List<IssueRecord> getActiveTransactions() throws DatabaseException {
        return issueDAO.getActiveIssues();
    }

    public List<IssueRecord> getMemberTransactions(int memberId) throws DatabaseException {
        memberDAO.getMemberById(memberId);
        return issueDAO.getIssuesByMember(memberId);
    }

    /**
     * Fetches real-time dashboard analytics from the database.
     */
    public Map<String, Object> getDashboardStats() throws DatabaseException {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("Total Book Titles", bookDAO.getTotalDistinctBookCount());
        stats.put("Total Copies", bookDAO.getTotalCopiesCount());
        stats.put("Available Copies", bookDAO.getTotalAvailableCopiesCount());
        stats.put("Issued Copies", issueDAO.getTotalActiveIssuedCount());
        stats.put("Registered Members", memberDAO.getTotalMemberCount());
        stats.put("Overdue Loans", issueDAO.getOverdueIssues(LocalDate.now()).size());
        stats.put("Collected Fines ($)", String.format("%.2f", issueDAO.getTotalCollectedFines()));
        stats.put("Pending Fines ($)", String.format("%.2f", issueDAO.getTotalPendingFines()));
        stats.put("Database Engine", DBConnection.getDatabaseType());
        return stats;
    }

    public Deque<String> getRecentActions() {
        synchronized (recentActions) {
            return new ArrayDeque<>(recentActions);
        }
    }

    private void recordAction(String action) {
        synchronized (recentActions) {
            recentActions.addFirst(action);
            if (recentActions.size() > 20) {
                recentActions.removeLast();
            }
        }
        FileManager.appendActivityLog(action, "exports/activity_log.txt");
    }

    public double getFinePerDay() {
        return finePerDay;
    }

    public int getLoanPeriodDays() {
        return loanPeriodDays;
    }
}
