package service;

import dao.MemberDAO;
import exceptions.DatabaseException;
import exceptions.InvalidInputException;
import exceptions.MemberNotFoundException;
import model.Member;
import utils.InputValidator;

import java.time.LocalDate;
import java.util.LinkedList;
import java.util.List;

/**
 * Service class for Member Management (Lectures 10-15 Validation, 16-25 Collections).
 */
public class MemberService {

    private final MemberDAO memberDAO;
    // LinkedList to maintain recent member registrations (Lectures 16-25 Collections)
    private final LinkedList<Member> recentRegistrations = new LinkedList<>();

    public MemberService() {
        this.memberDAO = new MemberDAO();
    }

    public MemberService(MemberDAO memberDAO) {
        this.memberDAO = memberDAO;
    }

    /**
     * Registers a new member with validation against duplicate email, phone, and empty name.
     */
    public Member registerMember(String name, String email, String phone, String address) throws DatabaseException {
        String validName = InputValidator.validateNonEmpty(name, "Member Name");
        String validEmail = InputValidator.validateEmail(email);
        String validPhone = InputValidator.validatePhone(phone);
        String validAddress = (address != null) ? address.trim() : "";

        // Check duplicate email
        Member existing = memberDAO.getMemberByEmail(validEmail);
        if (existing != null) {
            throw new InvalidInputException("A member with email '" + validEmail + "' is already registered: "
                    + existing.getName() + " (ID: " + existing.getMemberId() + ")");
        }

        Member member = new Member(0, validName, validEmail, validPhone, validAddress,
                LocalDate.now(), true, 3);
        int memberId = memberDAO.addMember(member);
        member.setMemberId(memberId);

        // Add to recent registrations LinkedList
        synchronized (recentRegistrations) {
            recentRegistrations.addFirst(member);
            if (recentRegistrations.size() > 10) {
                recentRegistrations.removeLast();
            }
        }

        return member;
    }

    public Member getMemberById(int memberId) throws DatabaseException {
        Member member = memberDAO.getMemberById(memberId);
        if (member == null) {
            throw new MemberNotFoundException("Member with ID #" + memberId + " not found in library records.");
        }
        return member;
    }

    public Member getMemberByEmail(String email) throws DatabaseException {
        String validEmail = InputValidator.validateEmail(email);
        Member member = memberDAO.getMemberByEmail(validEmail);
        if (member == null) {
            throw new MemberNotFoundException("Member with email '" + validEmail + "' not found.");
        }
        return member;
    }

    public List<Member> getAllMembers() throws DatabaseException {
        return memberDAO.getAllMembers();
    }

    public List<Member> searchMembersByName(String nameKeyword) throws DatabaseException {
        InputValidator.validateNonEmpty(nameKeyword, "Search Keyword");
        return memberDAO.searchMembersByName(nameKeyword);
    }

    public boolean updateMember(Member member) throws DatabaseException {
        if (member == null || member.getMemberId() <= 0) {
            throw new InvalidInputException("Invalid member entity for update.");
        }
        getMemberById(member.getMemberId()); // Verify existence

        InputValidator.validateNonEmpty(member.getName(), "Member Name");
        InputValidator.validateEmail(member.getEmail());
        InputValidator.validatePhone(member.getPhone());

        return memberDAO.updateMember(member);
    }

    public boolean deleteMember(int memberId) throws DatabaseException {
        getMemberById(memberId); // Verify existence
        return memberDAO.deleteMember(memberId);
    }

    public boolean deactivateMember(int memberId) throws DatabaseException {
        getMemberById(memberId);
        return memberDAO.setMemberActiveStatus(memberId, false);
    }

    public boolean activateMember(int memberId) throws DatabaseException {
        getMemberById(memberId);
        return memberDAO.setMemberActiveStatus(memberId, true);
    }

    public int getActiveIssuedCount(int memberId) throws DatabaseException {
        return memberDAO.getActiveIssuedCount(memberId);
    }

    public List<Member> getRecentRegistrations() {
        synchronized (recentRegistrations) {
            return new LinkedList<>(recentRegistrations);
        }
    }
}
