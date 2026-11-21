import exceptions.BookUnavailableException;
import exceptions.InvalidInputException;
import model.Book;
import model.Member;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.BookService;
import service.LibraryService;
import service.MemberService;
import threads.NotificationConsumer;
import threads.NotificationProducer;
import threads.NotificationQueue;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class ConcurrencyTest {

    private static LibraryService libraryService;
    private static BookService bookService;
    private static MemberService memberService;

    @BeforeAll
    public static void setUp() {
        libraryService = new LibraryService();
        bookService = new BookService();
        memberService = new MemberService();
    }

    @Test
    public void testConcurrentIssueLastCopyIntegrity() throws Exception {
        // Create 1 book with EXACTLY 1 available copy
        Book book = bookService.addBook(
                "Concurrency in Practice",
                "Brian Goetz",
                "Computer Science",
                "978-0" + (System.nanoTime() % 10000000000L),
                "Addison-Wesley",
                2006,
                55.0,
                1 // Only 1 copy!
        );

        // Create 2 distinct members
        Member m1 = memberService.registerMember("Racer One", "racer1." + System.currentTimeMillis() + "@uni.edu", "9876543301", "Lab A");
        Member m2 = memberService.registerMember("Racer Two", "racer2." + System.currentTimeMillis() + "@uni.edu", "9876543302", "Lab B");

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        Thread t1 = new Thread(() -> {
            try {
                libraryService.issueBook(m1.getMemberId(), book.getBookId());
                successCount.incrementAndGet();
            } catch (BookUnavailableException | InvalidInputException e) {
                failCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        Thread t2 = new Thread(() -> {
            try {
                libraryService.issueBook(m2.getMemberId(), book.getBookId());
                successCount.incrementAndGet();
            } catch (BookUnavailableException | InvalidInputException e) {
                failCount.incrementAndGet();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Start both threads concurrently
        t1.start();
        t2.start();

        t1.join();
        t2.join();

        // Exactly one thread must have succeeded, and exactly one must have failed
        assertEquals(1, successCount.get(), "Exactly one user should successfully issue the last copy.");
        assertEquals(1, failCount.get(), "The second concurrent user must be prevented from issuing the last copy.");

        // Inventory must remain exactly 0 (not -1!)
        Book finalBook = bookService.getBookById(book.getBookId());
        assertEquals(0, finalBook.getAvailableCopies(), "Available copies should be 0 and never negative.");
    }

    @Test
    public void testNotificationQueueProducerConsumer() throws InterruptedException {
        NotificationQueue queue = new NotificationQueue(2); // Small capacity to force wait() condition
        String[] messages = {"Msg 1", "Msg 2", "Msg 3", "Msg 4"};

        Thread producer = new Thread(new NotificationProducer(queue, messages, 20), "TestProducer");
        Thread consumer = new Thread(new NotificationConsumer(queue, messages.length, 30), "TestConsumer");

        producer.start();
        consumer.start();

        producer.join(3000);
        consumer.join(3000);

        assertTrue(queue.isEmpty(), "All messages should be consumed from queue.");
    }
}
