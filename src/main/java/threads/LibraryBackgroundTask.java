package threads;

import dao.IssueDAO;
import exceptions.DatabaseException;
import model.IssueRecord;

import java.time.LocalDate;
import java.util.List;

/**
 * Background Overdue Checker extending Thread (Lectures 26-29 Multithreading).
 * Demonstrates:
 * - Extending the Thread class
 * - Daemon thread capability (setDaemon(true))
 * - Thread lifecycle (RUNNABLE -> TIMED_WAITING via sleep -> TERMINATED)
 * - Handling interruption via interrupt()
 */
public class LibraryBackgroundTask extends Thread {

    private final IssueDAO issueDAO;
    private final int intervalSeconds;
    private volatile boolean running = true;

    public LibraryBackgroundTask(IssueDAO issueDAO, int intervalSeconds) {
        super("OverdueCheckerDaemon");
        this.issueDAO = issueDAO;
        this.intervalSeconds = intervalSeconds > 0 ? intervalSeconds : 10;
        setDaemon(true); // Daemon thread exits automatically when user terminates application
    }

    @Override
    public void run() {
        System.out.printf(">>> [%s] Daemon thread started (IsDaemon: %s, Priority: %d)\n",
                getName(), isDaemon(), getPriority());
        while (running && !isInterrupted()) {
            try {
                checkOverdueLoans();
                // Enters TIMED_WAITING state
                Thread.sleep(intervalSeconds * 1000L);
            } catch (InterruptedException e) {
                System.out.printf("[%s] Background task received interrupt signal. Shutting down...\n", getName());
                running = false;
                break;
            } catch (Exception e) {
                System.err.printf("[%s] Error during periodic check: %s\n", getName(), e.getMessage());
            }
        }
        System.out.printf("<<< [%s] Background thread terminated cleanly.\n", getName());
    }

    private void checkOverdueLoans() {
        try {
            LocalDate today = LocalDate.now();
            List<IssueRecord> overdueList = issueDAO.getOverdueIssues(today);
            if (!overdueList.isEmpty()) {
                System.out.printf("\n[Background Alert] Found %d overdue loan(s) as of %s:\n", overdueList.size(), today);
                for (IssueRecord rec : overdueList) {
                    long days = rec.calculateOverdueDays(today);
                    double fine = days * 5.0; // $5/day
                    System.out.printf("  * Issue #%d | Member: %s | Book: %s | %d days late | Accrued Fine: $%.2f\n",
                            rec.getIssueId(), rec.getMemberName(), rec.getBookTitle(), days, fine);
                }
            }
        } catch (DatabaseException e) {
            // Silently log or report in background
        }
    }

    public void stopTask() {
        this.running = false;
        this.interrupt();
    }
}
