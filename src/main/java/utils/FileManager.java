package utils;

import exceptions.DatabaseException;
import exceptions.InvalidInputException;
import model.Book;
import model.IssueRecord;
import model.Member;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * File Handling Utility (Lectures 49-54 File Handling).
 * Demonstrates:
 * - Text File I/O: FileReader, FileWriter, BufferedReader, BufferedWriter
 * - Binary File I/O: FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream
 * - Token-based stream processing: StreamTokenizer
 * - Integration of Collections (ArrayList, HashSet) with File I/O
 */
public class FileManager {

    /**
     * Exports a list of Books to CSV using FileWriter and BufferedWriter.
     */
    public static void exportBooksToCSV(List<Book> books, String filepath) throws IOException {
        File file = new File(filepath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("BookID,Title,Author,Category,ISBN,Publisher,PublicationYear,Price,TotalCopies,AvailableCopies");
            bw.newLine();
            for (Book b : books) {
                String line = String.format("%d,\"%s\",\"%s\",\"%s\",%s,\"%s\",%d,%.2f,%d,%d",
                        b.getBookId(),
                        escapeQuotes(b.getTitle()),
                        escapeQuotes(b.getAuthorName() != null ? b.getAuthorName() : ""),
                        escapeQuotes(b.getCategoryName() != null ? b.getCategoryName() : ""),
                        b.getIsbn(),
                        escapeQuotes(b.getPublisher() != null ? b.getPublisher() : ""),
                        b.getPublicationYear(),
                        b.getPrice(),
                        b.getTotalCopies(),
                        b.getAvailableCopies());
                bw.write(line);
                bw.newLine();
            }
        }
    }

    /**
     * Exports a list of Members to CSV.
     */
    public static void exportMembersToCSV(List<Member> members, String filepath) throws IOException {
        File file = new File(filepath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("MemberID,Name,Email,Phone,Address,RegistrationDate,IsActive,MaxBooksAllowed");
            bw.newLine();
            for (Member m : members) {
                String line = String.format("%d,\"%s\",%s,%s,\"%s\",%s,%s,%d",
                        m.getMemberId(),
                        escapeQuotes(m.getName()),
                        m.getEmail(),
                        m.getPhone(),
                        escapeQuotes(m.getAddress() != null ? m.getAddress() : ""),
                        m.getRegistrationDate(),
                        m.isActive(),
                        m.getMaxBooksAllowed());
                bw.write(line);
                bw.newLine();
            }
        }
    }

    /**
     * Exports transaction history to CSV.
     */
    public static void exportTransactionsToCSV(List<IssueRecord> records, String filepath) throws IOException {
        File file = new File(filepath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("IssueID,MemberID,MemberName,BookID,BookTitle,IssueDate,DueDate,ReturnDate,FineAmount,Status,FinePaid");
            bw.newLine();
            for (IssueRecord r : records) {
                String line = String.format("%d,%d,\"%s\",%d,\"%s\",%s,%s,%s,%.2f,%s,%s",
                        r.getIssueId(),
                        r.getMemberId(),
                        escapeQuotes(r.getMemberName() != null ? r.getMemberName() : ""),
                        r.getBookId(),
                        escapeQuotes(r.getBookTitle() != null ? r.getBookTitle() : ""),
                        r.getIssueDate(),
                        r.getDueDate(),
                        r.getReturnDate() != null ? r.getReturnDate().toString() : "N/A",
                        r.getFineAmount(),
                        r.getStatus(),
                        r.isFinePaid());
                bw.write(line);
                bw.newLine();
            }
        }
    }

