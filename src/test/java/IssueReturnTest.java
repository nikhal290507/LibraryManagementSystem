import exceptions.BookUnavailableException;
import exceptions.InvalidInputException;
import model.Book;
import model.IssueRecord;
import model.Member;
import org.junit.jupiter.api.*;
import service.BookService;
import service.LibraryService;
import service.MemberService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class IssueReturnTest {

    private static LibraryService libraryService;
    private static BookService bookService;
    private static MemberService memberService;

    private static Member testMember;
    private static Book testBook;
    private static IssueRecord activeIssue;

    @BeforeAll
    public static void setUp() throws Exception {
        libraryService = new LibraryService();
        bookService = new BookService();
        memberService = new MemberService();

        testMember = memberService.registerMember(
                "Issue Tester",
                "issue.test" + System.currentTimeMillis() + "@university.edu",
                "9876543220",
                "Test Lab 1"
        );

        testBook = bookService.addBook(
                "Test Driven Development",
                "Kent Beck",
                "Computer Science",
                "978-0" + (System.nanoTime() % 10000000000L),
                "Addison-Wesley",
                2002,
                40.0,
                1 // Exactly 1 copy to test stock exhaustion
        );
    }

    @Test
    @Order(1)
    public void testSuccessfulBookIssue() throws Exception {
        activeIssue = libraryService.issueBook(testMember.getMemberId(), testBook.getBookId());

        assertNotNull(activeIssue);
        assertTrue(activeIssue.getIssueId() > 0);
        assertEquals("ISSUED", activeIssue.getStatus());
        assertEquals(LocalDate.now(), activeIssue.getIssueDate());
        assertEquals(LocalDate.now().plusDays(14), activeIssue.getDueDate());

        // Verify book available copy decremented to 0
        Book updatedBook = bookService.getBookById(testBook.getBookId());
        assertEquals(0, updatedBook.getAvailableCopies());
    }

    @Test
    @Order(2)
    public void testCannotIssueUnavailableBook() throws Exception {
        Member secondMember = memberService.registerMember(
                "Second Student",
                "second." + System.currentTimeMillis() + "@university.edu",
                "9876543233",
                "Hall E"
        );
        // Book now has 0 available copies
        assertThrows(BookUnavailableException.class, () -> {
            libraryService.issueBook(secondMember.getMemberId(), testBook.getBookId());
        });
    }

    @Test
    @Order(3)
    public void testInactiveMemberCannotIssue() throws Exception {
        Member inactiveMember = memberService.registerMember(
                "Inactive Student",
                "inactive" + System.currentTimeMillis() + "@university.edu",
                "9876543229",
                "Campus B"
        );
        memberService.deactivateMember(inactiveMember.getMemberId());

        Book anotherBook = bookService.addBook(
                "Database Systems",
                "Raghu Ramakrishnan",
                "Computer Science",
                "978-007246563" + (System.currentTimeMillis() % 10),
                "McGraw-Hill",
                2003,
                65.0,
                2
        );

        assertThrows(InvalidInputException.class, () -> {
            libraryService.issueBook(inactiveMember.getMemberId(), anotherBook.getBookId());
        });
    }

    @Test
    @Order(4)
    public void testReturnBookRestoresStock() throws Exception {
        IssueRecord returned = libraryService.returnBook(activeIssue.getIssueId(), false);

        assertNotNull(returned);
        assertEquals("RETURNED", returned.getStatus());
        assertNotNull(returned.getReturnDate());

        // Verify inventory copy restored to 1
        Book restoredBook = bookService.getBookById(testBook.getBookId());
        assertEquals(1, restoredBook.getAvailableCopies());
    }

    @Test
    @Order(5)
    public void testCannotReturnAlreadyReturnedBook() {
        assertThrows(InvalidInputException.class, () -> {
            libraryService.returnBook(activeIssue.getIssueId(), false);
        });
    }

    @Test
    @Order(6)
    public void testFineCalculationLogic() {
        IssueRecord record = new IssueRecord();
        record.setDueDate(LocalDate.now().minusDays(5)); // 5 days overdue

        long overdueDays = record.calculateOverdueDays(LocalDate.now());
        assertEquals(5, overdueDays);

        double fine = record.calculateFine(5.00, LocalDate.now());
        assertEquals(25.00, fine, 0.001);
    }
}
