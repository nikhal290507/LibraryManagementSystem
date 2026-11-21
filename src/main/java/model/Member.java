package model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Model class representing a Library Member.
 */
public class Member implements Serializable {
    private static final long serialVersionUID = 1L;

    private int memberId;
    private String name;
    private String email;
    private String phone;
    private String address;
    private LocalDate registrationDate;
    private boolean active;
    private int maxBooksAllowed;

    public Member() {
        this.registrationDate = LocalDate.now();
        this.active = true;
        this.maxBooksAllowed = 3;
    }

    public Member(int memberId, String name, String email, String phone,
                  String address, LocalDate registrationDate, boolean active,
                  int maxBooksAllowed) {
        this.memberId = memberId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.registrationDate = registrationDate;
        this.active = active;
        this.maxBooksAllowed = maxBooksAllowed;
    }

    public Member(String name, String email, String phone, String address) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.registrationDate = LocalDate.now();
        this.active = true;
        this.maxBooksAllowed = 3;
    }

    // Getters and Setters
    public int getMemberId() {
        return memberId;
    }

    public void setMemberId(int memberId) {
        this.memberId = memberId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public LocalDate getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(LocalDate registrationDate) {
        this.registrationDate = registrationDate;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public int getMaxBooksAllowed() {
        return maxBooksAllowed;
    }

    public void setMaxBooksAllowed(int maxBooksAllowed) {
        this.maxBooksAllowed = maxBooksAllowed;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Member member = (Member) o;
        return Objects.equals(email, member.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email);
    }

    @Override
    public String toString() {
        return String.format("[%d] %s | Email: %s | Phone: %s | Status: %s",
                memberId, name, email, phone, active ? "ACTIVE" : "INACTIVE");
    }
}