    /**
     * Reads a CSV file using FileReader & BufferedReader, validates each row,
     * checks duplicate ISBNs using a HashSet (Lectures 16-25 Collections),
     * and returns an ArrayList of valid Book objects.
     */
    public static List<Book> importBooksFromCSV(String filepath) throws IOException {
        List<Book> validBooks = new ArrayList<>();
        Set<String> seenIsbns = new HashSet<>();
        File file = new File(filepath);

        if (!file.exists()) {
            throw new FileNotFoundException("Import file not found: " + filepath);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String header = br.readLine(); // Skip CSV header
            String line;
            int rowNum = 1;

            while ((line = br.readLine()) != null) {
                rowNum++;
                line = line.trim();
                if (line.isEmpty()) continue;

                // Simple CSV splitter handling quoted values
                String[] tokens = parseCsvLine(line);
                if (tokens.length < 8) {
                    System.err.printf("Warning: Row %d has insufficient columns (%d). Skipped.%n", rowNum, tokens.length);
                    continue;
                }

                try {
                    // Check if first column is numeric Book ID (from export) or Title (from import)
                    int offset = (tokens.length >= 10 && tokens[0].matches("^\\d+$")) ? 1 : 0;

                    String title = InputValidator.validateNonEmpty(tokens[offset], "Title in row " + rowNum);
                    String author = InputValidator.validateNonEmpty(tokens[offset + 1], "Author in row " + rowNum);
                    String category = InputValidator.validateNonEmpty(tokens[offset + 2], "Category in row " + rowNum);
                    String isbn = InputValidator.validateISBN(tokens[offset + 3]);
                    String publisher = tokens[offset + 4].trim();
                    int year = InputValidator.validateYear(Integer.parseInt(tokens[offset + 5].trim()));
                    double price = InputValidator.validateNonNegativeDouble(Double.parseDouble(tokens[offset + 6].trim()), "Price");
                    int copies = InputValidator.validatePositiveInt(Integer.parseInt(tokens[offset + 7].trim()), "Copies");

                    // Check duplicate in file batch using HashSet
                    if (!seenIsbns.add(isbn)) {
                        System.err.printf("Notice: Duplicate ISBN '%s' in CSV row %d. Skipped.%n", isbn, rowNum);
                        continue;
                    }

                    Book book = new Book(title, author, category, isbn, publisher, year, price, copies);
                    validBooks.add(book);
                } catch (Exception e) {
                    System.err.printf("Warning: Skipping invalid row %d [%s]: %s%n", rowNum, line, e.getMessage());
                }
            }
        }
        return validBooks;
    }

    /**
     * Demonstrates StreamTokenizer (Lectures 49-54) for token-based text processing.
     * Counts words, numbers, and lines in a text file.
     */
    public static String analyzeTextFileWithTokenizer(String filepath) throws IOException {
        File file = new File(filepath);
        if (!file.exists()) {
            return "File does not exist: " + filepath;
        }

        int wordCount = 0;
        int numberCount = 0;
        int lineCount = 0;

        try (FileReader fr = new FileReader(file)) {
            StreamTokenizer tokenizer = new StreamTokenizer(fr);
            tokenizer.eolIsSignificant(true);

            int token;
            while ((token = tokenizer.nextToken()) != StreamTokenizer.TT_EOF) {
                if (token == StreamTokenizer.TT_WORD) {
                    wordCount++;
                } else if (token == StreamTokenizer.TT_NUMBER) {
                    numberCount++;
                } else if (token == StreamTokenizer.TT_EOL) {
                    lineCount++;
                }
            }
        }
        return String.format("StreamTokenizer Analysis: %d lines, %d words, %d numeric values found in %s",
                lineCount, wordCount, numberCount, file.getName());
    }

    /**
     * Binary File Writing using FileOutputStream and ObjectOutputStream (Lectures 49-54).
     */
    public static void saveBinaryBackup(Serializable object, String filepath) throws IOException {
        File file = new File(filepath);
        if (file.getParentFile() != null) {
            file.getParentFile().mkdirs();
        }

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(object);
            oos.flush();
        }
    }

    /**
     * Binary File Reading using FileInputStream and ObjectInputStream (Lectures 49-54).
     */
    @SuppressWarnings("unchecked")
    public static <T> T loadBinaryBackup(String filepath) throws IOException, ClassNotFoundException {
        File file = new File(filepath);
        if (!file.exists()) {
            throw new FileNotFoundException("Backup file not found: " + filepath);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (T) ois.readObject();
        }
    }

    /**
     * Appends an activity log message to a text file using FileWriter.
     */
    public static synchronized void appendActivityLog(String message, String filepath) {
        try {
            File file = new File(filepath);
            if (file.getParentFile() != null) file.getParentFile().mkdirs();
            try (FileWriter fw = new FileWriter(file, true);
                 BufferedWriter bw = new BufferedWriter(fw);
                 PrintWriter pw = new PrintWriter(bw)) {
                String timestamp = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                pw.printf("[%s] %s%n", timestamp, message);
            }
        } catch (IOException e) {
            System.err.println("Failed to write to activity log: " + e.getMessage());
        }
    }

    /**
     * Reads and prints a text file content to console.
     */
    public static String readTextFile(String filepath) throws IOException {
        File file = new File(filepath);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + filepath);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    private static String escapeQuotes(String s) {
        return s.replace("\"", "\"\"");
    }

    private static String[] parseCsvLine(String line) {
        List<String> tokens = new ArrayList<>();
        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        tokens.add(sb.toString().trim());
        return tokens.toArray(new String[0]);
    }
}
