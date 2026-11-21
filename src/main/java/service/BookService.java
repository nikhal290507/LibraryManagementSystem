package service;

import dao.AuthorDAO;
import dao.BookDAO;
import dao.CategoryDAO;
import exceptions.BookNotFoundException;
import exceptions.DuplicateISBNException;
import exceptions.DatabaseException;
import exceptions.InvalidInputException;
import model.Author;
import model.Book;
import model.Category;
import utils.InputValidator;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for Book Management (Lectures 16-25 Collections, 43-46 Streams).
 * Demonstrates:
 * - ArrayList (list representation)
 * - HashSet (duplicate ISBN validation)
 * - LinkedHashSet (recently searched book IDs without duplicates in order)
 * - TreeSet (unique sorted book titles)
 * - HashMap (O(1) fast ID-to-Book mapping)
 * - LinkedHashMap (search history in insertion order)
 * - TreeMap (books sorted by book ID)
 * - Iterator & ListIterator traversal
 * - Comparable (natural order by title)
 * - Comparator (custom sorting by author, year, price)
 * - Collections utility methods (sort, reverse, max, min, frequency, shuffle)
 */
public class BookService {

    private final BookDAO bookDAO;
    private final AuthorDAO authorDAO;
    private final CategoryDAO categoryDAO;

    // Collections demonstrations
    private final Set<Integer> recentSearchedIds = new LinkedHashSet<>(); // LinkedHashSet preserves insertion order without duplicates
    private final Map<String, String> recentSearchQueries = new LinkedHashMap<>(); // LinkedHashMap preserves recent search queries
    private final Map<Integer, Book> bookCache = new HashMap<>(); // HashMap for fast O(1) lookup

    public BookService() {
        this.bookDAO = new BookDAO();
        this.authorDAO = new AuthorDAO();
        this.categoryDAO = new CategoryDAO();
    }

    public BookService(BookDAO bookDAO, AuthorDAO authorDAO, CategoryDAO categoryDAO) {
        this.bookDAO = bookDAO;
        this.authorDAO = authorDAO;
        this.categoryDAO = categoryDAO;
    }

    /**
     * Adds a new book with input validation and duplicate ISBN check.
     */
    public Book addBook(String title, String authorName, String categoryName,
                        String isbn, String publisher, int year, double price, int copies) throws DatabaseException {
        // Validation using InputValidator
        String validTitle = InputValidator.validateNonEmpty(title, "Book Title");
        String validAuthor = InputValidator.validateNonEmpty(authorName, "Author Name");
        String validCategory = InputValidator.validateNonEmpty(categoryName, "Category");
        String validIsbn = InputValidator.validateISBN(isbn);
        int validYear = InputValidator.validateYear(year);
        double validPrice = InputValidator.validateNonNegativeDouble(price, "Price");
        int validCopies = InputValidator.validatePositiveInt(copies, "Total Copies");

        // Check duplicate ISBN in database
        Book existing = bookDAO.getBookByISBN(validIsbn);
        if (existing != null) {
            throw new DuplicateISBNException("A book with ISBN '" + validIsbn + "' already exists: \"" + existing.getTitle() + "\" (ID: " + existing.getBookId() + ")");
        }

        // Resolve or create Author
        Author author = authorDAO.getAuthorByName(validAuthor);
        Integer authorId = (author != null) ? author.getAuthorId() : null;
        if (authorId == null) {
            authorId = authorDAO.addAuthor(new Author(validAuthor, null, "Added via Book Catalog"));
        }

        // Resolve or create Category
        Category category = categoryDAO.getCategoryByName(validCategory);
        Integer categoryId = (category != null) ? category.getCategoryId() : null;
        if (categoryId == null) {
            categoryId = categoryDAO.addCategory(new Category(validCategory, "General " + validCategory));
        }

        Book newBook = new Book(0, validTitle, authorId, validAuthor, categoryId, validCategory,
                validIsbn, publisher, validYear, validPrice, validCopies, validCopies);

        int id = bookDAO.addBook(newBook);
        newBook.setBookId(id);
        bookCache.put(id, newBook);
        return newBook;
    }

