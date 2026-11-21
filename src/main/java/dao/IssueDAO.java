package dao;

import database.DBConnection;
import exceptions.DatabaseException;
import model.IssueRecord;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Book Issue, Return, and Fine Tracking (Lectures 34-42 JDBC).
 * Supports external Connection passing for strict transaction management (commit & rollback).
 */
public class IssueDAO {

    public int createIssue(IssueRecord record, Connection conn) throws SQLException {
        String sql = "INSERT INTO issue_records (member_id, book_id, issue_date, due_date, status, fine_amount, fine_paid) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, record.getMemberId());
            ps.setInt(2, record.getBookId());
            ps.setDate(3, Date.valueOf(record.getIssueDate()));
            ps.setDate(4, Date.valueOf(record.getDueDate()));
            ps.setString(5, "ISSUED");
            ps.setDouble(6, 0.0);
            ps.setBoolean(7, false);

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    record.setIssueId(id);
                    return id;
                }
            }
            return -1;
        }
    }

    public IssueRecord getIssueById(int issueId) throws DatabaseException {
        String sql = "SELECT ir.*, m.name AS member_name, b.title AS book_title " +
                "FROM issue_records ir " +
                "JOIN members m ON ir.member_id = m.member_id " +
                "JOIN books b ON ir.book_id = b.book_id " +
                "WHERE ir.issue_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, issueId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToIssue(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving issue record #" + issueId + ": " + e.getMessage(), e);
        }
    }

    public IssueRecord getActiveIssue(int memberId, int bookId) throws DatabaseException {
        String sql = "SELECT ir.*, m.name AS member_name, b.title AS book_title " +
                "FROM issue_records ir " +
                "JOIN members m ON ir.member_id = m.member_id " +
                "JOIN books b ON ir.book_id = b.book_id " +
                "WHERE ir.member_id = ? AND ir.book_id = ? AND ir.status IN ('ISSUED', 'OVERDUE')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            ps.setInt(2, bookId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToIssue(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error checking existing active issue: " + e.getMessage(), e);
        }
    }

    public List<IssueRecord> getAllIssues() throws DatabaseException {
        List<IssueRecord> list = new ArrayList<>();
        String sql = "SELECT ir.*, m.name AS member_name, b.title AS book_title " +
                "FROM issue_records ir " +
                "JOIN members m ON ir.member_id = m.member_id " +
                "JOIN books b ON ir.book_id = b.book_id " +
                "ORDER BY ir.issue_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToIssue(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving issue history: " + e.getMessage(), e);
        }
    }

    public List<IssueRecord> getActiveIssues() throws DatabaseException {
        List<IssueRecord> list = new ArrayList<>();
        String sql = "SELECT ir.*, m.name AS member_name, b.title AS book_title " +
                "FROM issue_records ir " +
                "JOIN members m ON ir.member_id = m.member_id " +
                "JOIN books b ON ir.book_id = b.book_id " +
                "WHERE ir.status IN ('ISSUED', 'OVERDUE') " +
                "ORDER BY ir.due_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToIssue(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching active issues: " + e.getMessage(), e);
        }
    }

    public List<IssueRecord> getOverdueIssues(LocalDate currentDate) throws DatabaseException {
        List<IssueRecord> list = new ArrayList<>();
        String sql = "SELECT ir.*, m.name AS member_name, b.title AS book_title " +
                "FROM issue_records ir " +
                "JOIN members m ON ir.member_id = m.member_id " +
                "JOIN books b ON ir.book_id = b.book_id " +
                "WHERE ir.status IN ('ISSUED', 'OVERDUE') AND ir.due_date < ? " +
                "ORDER BY ir.due_date ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(currentDate != null ? currentDate : LocalDate.now()));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToIssue(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding overdue issues: " + e.getMessage(), e);
        }
    }

    public List<IssueRecord> getIssuesByMember(int memberId) throws DatabaseException {
        List<IssueRecord> list = new ArrayList<>();
        String sql = "SELECT ir.*, m.name AS member_name, b.title AS book_title " +
                "FROM issue_records ir " +
                "JOIN members m ON ir.member_id = m.member_id " +
                "JOIN books b ON ir.book_id = b.book_id " +
                "WHERE ir.member_id = ? ORDER BY ir.issue_id DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToIssue(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching member borrowing records: " + e.getMessage(), e);
        }
    }

    public boolean returnBook(int issueId, LocalDate returnDate, double fineAmount, boolean finePaid, Connection conn) throws SQLException {
        String sql = "UPDATE issue_records SET return_date = ?, fine_amount = ?, status = 'RETURNED', fine_paid = ? WHERE issue_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(returnDate));
            ps.setDouble(2, fineAmount);
            ps.setBoolean(3, finePaid);
            ps.setInt(4, issueId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateOverdueStatus(int issueId, double fineAmount) throws DatabaseException {
        String sql = "UPDATE issue_records SET status = 'OVERDUE', fine_amount = ? WHERE issue_id = ? AND status = 'ISSUED'";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, fineAmount);
            ps.setInt(2, issueId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating overdue status: " + e.getMessage(), e);
        }
    }

    public boolean payFine(int issueId) throws DatabaseException {
        String sql = "UPDATE issue_records SET fine_paid = TRUE WHERE issue_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, issueId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error recording fine payment: " + e.getMessage(), e);
        }
    }

    public int getTotalActiveIssuedCount() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM issue_records WHERE status IN ('ISSUED', 'OVERDUE')";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting active issues: " + e.getMessage(), e);
        }
    }

    public double getTotalCollectedFines() throws DatabaseException {
        String sql = "SELECT COALESCE(SUM(fine_amount), 0.0) FROM issue_records WHERE fine_paid = TRUE";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
            return 0.0;
        } catch (SQLException e) {
            throw new DatabaseException("Error calculating collected fines: " + e.getMessage(), e);
        }
    }

    public double getTotalPendingFines() throws DatabaseException {
        String sql = "SELECT COALESCE(SUM(fine_amount), 0.0) FROM issue_records WHERE fine_amount > 0 AND fine_paid = FALSE";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getDouble(1);
            }
            return 0.0;
        } catch (SQLException e) {
            throw new DatabaseException("Error calculating pending fines: " + e.getMessage(), e);
        }
    }

    private IssueRecord mapResultSetToIssue(ResultSet rs) throws SQLException {
        Date returnDateSql = rs.getDate("return_date");
        LocalDate returnDate = (returnDateSql != null) ? returnDateSql.toLocalDate() : null;

        return new IssueRecord(
                rs.getInt("issue_id"),
                rs.getInt("member_id"),
                rs.getString("member_name"),
                rs.getInt("book_id"),
                rs.getString("book_title"),
                rs.getDate("issue_date").toLocalDate(),
                rs.getDate("due_date").toLocalDate(),
                returnDate,
                rs.getDouble("fine_amount"),
                rs.getString("status"),
                rs.getBoolean("fine_paid")
        );
    }
}
