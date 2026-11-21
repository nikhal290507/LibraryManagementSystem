package threads;

/**
 * Notification Consumer implementing Runnable (Lectures 26-29 Multithreading).
 * Retrieves and processes alerts from the shared NotificationQueue.
 */
public class NotificationConsumer implements Runnable {

    private final NotificationQueue queue;
    private final int itemsToConsume;
    private final int delayMillis;

    public NotificationConsumer(NotificationQueue queue, int itemsToConsume, int delayMillis) {
        this.queue = queue;
        this.itemsToConsume = itemsToConsume;
        this.delayMillis = delayMillis;
    }

    @Override
    public void run() {
        System.out.printf(">>> [%s] Consumer thread started.\n", Thread.currentThread().getName());
        int consumed = 0;
        try {
            while (consumed < itemsToConsume) {
                String notification = queue.take();
                if (notification == null) break;
                consumed++;
                // Process notification (e.g. log / display)
                if (delayMillis > 0) {
                    Thread.sleep(delayMillis);
                }
            }
        } catch (InterruptedException e) {
            System.out.printf("[%s] Consumer was interrupted: %s\n", Thread.currentThread().getName(), e.getMessage());
            Thread.currentThread().interrupt();
        }
        System.out.printf("<<< [%s] Consumer thread completed. Processed %d notifications.\n",
                Thread.currentThread().getName(), consumed);
    }
}
