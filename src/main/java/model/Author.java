package model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Model class representing an Author.
 */
public class Author implements Serializable {
    private static final long serialVersionUID = 1L;

    private int authorId;
    private String name;
    private String email;
    private String biography;

    public Author() {
    }

    public Author(int authorId, String name, String email, String biography) {
        this.authorId = authorId;
        this.name = name;
        this.email = email;
        this.biography = biography;
    }

    public Author(String name, String email, String biography) {
        this.name = name;
        this.email = email;
        this.biography = biography;
    }

    public int getAuthorId() {
        return authorId;
    }

    public void setAuthorId(int authorId) {
        this.authorId = authorId;
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

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Author author = (Author) o;
        return authorId == author.authorId || Objects.equals(name, author.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(authorId, name);
    }

    @Override
    public String toString() {
        return String.format("[%d] %s (Email: %s)", authorId, name, email != null ? email : "N/A");
    }
}