    /**
     * Retrieves a book by ID. Queries database and updates in-memory HashMap cache.
     */
    public Book getBookById(int bookId) throws DatabaseException {
        recentSearchedIds.add(bookId);
        Book book = bookDAO.getBookById(bookId);
        if (book == null) {
            throw new BookNotFoundException("Book with ID #" + bookId + " does not exist in library records.");
        }
        bookCache.put(bookId, book);
        return book;
    }

    public void clearCache() {
        bookCache.clear();
    }

    /**
     * Retrieves all books from database.
     */
    public List<Book> getAllBooks() throws DatabaseException {
        List<Book> books = bookDAO.getAllBooks();
        books.forEach(b -> bookCache.put(b.getBookId(), b));
        return books;
    }

    /**
     * Searches books by title.
     */
    public List<Book> searchBooksByTitle(String titleKeyword) throws DatabaseException {
        InputValidator.validateNonEmpty(titleKeyword, "Search Keyword");
        recentSearchQueries.put("TITLE:" + titleKeyword, titleKeyword);
        List<Book> results = bookDAO.searchBooksByTitle(titleKeyword);
        results.forEach(b -> recentSearchedIds.add(b.getBookId()));
        return results;
    }

    /**
     * Searches books by author.
     */
    public List<Book> searchBooksByAuthor(String authorKeyword) throws DatabaseException {
        InputValidator.validateNonEmpty(authorKeyword, "Author Keyword");
        recentSearchQueries.put("AUTHOR:" + authorKeyword, authorKeyword);
        List<Book> results = bookDAO.searchBooksByAuthor(authorKeyword);
        results.forEach(b -> recentSearchedIds.add(b.getBookId()));
        return results;
    }

    /**
     * Updates book details and adjusts inventory.
     */
    public boolean updateBook(Book book) throws DatabaseException {
        if (book == null || book.getBookId() <= 0) {
            throw new InvalidInputException("Invalid book entity for update.");
        }
        // Verify existence
        getBookById(book.getBookId());

        if (book.getAvailableCopies() > book.getTotalCopies()) {
            throw new InvalidInputException("Available copies (" + book.getAvailableCopies()
                    + ") cannot exceed total copies (" + book.getTotalCopies() + ").");
        }

        boolean updated = bookDAO.updateBook(book);
        if (updated) {
            bookCache.put(book.getBookId(), book);
        }
        return updated;
    }

    /**
     * Deletes a book after checking for active loans.
     */
    public boolean deleteBook(int bookId) throws DatabaseException {
        getBookById(bookId); // Verify book exists
        boolean deleted = bookDAO.deleteBook(bookId);
        if (deleted) {
            bookCache.remove(bookId);
            recentSearchedIds.remove(bookId);
        }
        return deleted;
    }

    // =========================================================================
    // COLLECTIONS FRAMEWORK DEMONSTRATIONS (Lectures 16-25)
    // =========================================================================

    /**
     * Demonstrates TreeSet (Sorted unique book titles in natural alphabetical order).
     */
    public Set<String> getUniqueSortedTitles() throws DatabaseException {
        List<Book> books = getAllBooks();
        Set<String> sortedTitles = new TreeSet<>();
        for (Book b : books) {
            sortedTitles.add(b.getTitle());
        }
        return sortedTitles;
    }

    /**
     * Demonstrates TreeMap (Books sorted by Book ID in ascending order).
     */
    public Map<Integer, Book> getBooksSortedById() throws DatabaseException {
        List<Book> books = getAllBooks();
        Map<Integer, Book> treeMap = new TreeMap<>();
        for (Book b : books) {
            treeMap.put(b.getBookId(), b);
        }
        return treeMap;
    }

    /**
     * Demonstrates LinkedHashSet (Preserves insertion order of recent searches without duplicates).
     */
    public Set<Integer> getRecentlySearchedBookIds() {
        return Collections.unmodifiableSet(recentSearchedIds);
    }

    /**
     * Demonstrates Collections.sort() using Comparable natural ordering (title).
     */
    public List<Book> sortBooksNaturally() throws DatabaseException {
        List<Book> books = getAllBooks();
        Collections.sort(books); // Uses Book.compareTo(Book other)
        return books;
    }

