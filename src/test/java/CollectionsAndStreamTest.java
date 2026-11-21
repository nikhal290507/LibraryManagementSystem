import model.Book;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import service.BookService;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class CollectionsAndStreamTest {

    private static BookService bookService;

    @BeforeAll
    public static void setUp() {
        bookService = new BookService();
    }

    @Test
    public void testComparableNaturalSortingByTitle() throws Exception {
        List<Book> sorted = bookService.sortBooksNaturally();
        assertFalse(sorted.isEmpty());

        for (int i = 0; i < sorted.size() - 1; i++) {
            assertTrue(sorted.get(i).getTitle().compareToIgnoreCase(sorted.get(i + 1).getTitle()) <= 0,
                    "Books must be naturally sorted in alphabetical order by title.");
        }
    }

    @Test
    public void testComparatorSortingByPrice() throws Exception {
        List<Book> sortedByPrice = bookService.sortBooksBy("PRICE");
        assertFalse(sortedByPrice.isEmpty());

        for (int i = 0; i < sortedByPrice.size() - 1; i++) {
            assertTrue(sortedByPrice.get(i).getPrice() <= sortedByPrice.get(i + 1).getPrice(),
                    "Books must be ordered by price ascending.");
        }
    }

    @Test
    public void testTreeSetAndTreeMapCollections() throws Exception {
        Set<String> titlesInTreeSet = bookService.getUniqueSortedTitles();
        assertNotNull(titlesInTreeSet);
        assertFalse(titlesInTreeSet.isEmpty());

        Map<Integer, Book> mapInTreeMap = bookService.getBooksSortedById();
        assertNotNull(mapInTreeMap);
        assertFalse(mapInTreeMap.isEmpty());

        // Verify keys in TreeMap are strictly increasing
        int previousKey = -1;
        for (Integer key : mapInTreeMap.keySet()) {
            assertTrue(key > previousKey);
            previousKey = key;
        }
    }

    @Test
    public void testStreamApiOperations() throws Exception {
        List<Book> all = bookService.getAllBooks();

        // 1. Stream filter available books
        List<Book> available = all.stream()
                .filter(b -> b.getAvailableCopies() > 0)
                .collect(Collectors.toList());
        for (Book b : available) {
            assertTrue(b.getAvailableCopies() > 0);
        }

        // 2. Stream map titles
        List<String> titles = all.stream()
                .map(Book::getTitle)
                .collect(Collectors.toList());
        assertEquals(all.size(), titles.size());

        // 3. Stream distinct categories
        List<String> distinctCategories = all.stream()
                .map(Book::getCategoryName)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        assertFalse(distinctCategories.isEmpty());

        // 4. Stream max price
        OptionalDouble maxPrice = all.stream()
                .mapToDouble(Book::getPrice)
                .max();
        assertTrue(maxPrice.isPresent());
        assertTrue(maxPrice.getAsDouble() > 0.0);
    }
}
