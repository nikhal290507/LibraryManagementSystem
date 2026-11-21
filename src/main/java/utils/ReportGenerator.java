package utils;

import model.Book;
import model.IssueRecord;
import model.Member;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Report Generator demonstrating Lambda Expressions and Stream API (Lectures 43-46).
 * Features:
 * - filter(), map(), sorted(), distinct(), limit(), skip(), forEach(), collect()
 * - Method references (e.g. Book::getTitle, IssueRecord::getFineAmount)
 * - Functional interfaces (Predicate, Function, Consumer, Comparator)
 * - Generates clean textual and file reports
 */
public class ReportGenerator {

    /**
     * Generates a comprehensive summary report string using Stream API.
     */
    public static String generateComprehensiveReport(List<Book> books, List<Member> members,
                                                    List<IssueRecord> issues, double collectedFines,
                                                    double pendingFines) {
        StringBuilder sb = new StringBuilder();
        String divider = "=".repeat(75);
        String subDivider = "-".repeat(75);

        sb.append(divider).append("\n");
        sb.append(String.format("%45s\n", "LIBRARY ANALYTICS & AUDIT REPORT"));
        sb.append(String.format("%42s\n", "Generated: " + LocalDate.now()));
        sb.append(divider).append("\n\n");

        // 1. Overall Statistics using Streams
        int totalBooks = books.size();
        int totalCopies = books.stream().mapToInt(Book::getTotalCopies).sum();
        int availableCopies = books.stream().mapToInt(Book::getAvailableCopies).sum();
        long activeMembers = members.stream().filter(Member::isActive).count();
        long inactiveMembers = members.stream().filter(m -> !m.isActive()).count();

        // Overdue count using Stream filter
        LocalDate today = LocalDate.now();
        List<IssueRecord> overdueList = issues.stream()
                .filter(i -> "ISSUED".equalsIgnoreCase(i.getStatus()) || "OVERDUE".equalsIgnoreCase(i.getStatus()))
                .filter(i -> i.getDueDate().isBefore(today))
                .collect(Collectors.toList());

        sb.append("[1] EXECUTIVE SUMMARY\n");
        sb.append(subDivider).append("\n");
        sb.append(String.format("Total Distinct Book Titles : %d\n", totalBooks));
        sb.append(String.format("Total Physical Copies      : %d\n", totalCopies));
        sb.append(String.format("Available Copies on Shelf  : %d\n", availableCopies));
        sb.append(String.format("Currently Issued Copies    : %d\n", (totalCopies - availableCopies)));
        sb.append(String.format("Active Library Members     : %d\n", activeMembers));
        sb.append(String.format("Inactive Members           : %d\n", inactiveMembers));
        sb.append(String.format("Overdue Transactions Count : %d\n", overdueList.size()));
        sb.append(String.format("Total Fines Collected      : $%.2f\n", collectedFines));
        sb.append(String.format("Total Fines Pending        : $%.2f\n\n", pendingFines));

        // 2. Distinct Categories and Book Counts (Demonstrates groupingBy and distinct)
        sb.append("[2] BOOKS BY CATEGORY (Stream grouping & distinct)\n");
        sb.append(subDivider).append("\n");
        Map<String, Long> categoryCount = books.stream()
                .collect(Collectors.groupingBy(
                        b -> (b.getCategoryName() != null ? b.getCategoryName() : "Uncategorized"),
                        Collectors.counting()
                ));
        categoryCount.forEach((cat, count) ->
                sb.append(String.format(" - %-25s : %d title(s)\n", cat, count))
        );
        sb.append("\n");

        // 3. Top 5 Most Issued / Borrowed Books (Stream frequency, mapping, sorting, limit)
        sb.append("[3] TOP 5 MOST ISSUED BOOKS (Stream limit & sorting)\n");
        sb.append(subDivider).append("\n");
        Map<Integer, Long> issueCountByBook = issues.stream()
                .collect(Collectors.groupingBy(IssueRecord::getBookId, Collectors.counting()));

        List<Map.Entry<Integer, Long>> topBookEntries = issueCountByBook.entrySet().stream()
                .sorted(Map.Entry.<Integer, Long>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        if (topBookEntries.isEmpty()) {
            sb.append(" No books have been issued yet.\n");
        } else {
            int rank = 1;
            for (Map.Entry<Integer, Long> entry : topBookEntries) {
                int bookId = entry.getKey();
                long count = entry.getValue();
                String title = books.stream()
                        .filter(b -> b.getBookId() == bookId)
                        .map(Book::getTitle)
                        .findFirst()
                        .orElse("Unknown Title (ID " + bookId + ")");
                sb.append(String.format(" %d. %-35s [ID: %d] - Borrowed %d time(s)\n", rank++, title, bookId, count));
            }
        }
        sb.append("\n");

        // 4. Overdue Books List with Fines
        sb.append("[4] CURRENTLY OVERDUE LOANS (Stream filter & mapping)\n");
        sb.append(subDivider).append("\n");
        if (overdueList.isEmpty()) {
            sb.append(" Excellent! There are currently no overdue books.\n");
        } else {
            sb.append(String.format("%-8s | %-20s | %-20s | %-12s | %-10s\n",
                    "IssueID", "Member", "Book Title", "Due Date", "Days Late"));
            sb.append(subDivider).append("\n");
            overdueList.forEach(ov -> {
                long daysLate = ov.calculateOverdueDays(today);
                sb.append(String.format("%-8d | %-20s | %-20s | %-12s | %-10d\n",
                        ov.getIssueId(),
                        truncate(ov.getMemberName(), 20),
                        truncate(ov.getBookTitle(), 20),
                        ov.getDueDate(),
                        daysLate));
            });
        }
        sb.append("\n");

        // 5. Books with Low Stock (<= 1 available copy)
        sb.append("[5] LOW STOCK ALERTS (Available Copies <= 1)\n");
        sb.append(subDivider).append("\n");
        List<Book> lowStock = books.stream()
                .filter(b -> b.getAvailableCopies() <= 1)
                .sorted(Comparator.comparingInt(Book::getAvailableCopies))
                .collect(Collectors.toList());

        if (lowStock.isEmpty()) {
            sb.append(" All books have healthy inventory (> 1 copy available).\n");
        } else {
            lowStock.forEach(b ->
                    sb.append(String.format(" - [ID %d] %-30s | Available: %d / %d total copies\n",
                            b.getBookId(), truncate(b.getTitle(), 30), b.getAvailableCopies(), b.getTotalCopies()))
            );
        }
        sb.append("\n").append(divider).append("\n");

        return sb.toString();
    }

    /**
     * Saves the generated report to disk in the exports directory.
     */
    public static File saveReportToFile(String reportContent, String filename) throws IOException {
        File dir = new File("exports");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        File reportFile = new File(dir, filename);
        try (PrintWriter pw = new PrintWriter(new FileWriter(reportFile))) {
            pw.print(reportContent);
        }
        return reportFile;
    }

    /**
     * Demonstrates custom Predicate and generic stream filtering (Lectures 43-46).
     */
    public static List<Book> filterBooks(List<Book> books, Predicate<Book> condition) {
        return books.stream()
                .filter(condition)
                .collect(Collectors.toList());
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return "N/A";
        return (s.length() > maxLen) ? s.substring(0, maxLen - 3) + "..." : s;
    }
}
