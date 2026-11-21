import exceptions.InvalidInputException;
import exceptions.MemberNotFoundException;
import model.Member;
import org.junit.jupiter.api.*;
import service.MemberService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MemberServiceTest {

    private static MemberService memberService;

    @BeforeAll
    public static void setUp() {
        memberService = new MemberService();
    }

    @Test
    @Order(1)
    public void testRegisterMemberSuccessfully() throws Exception {
        String testEmail = "test.member" + System.currentTimeMillis() + "@university.edu";
        Member m = memberService.registerMember("John Doe", testEmail, "9876543210", "Dorm Room 101");

        assertNotNull(m);
        assertTrue(m.getMemberId() > 0);
        assertEquals("John Doe", m.getName());
        assertEquals(testEmail, m.getEmail());
        assertTrue(m.isActive());
    }

    @Test
    @Order(2)
    public void testDuplicateEmailThrowsException() throws Exception {
        String uniqueEmail = "dup.check" + System.currentTimeMillis() + "@university.edu";
        memberService.registerMember("First Student", uniqueEmail, "9876543211", "Hall A");

        assertThrows(InvalidInputException.class, () -> {
            memberService.registerMember("Second Student", uniqueEmail, "9876543212", "Hall B");
        });
    }

    @Test
    @Order(3)
    public void testInvalidEmailAndPhoneFormats() {
        // Bad email format
        assertThrows(InvalidInputException.class, () -> {
            memberService.registerMember("Bad Email", "not-an-email", "9876543210", "Hall C");
        });

        // Bad phone format
        assertThrows(InvalidInputException.class, () -> {
            memberService.registerMember("Bad Phone", "valid@uni.edu", "123", "Hall C");
        });

        // Empty name
        assertThrows(InvalidInputException.class, () -> {
            memberService.registerMember("   ", "valid2@uni.edu", "9876543210", "Hall C");
        });
    }

    @Test
    @Order(4)
    public void testMemberDeactivateAndActivate() throws Exception {
        String email = "status.test" + System.currentTimeMillis() + "@university.edu";
        Member m = memberService.registerMember("Status Test", email, "9876543215", "Hall D");

        memberService.deactivateMember(m.getMemberId());
        Member updated = memberService.getMemberById(m.getMemberId());
        assertFalse(updated.isActive());

        memberService.activateMember(m.getMemberId());
        Member reactivated = memberService.getMemberById(m.getMemberId());
        assertTrue(reactivated.isActive());
    }

    @Test
    @Order(5)
    public void testMemberNotFoundThrowsException() {
        assertThrows(MemberNotFoundException.class, () -> {
            memberService.getMemberById(888888);
        });
    }
}
