package threads;

/**
 * Notification Producer implementing Runnable (Lectures 26-29 Multithreading).
 * Places system alerts and borrowing events into the shared NotificationQueue.
 */
public class NotificationProducer implements Runnable {

    private final NotificationQueue queue;
    private final String[] notifications;
    private final int delayMillis;

    public NotificationProducer(NotificationQueue queue, String[] notifications, int delayMillis) {
        this.queue = queue;
        this.notifications = notifications;
        this.delayMillis = delayMillis;
    }

    @Override
    public void run() {
        System.out.printf(">>> [%s] Producer thread started with priority %d\n",
                Thread.currentThread().getName(), Thread.currentThread().getPriority());
        try {
            for (String msg : notifications) {
                queue.put(msg);
                if (delayMillis > 0) {
                    Thread.sleep(delayMillis); // sleep() retains locks if inside synchronized block, pauses thread
                }
            }
        } catch (InterruptedException e) {
            System.out.printf("[%s] Producer was interrupted: %s\n", Thread.currentThread().getName(), e.getMessage());
            Thread.currentThread().interrupt(); // Restore interrupted status
        }
        System.out.printf("<<< [%s] Producer thread finished adding all items.\n", Thread.currentThread().getName());
    }
}
