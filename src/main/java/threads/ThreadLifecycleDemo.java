package threads;

/**
 * Thread Lifecycle Demonstration (Lectures 26-29 Multithreading).
 * Explicitly traces and displays all six Java Thread States:
 * 1. NEW
 * 2. RUNNABLE
 * 3. TIMED_WAITING
 * 4. WAITING
 * 5. BLOCKED
 * 6. TERMINATED
 *
 * Also demonstrates:
 * - Thread.start() vs Thread.run()
 * - Thread priorities (MIN_PRIORITY, NORM_PRIORITY, MAX_PRIORITY)
 * - Thread.join()
 */
public class ThreadLifecycleDemo {

    public static void runDemo() {
        System.out.println("\n=======================================================");
        System.out.println("     JAVA THREAD LIFECYCLE & CONCURRENCY DEMO         ");
        System.out.println("=======================================================");

        final Object sharedLock = new Object();

        // 1. NEW State
        Thread worker = new Thread(() -> {
            try {
                System.out.printf("[Worker] Running on thread '%s'. Entering TIMED_WAITING for 400ms...\n",
                        Thread.currentThread().getName());
                // 3. TIMED_WAITING State
                Thread.sleep(400);

                // 4. WAITING State
                synchronized (sharedLock) {
                    System.out.printf("[Worker] Acquired lock. Now calling wait() -> entering WAITING state...\n");
                    sharedLock.wait();
                    System.out.printf("[Worker] Woken up from wait()! Completing execution...\n");
                }
            } catch (InterruptedException e) {
                System.out.println("[Worker] Interrupted.");
            }
        }, "Lifecycle-Worker-1");

        System.out.printf("[Main] 1. After creation: Worker state is -> %s\n", worker.getState());

        // Demonstrating priority
        worker.setPriority(Thread.NORM_PRIORITY + 2);
        System.out.printf("[Main] Worker Priority set to %d (Max: %d, Min: %d)\n",
                worker.getPriority(), Thread.MAX_PRIORITY, Thread.MIN_PRIORITY);

        // 2. RUNNABLE State
        worker.start();
        System.out.printf("[Main] 2. After start(): Worker state is -> %s\n", worker.getState());

        try {
            // Give worker time to enter sleep(400)
            Thread.sleep(150);
            System.out.printf("[Main] 3. While sleeping: Worker state is -> %s\n", worker.getState());

            // Wait for worker to finish sleep and enter wait()
            Thread.sleep(350);
            System.out.printf("[Main] 4. While inside wait(): Worker state is -> %s\n", worker.getState());

            // 5. Demonstrating BLOCKED State
            // We lock the shared object, then wake the worker. The worker will transition to BLOCKED
            // while waiting for main to release sharedLock!
            synchronized (sharedLock) {
                System.out.println("[Main] Main thread acquired lock. Calling notifyAll()...");
                sharedLock.notifyAll(); // Worker moves from WAITING to BLOCKED waiting for lock release

                // Give short pause so worker attempts lock acquisition
                Thread.sleep(50);
                System.out.printf("[Main] 5. While worker awaits lock release: Worker state is -> %s\n", worker.getState());
            }

            // 6. TERMINATED State using join()
            System.out.println("[Main] Main thread released lock. Calling worker.join() to wait for termination...");
            worker.join(); // Main thread waits until worker terminates
            System.out.printf("[Main] 6. After completion: Worker state is -> %s\n", worker.getState());

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Demonstration of start() vs run()
        System.out.println("\n--- START() vs RUN() DEMONSTRATION ---");
        Runnable sampleRunnable = () ->
                System.out.printf("  Execution in thread: %s (Is Main Thread: %s)\n",
                        Thread.currentThread().getName(),
                        Thread.currentThread().getName().equalsIgnoreCase("main"));

        System.out.println("Calling runnable.run() directly:");
        sampleRunnable.run(); // Executes synchronously on main thread!

        System.out.println("Calling new Thread(runnable).start():");
        Thread t = new Thread(sampleRunnable, "DemoThread-New");
        t.start(); // Spawns a brand new OS/JVM thread!
        try {
            t.join();
        } catch (InterruptedException ignored) {}

        System.out.println("=======================================================\n");
    }
}
