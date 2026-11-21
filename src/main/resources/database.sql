-- =====================================================================
-- Library Management System Database Schema (MySQL 8.0+)
-- Database: library_management
-- =====================================================================

CREATE DATABASE IF NOT EXISTS library_management;
USE library_management;

-- 1. Authors Table
CREATE TABLE IF NOT EXISTS authors (
    author_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    biography VARCHAR(255)
);

-- 2. Categories Table
CREATE TABLE IF NOT EXISTS categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- 3. Books Table
CREATE TABLE IF NOT EXISTS books (
    book_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    author_id INT,
    author_name VARCHAR(100),
    category_id INT,
    category_name VARCHAR(100),
    isbn VARCHAR(20) NOT NULL UNIQUE,
    publisher VARCHAR(100),
    publication_year INT DEFAULT 2024,
    price DECIMAL(10, 2) DEFAULT 0.00,
    total_copies INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_book_author FOREIGN KEY (author_id) REFERENCES authors(author_id) ON DELETE SET NULL,
    CONSTRAINT fk_book_category FOREIGN KEY (category_id) REFERENCES categories(category_id) ON DELETE SET NULL
);

-- 4. Members Table
CREATE TABLE IF NOT EXISTS members (
    member_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(255),
    registration_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    max_books_allowed INT DEFAULT 3
);

-- 5. Issue Records Table
CREATE TABLE IF NOT EXISTS issue_records (
    issue_id INT AUTO_INCREMENT PRIMARY KEY,
    member_id INT NOT NULL,
    book_id INT NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    return_date DATE,
    fine_amount DECIMAL(10, 2) DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'ISSUED',
    fine_paid BOOLEAN DEFAULT FALSE,
    CONSTRAINT fk_issue_member FOREIGN KEY (member_id) REFERENCES members(member_id),
    CONSTRAINT fk_issue_book FOREIGN KEY (book_id) REFERENCES books(book_id)
);

-- =====================================================================
-- Sample Seed Data
-- =====================================================================

-- Authors
INSERT INTO authors (name, email, biography) VALUES 
('James Gosling', 'james@sun.com', 'Father of the Java programming language'),
('Joshua Bloch', 'joshua@effectivejava.com', 'Chief Java Architect, Author of Effective Java'),
('Robert C. Martin', 'unclebob@cleancoder.com', 'Author of Clean Code, Agile Craftsman'),
('Martin Fowler', 'fowler@martinfowler.com', 'Author of Refactoring, Architecture Expert'),
('Erich Gamma', 'gamma@designpatterns.com', 'Co-author of Design Patterns (GoF)')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Categories
INSERT INTO categories (name, description) VALUES
('Computer Science', 'Programming, Algorithms, Data Structures, and Software Engineering'),
('Mathematics', 'Calculus, Linear Algebra, Probability and Discrete Math'),
('Physics', 'Classical Mechanics, Quantum Physics, Relativity'),
('Literature', 'Classic and Modern Fiction, Essays, Poetry'),
('Philosophy', 'Logic, Epistemology, Ethics and History of Thought')
ON DUPLICATE KEY UPDATE name=VALUES(name);

-- Books
INSERT INTO books (title, author_id, author_name, category_id, category_name, isbn, publisher, publication_year, price, total_copies, available_copies) VALUES
('Effective Java', 2, 'Joshua Bloch', 1, 'Computer Science', '978-0134685991', 'Addison-Wesley', 2018, 55.00, 5, 5),
('Clean Code', 3, 'Robert C. Martin', 1, 'Computer Science', '978-0132350884', 'Prentice Hall', 2008, 48.00, 4, 4),
('Design Patterns', 5, 'Erich Gamma', 1, 'Computer Science', '978-0201633610', 'Addison-Wesley', 1994, 60.00, 3, 3),
('The Java Programming Language', 1, 'James Gosling', 1, 'Computer Science', '978-0321349804', 'Addison-Wesley', 2005, 45.00, 2, 2),
('Refactoring', 4, 'Martin Fowler', 1, 'Computer Science', '978-0201485677', 'Addison-Wesley', 1999, 52.00, 4, 4)
ON DUPLICATE KEY UPDATE title=VALUES(title);

-- Members
INSERT INTO members (name, email, phone, address, registration_date, is_active, max_books_allowed) VALUES
('Alice Johnson', 'alice.j@university.edu', '9876543210', '101 Campus Hostel A', '2024-01-15', TRUE, 3),
('Bob Smith', 'bob.smith@university.edu', '9876543211', '204 Green Avenue', '2024-02-01', TRUE, 3),
('Charlie Brown', 'charlie.b@university.edu', '9876543212', '12 River Side Apt', '2024-02-20', TRUE, 3),
('Diana Prince', 'diana.p@university.edu', '9876543213', '405 Science Hall', '2024-03-01', TRUE, 3),
('Evan Wright', 'evan.w@university.edu', '9876543214', '77 Oak Lane', '2024-03-10', FALSE, 3)
ON DUPLICATE KEY UPDATE name=VALUES(name);
