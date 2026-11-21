package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Model class representing a Book Issue / Loan Transaction.
 */
public class IssueRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    private int issueId;
    private int memberId;
    private String memberName;
    private int bookId;
    private String bookTitle;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private double fineAmount;
    private String status; // 'ISSUED', 'RETURNED', 'OVERDUE'
    private boolean finePaid;

    public IssueRecord() {
        this.issueDate = LocalDate.now();
        this.dueDate = issueDate.plusDays(14);
        this.status = "ISSUED";
        this.finePaid = false;
        this.fineAmount = 0.0;
    }

    public IssueRecord(int issueId, int memberId, String memberName, int bookId,
                       String bookTitle, LocalDate issueDate, LocalDate dueDate,
                       LocalDate returnDate, double fineAmount, String status,
                       boolean finePaid) {
        this.issueId = issueId;
        this.memberId = memberId;
        this.memberName = memberName;
        this.bookId = bookId;
        this.bookTitle = bookTitle;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fineAmount = fineAmount;
        this.status = status;
        this.finePaid = finePaid;
    }

    public IssueRecord(int memberId, int bookId, LocalDate issueDate, LocalDate dueDate) {
        this.memberId = memberId;
        this.bookId = bookId;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.status = "ISSUED";
        this.finePaid = false;
        this.fineAmount = 0.0;
    }

    /**
     * Calculates overdue days using LocalDate and ChronoUnit (Lectures 10-15 & Date API).
     */
    public long calculateOverdueDays(LocalDate currentDate) {
        if (currentDate == null) currentDate = LocalDate.now();
        LocalDate effectiveDate = (returnDate != null) ? returnDate : currentDate;
        if (effectiveDate.isAfter(dueDate)) {
            return ChronoUnit.DAYS.between(dueDate, effectiveDate);
        }
        return 0;
    }

    /**
     * Calculates fine based on daily rate.
     */
    public double calculateFine(double ratePerDay, LocalDate currentDate) {
        long overdueDays = calculateOverdueDays(currentDate);
        return overdueDays * ratePerDay;
    }

    // Getters and Setters
    public int getIssueId() {
        return issueId;
    }

    public void setIssueId(int issueId) {
        this.issueId = issueId;
    }

    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnDate() {
        return returnDate;
    }

    public void setReturnDate(LocalDate returnDate) {
        this.returnDate = returnDate;
    }

    public double getFineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isFinePaid() {
        return finePaid;
    }

    public void setFinePaid(boolean finePaid) {
        this.finePaid = finePaid;
    }

    @Override
    public String toString() {
        return String.format("[%d] Member ID: %d (%s) | Book ID: %d (%s) | Issued: %s | Due: %s | Status: %s | Fine: $%.2f (Paid: %s)",
                issueId, memberId, memberName != null ? memberName : "N/A",
                bookId, bookTitle != null ? bookTitle : "N/A",
                issueDate, dueDate, status, fineAmount, finePaid ? "YES" : "NO");
    }
}
