package dao;

import database.DBConnection;
import exceptions.DatabaseException;
import model.Author;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Authors (Lectures 34-42 JDBC).
 * Uses PreparedStatement to prevent SQL injection and try-with-resources.
 */
public class AuthorDAO {

    public int addAuthor(Author author) throws DatabaseException {
        String sql = "INSERT INTO authors (name, email, biography) VALUES (?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, author.getName());
            ps.setString(2, author.getEmail());
            ps.setString(3, author.getBiography());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    author.setAuthorId(id);
                    return id;
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new DatabaseException("Error inserting author: " + e.getMessage(), e);
        }
    }

    public Author getAuthorById(int id) throws DatabaseException {
        String sql = "SELECT * FROM authors WHERE author_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAuthor(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving author with ID " + id + ": " + e.getMessage(), e);
        }
    }

    public Author getAuthorByName(String name) throws DatabaseException {
        String sql = "SELECT * FROM authors WHERE LOWER(name) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAuthor(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error searching author by name: " + e.getMessage(), e);
        }
    }

    public List<Author> getAllAuthors() throws DatabaseException {
        List<Author> authors = new ArrayList<>();
        String sql = "SELECT * FROM authors ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                authors.add(mapResultSetToAuthor(rs));
            }
            return authors;
        } catch (SQLException e) {
            throw new DatabaseException("Error listing authors: " + e.getMessage(), e);
        }
    }

    public boolean updateAuthor(Author author) throws DatabaseException {
        String sql = "UPDATE authors SET name = ?, email = ?, biography = ? WHERE author_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, author.getName());
            ps.setString(2, author.getEmail());
            ps.setString(3, author.getBiography());
            ps.setInt(4, author.getAuthorId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating author: " + e.getMessage(), e);
        }
    }

    public boolean deleteAuthor(int id) throws DatabaseException {
        // Check if books are referencing this author
        String checkSql = "SELECT COUNT(*) FROM books WHERE author_id = ?";
        String deleteSql = "DELETE FROM authors WHERE author_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
            checkPs.setInt(1, id);
            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    throw new DatabaseException("Cannot delete author: Existing books reference this author.");
                }
            }
            try (PreparedStatement deletePs = conn.prepareStatement(deleteSql)) {
                deletePs.setInt(1, id);
                return deletePs.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting author: " + e.getMessage(), e);
        }
    }

    private Author mapResultSetToAuthor(ResultSet rs) throws SQLException {
        return new Author(
                rs.getInt("author_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("biography")
        );
    }
}
