import model.Book;
import org.junit.jupiter.api.Test;
import utils.FileManager;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class FileHandlingTest {

    @Test
    public void testCsvExportAndImport() throws Exception {
        List<Book> testBooks = new ArrayList<>();
        testBooks.add(new Book("Test Book 1", "Author One", "Science", "978-1111111110", "Pub A", 2021, 25.0, 3));
        testBooks.add(new Book("Test Book 2", "Author Two", "Math", "978-2222222220", "Pub B", 2022, 35.0, 4));

        String testCsvPath = "exports/test_books.csv";
        FileManager.exportBooksToCSV(testBooks, testCsvPath);

        File exported = new File(testCsvPath);
        assertTrue(exported.exists());
        assertTrue(exported.length() > 0);

        List<Book> imported = FileManager.importBooksFromCSV(testCsvPath);
        assertEquals(2, imported.size());
        assertEquals("Test Book 1", imported.get(0).getTitle());
        assertEquals("978-1111111110", imported.get(0).getIsbn());
    }

    @Test
    public void testBinaryBackupAndRestore() throws Exception {
        ArrayList<Book> list = new ArrayList<>();
        list.add(new Book("Binary Book", "Binary Author", "Tech", "978-3333333330", "BinPub", 2023, 49.99, 2));

        String binPath = "exports/test_binary.dat";
        FileManager.saveBinaryBackup(list, binPath);

        File binFile = new File(binPath);
        assertTrue(binFile.exists());

        ArrayList<Book> restored = FileManager.loadBinaryBackup(binPath);
        assertNotNull(restored);
        assertEquals(1, restored.size());
        assertEquals("Binary Book", restored.get(0).getTitle());
    }

    @Test
    public void testStreamTokenizerAnalysis() throws Exception {
        String analysis = FileManager.analyzeTextFileWithTokenizer("src/main/resources/database.sql");
        assertNotNull(analysis);
        assertTrue(analysis.contains("StreamTokenizer Analysis:"));
    }
}
