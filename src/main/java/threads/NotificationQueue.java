package threads;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Shared Bounded Notification Queue (Lectures 30-33 Synchronization & Inter-Thread Communication).
 * Implements the classic Producer-Consumer pattern using:
 * - Intrinsic lock (synchronized methods)
 * - Guarded blocks with wait(), notify(), and notifyAll()
 * - while loop testing condition to prevent spurious wakeups
 *
 * Core Concept Highlight:
 * - wait(): Releases the intrinsic monitor lock and enters the WAITING state until notified.
 * - sleep(): Pauses execution for a duration but DOES NOT release the monitor lock.
 */
public class NotificationQueue {

    private final Queue<String> queue = new LinkedList<>();
    private final int capacity;
    private boolean active = true;

    public NotificationQueue(int capacity) {
        this.capacity = capacity > 0 ? capacity : 5;
    }

    /**
     * Producer method: Adds a notification to the queue.
     * If queue is full, calling thread WAITS until consumer removes an item.
     */
    public synchronized void put(String notification) throws InterruptedException {
        while (queue.size() >= capacity && active) {
            System.out.printf("[%s] Queue is FULL (%d/%d). Producer is WAITING (wait() called)...\n",
                    Thread.currentThread().getName(), queue.size(), capacity);
            wait(); // Releases lock and waits for consumer notification
        }

        if (!active) return;

        queue.add(notification);
        System.out.printf("[%s] PRODUCED: \"%s\" (Queue size: %d/%d)\n",
                Thread.currentThread().getName(), notification, queue.size(), capacity);

        // Notify consumer threads that new item is available
        notifyAll();
    }

    /**
     * Consumer method: Retrieves and removes a notification.
     * If queue is empty, calling thread WAITS until producer adds an item.
     */
    public synchronized String take() throws InterruptedException {
        while (queue.isEmpty() && active) {
            System.out.printf("[%s] Queue is EMPTY. Consumer is WAITING (wait() called)...\n",
                    Thread.currentThread().getName());
            wait(); // Releases lock and waits for producer notification
        }

        if (!active && queue.isEmpty()) return null;

        String item = queue.poll();
        System.out.printf("[%s] CONSUMED: \"%s\" (Queue size: %d/%d)\n",
                Thread.currentThread().getName(), item, queue.size(), capacity);

        // Notify producer threads that space is now available
        notifyAll();
        return item;
    }

    public synchronized void stop() {
        this.active = false;
        notifyAll();
    }

    public synchronized int size() {
        return queue.size();
    }

    public synchronized boolean isEmpty() {
        return queue.isEmpty();
    }
}