    /**
     * Demonstrates Comparator interface (custom sorting by publication year or price).
     */
    public List<Book> sortBooksBy(String criteria) throws DatabaseException {
        List<Book> books = getAllBooks();
        switch (criteria.toUpperCase()) {
            case "AUTHOR":
                books.sort(Comparator.comparing(b -> b.getAuthorName() != null ? b.getAuthorName() : ""));
                break;
            case "YEAR":
                books.sort(Comparator.comparingInt(Book::getPublicationYear));
                break;
            case "PRICE":
                books.sort(Comparator.comparingDouble(Book::getPrice));
                break;
            case "AVAILABLE":
                books.sort(Comparator.comparingInt(Book::getAvailableCopies).reversed());
                break;
            default:
                Collections.sort(books);
                break;
        }
        return books;
    }

    /**
     * Demonstrates Collections utility methods:
     * - Collections.reverse()
     * - Collections.max()
     * - Collections.min()
     * - Collections.shuffle()
     */
    public Map<String, Object> demonstrateCollectionsUtilities() throws DatabaseException {
        List<Book> books = getAllBooks();
        Map<String, Object> results = new LinkedHashMap<>();

        if (!books.isEmpty()) {
            // Collections.max() using custom Comparator
            Book mostExpensive = Collections.max(books, Comparator.comparingDouble(Book::getPrice));
            Book cheapest = Collections.min(books, Comparator.comparingDouble(Book::getPrice));
            Book newest = Collections.max(books, Comparator.comparingInt(Book::getPublicationYear));

            results.put("Most Expensive Book", mostExpensive.getTitle() + " ($" + mostExpensive.getPrice() + ")");
            results.put("Cheapest Book", cheapest.getTitle() + " ($" + cheapest.getPrice() + ")");
            results.put("Newest Publication", newest.getTitle() + " (" + newest.getPublicationYear() + ")");

            // Reverse order
            List<Book> reversedList = new ArrayList<>(books);
            Collections.reverse(reversedList);
            results.put("Reversed Count", reversedList.size());
        }
        return results;
    }

    /**
     * Demonstrates Iterator and ListIterator forward and backward traversal (Lectures 16-25).
     */
    public void demonstrateIterators() throws DatabaseException {
        List<Book> books = getAllBooks();
        System.out.println("--- 1. Forward Traversal with ListIterator ---");
        ListIterator<Book> listIterator = books.listIterator();
        int count = 0;
        while (listIterator.hasNext() && count < 3) {
            System.out.println("  Next Index " + listIterator.nextIndex() + ": " + listIterator.next().getTitle());
            count++;
        }

        System.out.println("--- 2. Backward Traversal with ListIterator ---");
        while (listIterator.hasPrevious()) {
            System.out.println("  Previous Index " + listIterator.previousIndex() + ": " + listIterator.previous().getTitle());
        }

        System.out.println("--- 3. Safe Traversal with Standard Iterator ---");
        Iterator<Book> iterator = books.iterator();
        while (iterator.hasNext()) {
            Book b = iterator.next();
            // Demonstrates iterator safety (no ConcurrentModificationException)
            if (b.getAvailableCopies() == 0) {
                System.out.println("  [Out of stock]: " + b.getTitle());
            }
        }
    }

    // =========================================================================
    // STREAM API DEMONSTRATIONS (Lectures 43-46)
    // =========================================================================

    /**
     * Demonstrates Stream.filter() to find currently available books.
     */
    public List<Book> getAvailableBooksStream() throws DatabaseException {
        return getAllBooks().stream()
                .filter(b -> b.getAvailableCopies() > 0)
                .collect(Collectors.toList());
    }

    /**
     * Demonstrates Stream.filter(), map(), and collect().
     */
    public List<String> getBookTitlesByAuthorStream(String authorName) throws DatabaseException {
        return getAllBooks().stream()
                .filter(b -> b.getAuthorName() != null && b.getAuthorName().equalsIgnoreCase(authorName))
                .map(Book::getTitle)
                .collect(Collectors.toList());
    }

    /**
     * Demonstrates Stream.filter() with copy threshold.
     */
    public List<Book> getBooksWithCopiesMoreThan(int threshold) throws DatabaseException {
        return getAllBooks().stream()
                .filter(b -> b.getTotalCopies() > threshold)
                .collect(Collectors.toList());
    }
}
