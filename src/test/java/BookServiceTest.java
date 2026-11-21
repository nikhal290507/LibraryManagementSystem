import exceptions.BookNotFoundException;
import exceptions.DuplicateISBNException;
import exceptions.InvalidInputException;
import model.Book;
import org.junit.jupiter.api.*;
import service.BookService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BookServiceTest {

    private static BookService bookService;
    private static String testIsbn;

    @BeforeAll
    public static void setUp() {
        bookService = new BookService();
        testIsbn = "978-9" + (System.nanoTime() % 1000000000L);
    }

    @Test
    @Order(1)
    public void testAddBookSuccessfully() throws Exception {
        Book book = bookService.addBook(
                "Operating System Concepts",
                "Abraham Silberschatz",
                "Computer Science",
                testIsbn,
                "Wiley",
                2018,
                75.00,
                5
        );

        assertNotNull(book);
        assertTrue(book.getBookId() > 0);
        assertEquals("Operating System Concepts", book.getTitle());
        assertEquals(5, book.getAvailableCopies());
    }

    @Test
    @Order(2)
    public void testDuplicateISBNThrowsException() {
        assertThrows(DuplicateISBNException.class, () -> {
            bookService.addBook(
                    "Operating System Concepts Second Copy",
                    "Abraham Silberschatz",
                    "Computer Science",
                    testIsbn, // duplicate ISBN from test 1
                    "Wiley",
                    2018,
                    75.00,
                    2
            );
        });
    }

    @Test
    @Order(3)
    public void testInvalidInputValidations() {
        // Empty title
        assertThrows(InvalidInputException.class, () -> {
            bookService.addBook("", "Author", "Cat", "978-1111111111", "Pub", 2020, 10.0, 1);
        });

        // Negative price
        assertThrows(InvalidInputException.class, () -> {
            bookService.addBook("Title", "Author", "Cat", "978-1111111112", "Pub", 2020, -10.0, 1);
        });

        // Zero copies
        assertThrows(InvalidInputException.class, () -> {
            bookService.addBook("Title", "Author", "Cat", "978-1111111113", "Pub", 2020, 10.0, 0);
        });

        // Invalid future year
        assertThrows(InvalidInputException.class, () -> {
            bookService.addBook("Title", "Author", "Cat", "978-1111111114", "Pub", 3000, 10.0, 1);
        });
    }

    @Test
    @Order(4)
    public void testGetBookByIdAndSearch() throws Exception {
        List<Book> books = bookService.searchBooksByTitle("Operating System");
        assertFalse(books.isEmpty());

        Book found = bookService.getBookById(books.get(0).getBookId());
        assertEquals(books.get(0).getBookId(), found.getBookId());
    }

    @Test
    @Order(5)
    public void testNonExistentBookThrowsException() {
        assertThrows(BookNotFoundException.class, () -> {
            bookService.getBookById(999999);
        });
    }
}
