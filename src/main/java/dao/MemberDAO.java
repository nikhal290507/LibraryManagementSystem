package dao;

import database.DBConnection;
import exceptions.DatabaseException;
import model.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object for Member Management (Lectures 34-42 JDBC).
 */
public class MemberDAO {

    public int addMember(Member member) throws DatabaseException {
        String sql = "INSERT INTO members (name, email, phone, address, registration_date, is_active, max_books_allowed) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, member.getName());
            ps.setString(2, member.getEmail());
            ps.setString(3, member.getPhone());
            ps.setString(4, member.getAddress());
            ps.setDate(5, Date.valueOf(member.getRegistrationDate()));
            ps.setBoolean(6, member.isActive());
            ps.setInt(7, member.getMaxBooksAllowed());

            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    member.setMemberId(id);
                    return id;
                }
            }
            return -1;
        } catch (SQLException e) {
            throw new DatabaseException("Failed to register member: " + e.getMessage(), e);
        }
    }

    public Member getMemberById(int id) throws DatabaseException {
        String sql = "SELECT * FROM members WHERE member_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMember(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error retrieving member ID " + id + ": " + e.getMessage(), e);
        }
    }

    public Member getMemberByEmail(String email) throws DatabaseException {
        String sql = "SELECT * FROM members WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMember(rs);
                }
            }
            return null;
        } catch (SQLException e) {
            throw new DatabaseException("Error finding member with email " + email + ": " + e.getMessage(), e);
        }
    }

    public List<Member> getAllMembers() throws DatabaseException {
        List<Member> list = new ArrayList<>();
        String sql = "SELECT * FROM members ORDER BY member_id ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToMember(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error fetching members: " + e.getMessage(), e);
        }
    }

    public List<Member> searchMembersByName(String nameKeyword) throws DatabaseException {
        List<Member> list = new ArrayList<>();
        String sql = "SELECT * FROM members WHERE LOWER(name) LIKE LOWER(?) ORDER BY name ASC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + nameKeyword.trim() + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToMember(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new DatabaseException("Error searching members by name: " + e.getMessage(), e);
        }
    }

    public boolean updateMember(Member member) throws DatabaseException {
        String sql = "UPDATE members SET name = ?, email = ?, phone = ?, address = ?, " +
                "is_active = ?, max_books_allowed = ? WHERE member_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, member.getName());
            ps.setString(2, member.getEmail());
            ps.setString(3, member.getPhone());
            ps.setString(4, member.getAddress());
            ps.setBoolean(5, member.isActive());
            ps.setInt(6, member.getMaxBooksAllowed());
            ps.setInt(7, member.getMemberId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error updating member: " + e.getMessage(), e);
        }
    }

    public boolean deleteMember(int memberId) throws DatabaseException {
        // Prevent deletion if active books are issued
        int activeCount = getActiveIssuedCount(memberId);
        if (activeCount > 0) {
            throw new DatabaseException("Cannot delete member: Member has " + activeCount + " book(s) currently issued!");
        }
        String sql = "DELETE FROM members WHERE member_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error deleting member: " + e.getMessage(), e);
        }
    }

    public boolean setMemberActiveStatus(int memberId, boolean active) throws DatabaseException {
        String sql = "UPDATE members SET is_active = ? WHERE member_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active);
            ps.setInt(2, memberId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error modifying member active status: " + e.getMessage(), e);
        }
    }

    public int getActiveIssuedCount(int memberId) throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM issue_records WHERE member_id = ? AND status IN ('ISSUED', 'OVERDUE')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error checking member active issues: " + e.getMessage(), e);
        }
    }

    public int getTotalMemberCount() throws DatabaseException {
        String sql = "SELECT COUNT(*) FROM members";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException e) {
            throw new DatabaseException("Error counting members: " + e.getMessage(), e);
        }
    }

    private Member mapResultSetToMember(ResultSet rs) throws SQLException {
        return new Member(
                rs.getInt("member_id"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"),
                rs.getString("address"),
                rs.getDate("registration_date").toLocalDate(),
                rs.getBoolean("is_active"),
                rs.getInt("max_books_allowed")
        );
    }
}
