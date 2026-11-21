package threads;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Concurrency & Race Condition Demonstration (Lectures 30-33 Synchronization).
 * Demonstrates the classic problem:
 * Two users attempt to issue the very last remaining copy of a book at the exact same millisecond.
 *
 * Compares:
 * 1. Unsynchronized Execution (Causes Race Condition -> Negative Inventory / Duplicate Issue)
 * 2. Synchronized Execution (Critical Section protected by Intrinsic Lock -> Prevents Race Condition)
 */
public class ConcurrencyIssueDemo {

    /**
     * Unsynchronized Book Counter demonstrating a Race Condition.
     */
    static class UnsafeBookStock {
        private int availableCopies = 1;

        public boolean issueCopy(String userName) {
            // Check condition
            if (availableCopies > 0) {
                // Simulate context switch / latency between check and update
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {}

                availableCopies--;
                System.out.printf("  [UNSAFE] %s successfully issued book! Remaining copies: %d\n", userName, availableCopies);
                return true;
            } else {
                System.out.printf("  [UNSAFE] %s failed to issue book. Out of stock! Remaining copies: %d\n", userName, availableCopies);
                return false;
            }
        }

        public int getAvailableCopies() {
            return availableCopies;
        }
    }

    /**
     * Safe Book Counter using synchronized critical section with intrinsic lock.
     */
    static class SafeBookStock {
        private int availableCopies = 1;
        private final Object lock = new Object();

        public boolean issueCopy(String userName) {
            // Synchronized block guards the critical section
            synchronized (lock) {
                if (availableCopies > 0) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {}

                    availableCopies--;
                    System.out.printf("  [SAFE] %s successfully issued book! Remaining copies: %d\n", userName, availableCopies);
                    return true;
                } else {
                    System.out.printf("  [SAFE] %s failed to issue book. Out of stock! Remaining copies: %d\n", userName, availableCopies);
                    return false;
                }
            }
        }

        public int getAvailableCopies() {
            synchronized (lock) {
                return availableCopies;
            }
        }
    }

    public static void runComparison() {
        System.out.println("\n=======================================================");
        System.out.println("  CONCURRENCY SIMULATION: ISSUING LAST AVAILABLE COPY  ");
        System.out.println("=======================================================");

        // Part 1: Without Synchronization (Race Condition)
        System.out.println("1. Running UNSYNCHRONIZED Issue Simulation (Starting Stock: 1 copy):");
        UnsafeBookStock unsafeStock = new UnsafeBookStock();

        Thread t1 = new Thread(() -> unsafeStock.issueCopy("Alice"), "Student-Alice");
        Thread t2 = new Thread(() -> unsafeStock.issueCopy("Bob"), "Student-Bob");

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException ignored) {}

        System.out.printf("Result: Unsafe Final Stock = %d (Notice: May drop to 0 or even -1 due to race condition!)\n\n",
                unsafeStock.getAvailableCopies());

        // Part 2: With Synchronization (Protected Critical Section)
        System.out.println("2. Running SYNCHRONIZED Protected Simulation (Starting Stock: 1 copy):");
        SafeBookStock safeStock = new SafeBookStock();

        Thread t3 = new Thread(() -> safeStock.issueCopy("Alice"), "Student-Alice");
        Thread t4 = new Thread(() -> safeStock.issueCopy("Bob"), "Student-Bob");

        t3.start();
        t4.start();

        try {
            t3.join();
            t4.join();
        } catch (InterruptedException ignored) {}

        System.out.printf("Result: Safe Final Stock = %d (Guaranteed atomic check-and-decrement. Race condition prevented!)\n",
                safeStock.getAvailableCopies());
        System.out.println("=======================================================\n");
    }
}
