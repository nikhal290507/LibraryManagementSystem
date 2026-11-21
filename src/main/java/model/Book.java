package model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model class representing a Book in the library.
 * Implements Comparable for natural ordering by title (Lectures 16-25 Collections).
 * Implements Serializable for binary file handling (Lectures 49-54).
 */
public class Book implements Comparable<Book>, Serializable {
    private static final long serialVersionUID = 1L;

    private int bookId;
    private String title;
    private Integer authorId;
    private String authorName;
    private Integer categoryId;
    private String categoryName;
    private String isbn;
    private String publisher;
    private int publicationYear;
    private double price;
    private int totalCopies;
    private int availableCopies;

    public Book() {
    }

    public Book(int bookId, String title, Integer authorId, String authorName,
                Integer categoryId, String categoryName, String isbn,
                String publisher, int publicationYear, double price,
                int totalCopies, int availableCopies) {
        this.bookId = bookId;
        this.title = title;
        this.authorId = authorId;
        this.authorName = authorName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.isbn = isbn;
        this.publisher = publisher;
        this.publicationYear = publicationYear;
        this.price = price;
        this.totalCopies = totalCopies;
        this.availableCopies = availableCopies;
    }

    public Book(String title, String authorName, String categoryName, String isbn,
                String publisher, int publicationYear, double price, int totalCopies) {
        this.title = title;
        this.authorName = authorName;
        this.categoryName = categoryName;
        this.isbn = isbn;
        this.publisher = publisher;
        this.publicationYear = publicationYear;
        this.price = price;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    // Natural sorting by title (alphabetical) - Demonstrates Comparable interface
    @Override
    public int compareTo(Book other) {
        if (other == null) return 1;
        int titleCompare = this.title.compareToIgnoreCase(other.title);
        if (titleCompare != 0) {
            return titleCompare;
        }
        return Integer.compare(this.bookId, other.bookId);
    }

    // Getters and Setters
    public int getBookId() {
        return bookId;
    }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Integer authorId) {
        this.authorId = authorId;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public int getPublicationYear() {
        return publicationYear;
    }

    public void setPublicationYear(int publicationYear) {
        this.publicationYear = publicationYear;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Book book = (Book) o;
        return Objects.equals(isbn, book.isbn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(isbn);
    }

    @Override
    public String toString() {
        return String.format("[%d] %s | Author: %s | ISBN: %s | Avail: %d/%d | Price: $%.2f",
                bookId, title, authorName, isbn, availableCopies, totalCopies, price);
    }
}
