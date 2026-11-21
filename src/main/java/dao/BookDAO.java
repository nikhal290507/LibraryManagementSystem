package dao;

import database.DBConnection;
import exceptions.DatabaseException;
import model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Book Management (Lectures 34-42 JDBC).
 * Demonstrates PreparedStatement, Statement, ResultSet navigation,
 * transaction support, and atomic conditional updates.
 */
public class BookDAO {

    public int addBook(Book book) throws DatabaseException {
        String sql = "INSERT INTO books (title, author_id, author_name, category_id, category_name, " +
                "isbn, publisher, publication_year, price, total_copies, available_copies) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, book.getTitle());
            if (book.getAuthorId() != null) ps.setInt(2, book.getAuthorId()); else ps.setNull(2, Types.INTEGER);
            ps.setString(3, book.getAuthorName());
            if (book.getCategoryId() != null) ps.setInt(4, book.getCategoryId()); else ps.setNull(4, Types.INTEGER);
            ps.setString(5, book.getCategoryName());
            ps.setString(6, book.getIsbn());
            ps.setString(7, book.getPublisher());
            ps.setInt(8, book.getPublicationYear());
            ps.setDouble(9, book.getPrice());
            ps.setInt(10, book.getTotalCopies());
            ps.setInt(11, book.getAvailableCopies());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    book.setBookId(id);
                    return id;
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to insert book: " + e.getMessage(), e);
        }
    }

    public Book getBookById(int id) throws DatabaseException {
        String sql = "SELECT * FROM books WHERE book_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBook(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving book with ID " + id + ": " + e.getMessage(), e);
        }
    }

    public Book getBookByISBN(String isbn) throws DatabaseException {
        String sql = "SELECT * FROM books WHERE isbn = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, isbn.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToBook(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding book with ISBN " + isbn + ": " + e.getMessage(), e);
        }
    }

    public List<Book> getAllBooks() throws DatabaseException {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT * FROM books ORDER BY book_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToBook(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching books: " + e.getMessage(), e);
        }
    }

    public List<Book> searchBooksByTitle(String titleKeyword) throws DatabaseException {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE LOWER(title) LIKE LOWER(?) ORDER BY title ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + titleKeyword.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBook(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error searching books by title: " + e.getMessage(), e);
        }
    }

    public List<Book> searchBooksByAuthor(String authorKeyword) throws DatabaseException {
        List<Book> list = new ArrayList<>();
        String sql = "SELECT * FROM books WHERE LOWER(author_name) LIKE LOWER(?) ORDER BY title ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + authorKeyword.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToBook(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error searching books by author: " + e.getMessage(), e);
        }
    }

    public boolean updateBook(Book book) throws DatabaseException {
        String sql = "UPDATE books SET title = ?, author_name = ?, category_name = ?, " +
                "publisher = ?, publication_year = ?, price = ?, total_copies = ?, available_copies = ? " +
                "WHERE book_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, book.getTitle());
            ps.setString(2, book.getAuthorName());
            ps.setString(3, book.getCategoryName());
            ps.setString(4, book.getPublisher());
            ps.setInt(5, book.getPublicationYear());
            ps.setDouble(6, book.getPrice());
            ps.setInt(7, book.getTotalCopies());
            ps.setInt(8, book.getAvailableCopies());
            ps.setInt(9, book.getBookId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating book: " + e.getMessage(), e);
        }
    }

    public boolean deleteBook(int bookId) throws DatabaseException {
        // Prevent deletion if copies are currently issued
        String checkSql = "SELECT COUNT(*) FROM issue_records WHERE book_id = ? AND status IN ('ISSUED', 'OVERDUE')";
        String deleteSql = "DELETE FROM books WHERE book_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setInt(1, bookId);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new DatabaseException("Cannot delete book: Copies of this book are currently issued to members!");
                }
            }
            try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                deletePs.setInt(1, bookId);
                return deletePs.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting book: " + e.getMessage(), e);
        }
    }

    /**
     * Atomically decrements available copies within a database transaction.
     * Prevents race conditions when two threads attempt to issue the last available copy.
     * Returns true only if decrement succeeded.
     */
    public boolean atomicDecrementAvailableCopies(int bookId, Connection conn) throws SQLException {
        String sql = "UPDATE books SET available_copies = available_copies - 1 WHERE book_id = ? AND available_copies > 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            int rows = ps.executeUpdate();
            return rows > 0;
        }
    }

    /**
     * Atomically increments available copies upon book return.
     */
    public boolean atomicIncrementAvailableCopies(int bookId, Connection conn) throws SQLException {
        String sql = "UPDATE books SET available_copies = available_copies + 1 WHERE book_id = ? AND available_copies < total_copies";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Demonstrates Statement for static query (Lectures 34-42).
     */
    public int getTotalDistinctBookCount() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM books";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting books: " + e.getMessage(), e);
        }
    }

    public int getTotalCopiesCount() throws DatabaseException {
        String sql = "SELECT COALESCE(SUM(total_copies), 0) FROM books";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error calculating total copies: " + e.getMessage(), e);
        }
    }

    public int getTotalAvailableCopiesCount() throws DatabaseException {
        String sql = "SELECT COALESCE(SUM(available_copies), 0) FROM books";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error calculating available copies: " + e.getMessage(), e);
        }
    }

    private Book mapResultSetToBook(ResultSet rs) throws SQLException {
        return new Book(
                rs.getInt("book_id"),
                rs.getString("title"),
                (Integer) rs.getObject("author_id"),
                rs.getString("author_name"),
                (Integer) rs.getObject("category_id"),
                rs.getString("category_name"),
                rs.getString("isbn"),
                rs.getString("publisher"),
                rs.getInt("publication_year"),
                rs.getDouble("price"),
                rs.getInt("total_copies"),
                rs.getInt("available_copies")
        );
    }
}
