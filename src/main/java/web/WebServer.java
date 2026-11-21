package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import dao.AuthorDAO;
import dao.CategoryDAO;
import database.DBConnection;
import exceptions.BookUnavailableException;
import exceptions.DuplicateISBNException;
import exceptions.InvalidInputException;
import model.Book;
import model.IssueRecord;
import model.Member;
import service.BookService;
import service.LibraryService;
import service.MemberService;
import threads.ConcurrencyIssueDemo;
import threads.NotificationConsumer;
import threads.NotificationProducer;
import threads.NotificationQueue;
import utils.ReportGenerator;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Embedded Web Server for Live Browser Demonstration.
 * Uses 100% pure standard Java SE HTTP Server (com.sun.net.httpserver.HttpServer).
 * Zero external frameworks, zero Spring Boot.
 * Connects directly to the existing Service, DAO, and Thread layers.
 */
public class WebServer {

    private static final int PORT = 8080;
    private static BookService bookService;
    private static MemberService memberService;
    private static LibraryService libraryService;

    public static void main(String[] args) throws IOException {
        bookService = new BookService();
        memberService = new MemberService();
        libraryService = new LibraryService();

        HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
        server.setExecutor(Executors.newFixedThreadPool(10));

        // Routes
        server.createContext("/", new StaticFileHandler());
        server.createContext("/api/dashboard", new DashboardApiHandler());
        server.createContext("/api/books", new BooksApiHandler());
        server.createContext("/api/members", new MembersApiHandler());
        server.createContext("/api/transactions", new TransactionsApiHandler());
        server.createContext("/api/issue", new IssueApiHandler());
        server.createContext("/api/return", new ReturnApiHandler());
        server.createContext("/api/fines/pay", new PayFineApiHandler());
        server.createContext("/api/report", new ReportApiHandler());
        server.createContext("/api/threads/producer-consumer", new ProducerConsumerApiHandler());

        server.start();
        System.out.println("=========================================================================");
        System.out.printf("  >>> Library Management System LIVE on Browser: http://localhost:%d <<<\n", PORT);
        System.out.println("  Press Ctrl+C to terminate the server.");
        System.out.println("=========================================================================");
    }

    // =========================================================================
    // STATIC HTML DASHBOARD HANDLER
    // =========================================================================
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                return;
            }
            String html = getIndexHtml();
            sendResponse(exchange, 200, html, "text/html; charset=UTF-8");
        }
    }

    // =========================================================================
    // REST API HANDLERS
    // =========================================================================
    static class DashboardApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, Object> stats = libraryService.getDashboardStats();
                Deque<String> recent = libraryService.getRecentActions();

                StringBuilder json = new StringBuilder("{");
                json.append("\"stats\":{");
                int count = 0;
                for (Map.Entry<String, Object> e : stats.entrySet()) {
                    if (count++ > 0) json.append(",");
                    json.append("\"").append(escapeJson(e.getKey())).append("\":\"").append(escapeJson(String.valueOf(e.getValue()))).append("\"");
                }
                json.append("},\"recentActions\":[");
                int aCount = 0;
                for (String act : recent) {
                    if (aCount++ > 0) json.append(",");
                    json.append("\"").append(escapeJson(act)).append("\"");
                }
                json.append("]}");

                sendResponse(exchange, 200, json.toString(), "application/json");
            } catch (Exception e) {
                sendErrorResponse(exchange, 500, e.getMessage());
            }
        }
    }

    static class BooksApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    List<Book> books = bookService.getAllBooks();
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < books.size(); i++) {
                        if (i > 0) json.append(",");
                        Book b = books.get(i);
                        json.append(String.format("{\"id\":%d,\"title\":\"%s\",\"author\":\"%s\",\"category\":\"%s\",\"isbn\":\"%s\",\"publisher\":\"%s\",\"year\":%d,\"price\":%.2f,\"totalCopies\":%d,\"availableCopies\":%d}",
                                b.getBookId(), escapeJson(b.getTitle()), escapeJson(b.getAuthorName() != null ? b.getAuthorName() : ""),
                                escapeJson(b.getCategoryName() != null ? b.getCategoryName() : ""),
                                escapeJson(b.getIsbn()), escapeJson(b.getPublisher() != null ? b.getPublisher() : ""),
                                b.getPublicationYear(), b.getPrice(), b.getTotalCopies(), b.getAvailableCopies()));
                    }
                    json.append("]");
                    sendResponse(exchange, 200, json.toString(), "application/json");
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map<String, String> params = parseFormBody(exchange);
                    String action = params.getOrDefault("action", "add");
                    if ("delete".equalsIgnoreCase(action)) {
                        int id = Integer.parseInt(params.get("id"));
                        bookService.deleteBook(id);
                        sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Book deleted successfully\"}", "application/json");
                    } else {
                        String title = params.get("title");
                        String author = params.get("author");
                        String category = params.get("category");
                        String isbn = params.get("isbn");
                        String publisher = params.get("publisher");
                        int year = Integer.parseInt(params.get("year"));
                        double price = Double.parseDouble(params.get("price"));
                        int copies = Integer.parseInt(params.get("copies"));

                        Book added = bookService.addBook(title, author, category, isbn, publisher, year, price, copies);
                        sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Book added with ID #" + added.getBookId() + "\"}", "application/json");
                    }
                }
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
        }
    }

    static class MembersApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                    List<Member> members = memberService.getAllMembers();
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < members.size(); i++) {
                        if (i > 0) json.append(",");
                        Member m = members.get(i);
                        int activeLoans = memberService.getActiveIssuedCount(m.getMemberId());
                        json.append(String.format("{\"id\":%d,\"name\":\"%s\",\"email\":\"%s\",\"phone\":\"%s\",\"address\":\"%s\",\"date\":\"%s\",\"active\":%s,\"activeLoans\":%d,\"maxAllowed\":%d}",
                                m.getMemberId(), escapeJson(m.getName()), escapeJson(m.getEmail()),
                                escapeJson(m.getPhone()), escapeJson(m.getAddress() != null ? m.getAddress() : ""),
                                m.getRegistrationDate(), m.isActive(), activeLoans, m.getMaxBooksAllowed()));
                    }
                    json.append("]");
                    sendResponse(exchange, 200, json.toString(), "application/json");
                } else if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    Map<String, String> params = parseFormBody(exchange);
                    String action = params.getOrDefault("action", "register");
                    if ("toggle".equalsIgnoreCase(action)) {
                        int id = Integer.parseInt(params.get("id"));
                        Member m = memberService.getMemberById(id);
                        if (m.isActive()) memberService.deactivateMember(id); else memberService.activateMember(id);
                        sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Member status updated\"}", "application/json");
                    } else {
                        String name = params.get("name");
                        String email = params.get("email");
                        String phone = params.get("phone");
                        String address = params.get("address");

                        Member m = memberService.registerMember(name, email, phone, address);
                        sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Member registered with ID #" + m.getMemberId() + "\"}", "application/json");
                    }
                }
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
        }
    }

    static class TransactionsApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<IssueRecord> list = libraryService.getAllTransactions();
                StringBuilder json = new StringBuilder("[");
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) json.append(",");
                    IssueRecord r = list.get(i);
                    json.append(String.format("{\"id\":%d,\"memberId\":%d,\"memberName\":\"%s\",\"bookId\":%d,\"bookTitle\":\"%s\",\"issueDate\":\"%s\",\"dueDate\":\"%s\",\"returnDate\":\"%s\",\"fine\":%.2f,\"status\":\"%s\",\"finePaid\":%s}",
                            r.getIssueId(), r.getMemberId(), escapeJson(r.getMemberName() != null ? r.getMemberName() : "N/A"),
                            r.getBookId(), escapeJson(r.getBookTitle() != null ? r.getBookTitle() : "N/A"),
                            r.getIssueDate(), r.getDueDate(), r.getReturnDate() != null ? r.getReturnDate().toString() : "ACTIVE",
                            r.getFineAmount(), r.getStatus(), r.isFinePaid()));
                }
                json.append("]");
                sendResponse(exchange, 200, json.toString(), "application/json");
            } catch (Exception e) {
                sendErrorResponse(exchange, 500, e.getMessage());
            }
        }
    }

    static class IssueApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                    return;
                }
                Map<String, String> params = parseFormBody(exchange);
                int memberId = Integer.parseInt(params.get("memberId"));
                int bookId = Integer.parseInt(params.get("bookId"));

                IssueRecord issue = libraryService.issueBook(memberId, bookId);
                sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Book issued successfully! (Issue #" + issue.getIssueId() + ", Due: " + issue.getDueDate() + ")\"}", "application/json");
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
        }
    }

    static class ReturnApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    sendResponse(exchange, 405, "Method Not Allowed", "text/plain");
                    return;
                }
                Map<String, String> params = parseFormBody(exchange);
                int issueId = Integer.parseInt(params.get("issueId"));
                boolean payNow = Boolean.parseBoolean(params.getOrDefault("payNow", "true"));

                IssueRecord record = libraryService.returnBook(issueId, payNow);
                String msg = String.format("Book returned successfully! Accrued Fine: $%.2f (Paid: %s)",
                        record.getFineAmount(), record.isFinePaid() ? "YES" : "NO");
                sendResponse(exchange, 200, "{\"success\":true,\"message\":\"" + escapeJson(msg) + "\"}", "application/json");
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
        }
    }

    static class PayFineApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                Map<String, String> params = parseFormBody(exchange);
                int issueId = Integer.parseInt(params.get("issueId"));
                libraryService.payFine(issueId);
                sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Fine paid successfully!\"}", "application/json");
            } catch (Exception e) {
                sendErrorResponse(exchange, 400, e.getMessage());
            }
        }
    }

    static class ReportApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                List<Book> books = bookService.getAllBooks();
                List<Member> members = memberService.getAllMembers();
                List<IssueRecord> issues = libraryService.getAllTransactions();
                Map<String, Object> stats = libraryService.getDashboardStats();

                double coll = Double.parseDouble((String) stats.get("Collected Fines ($)"));
                double pend = Double.parseDouble((String) stats.get("Pending Fines ($)"));

                String report = ReportGenerator.generateComprehensiveReport(books, members, issues, coll, pend);
                sendResponse(exchange, 200, "{\"report\":\"" + escapeJson(report) + "\"}", "application/json");
            } catch (Exception e) {
                sendErrorResponse(exchange, 500, e.getMessage());
            }
        }
    }

    static class ProducerConsumerApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                NotificationQueue queue = new NotificationQueue(3);
                List<String> logMessages = new ArrayList<>();

                String[] sampleAlerts = {
                        "Book 'Effective Java' borrowed by Alice",
                        "Book 'Clean Code' returned by Bob",
                        "Overdue fine calculated for Issue #4",
                        "New member 'Charlie' registered"
                };

                Thread prod = new Thread(new NotificationProducer(queue, sampleAlerts, 50));
                Thread cons = new Thread(new NotificationConsumer(queue, sampleAlerts.length, 80));

                prod.start();
                cons.start();
                prod.join();
                cons.join();

                sendResponse(exchange, 200, "{\"success\":true,\"message\":\"Multithreading Producer-Consumer simulation executed successfully with wait() and notifyAll()!\"}", "application/json");
            } catch (Exception e) {
                sendErrorResponse(exchange, 500, e.getMessage());
            }
        }
    }

    // =========================================================================
    // UTILITY METHODS
    // =========================================================================
    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendErrorResponse(HttpExchange exchange, int statusCode, String errorMsg) throws IOException {
        String json = "{\"success\":false,\"error\":\"" + escapeJson(errorMsg) + "\"}";
        sendResponse(exchange, statusCode, json, "application/json");
    }

    private static Map<String, String> parseFormBody(HttpExchange exchange) throws IOException {
        Map<String, String> map = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line);
            String body = sb.toString();
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    String k = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String v = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    map.put(k, v);
                }
            }
        }
        return map;
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\b", "\\b")
                .replace("\f", "\\f")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // =========================================================================
    // EMBEDDED SINGLE-PAGE WEB DASHBOARD (HTML, CSS & JS)
    // =========================================================================
    private static String getIndexHtml() {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\">\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "  <title>Library Management System - University Project</title>\n" +
                "  <link rel=\"stylesheet\" href=\"https://fonts.googleapis.com/css2?family=Inter:wght@300;400;500;600;700&display=swap\">\n" +
                "  <style>\n" +
                "    :root {\n" +
                "      --bg-dark: #0f172a;\n" +
                "      --sidebar-bg: #1e293b;\n" +
                "      --sidebar-hover: #334155;\n" +
                "      --primary: #2563eb;\n" +
                "      --primary-hover: #1d4ed8;\n" +
                "      --accent: #38bdf8;\n" +
                "      --bg-light: #f8fafc;\n" +
                "      --card-bg: #ffffff;\n" +
                "      --border: #e2e8f0;\n" +
                "      --text-main: #1e293b;\n" +
                "      --text-muted: #64748b;\n" +
                "      --success: #10b981;\n" +
                "      --danger: #ef4444;\n" +
                "      --warning: #f59e0b;\n" +
                "    }\n" +
                "    * { box-sizing: border-box; margin: 0; padding: 0; font-family: 'Inter', sans-serif; }\n" +
                "    body { display: flex; height: 100vh; background-color: var(--bg-light); color: var(--text-main); overflow: hidden; }\n" +
                "    #sidebar {\n" +
                "      width: 260px; background-color: var(--sidebar-bg); color: #fff;\n" +
                "      display: flex; flex-direction: column; flex-shrink: 0; z-index: 10;\n" +
                "    }\n" +
                "    .brand {\n" +
                "      padding: 24px 20px; font-size: 1.15rem; font-weight: 700; color: #fff;\n" +
                "      border-bottom: 1px solid rgba(255,255,255,0.08); display: flex; align-items: center; gap: 10px;\n" +
                "    }\n" +
                "    .brand span { color: var(--accent); }\n" +
                "    .nav-list { list-style: none; padding: 15px 10px; flex: 1; overflow-y: auto; }\n" +
                "    .nav-item {\n" +
                "      padding: 12px 16px; margin-bottom: 4px; border-radius: 8px; cursor: pointer;\n" +
                "      font-size: 0.92rem; font-weight: 500; display: flex; align-items: center; gap: 12px;\n" +
                "      color: #94a3b8; transition: all 0.2s;\n" +
                "    }\n" +
                "    .nav-item:hover { background-color: var(--sidebar-hover); color: #fff; }\n" +
                "    .nav-item.active { background-color: var(--primary); color: #fff; font-weight: 600; }\n" +
                "    .badge-status {\n" +
                "      margin: 15px; padding: 12px; background: rgba(56,189,248,0.1); border-radius: 8px;\n" +
                "      border: 1px solid rgba(56,189,248,0.2); font-size: 0.8rem; color: #bae6fd;\n" +
                "    }\n" +
                "    #main-content { flex: 1; display: flex; flex-direction: column; overflow: hidden; }\n" +
                "    header {\n" +
                "      height: 70px; background-color: #fff; border-bottom: 1px solid var(--border);\n" +
                "      display: flex; align-items: center; justify-content: space-between; padding: 0 30px;\n" +
                "    }\n" +
                "    .header-title { font-size: 1.35rem; font-weight: 700; }\n" +
                "    .content-area { flex: 1; padding: 30px; overflow-y: auto; }\n" +
                "    .tab-view { display: none; }\n" +
                "    .tab-view.active { display: block; animation: fadeIn 0.2s ease-in-out; }\n" +
                "    @keyframes fadeIn { from { opacity: 0; transform: translateY(6px); } to { opacity: 1; transform: translateY(0); } }\n" +
                "    .grid-stats {\n" +
                "      display: grid; grid-template-columns: repeat(auto-fit, minmax(210px, 1fr)); gap: 20px; margin-bottom: 25px;\n" +
                "    }\n" +
                "    .card {\n" +
                "      background-color: var(--card-bg); padding: 22px; border-radius: 12px;\n" +
                "      border: 1px solid var(--border); box-shadow: 0 2px 4px rgba(0,0,0,0.02);\n" +
                "    }\n" +
                "    .stat-card .label { font-size: 0.82rem; font-weight: 600; text-transform: uppercase; color: var(--text-muted); }\n" +
                "    .stat-card .value { font-size: 1.85rem; font-weight: 700; margin-top: 8px; color: var(--text-main); }\n" +
                "    .card-title { font-size: 1.1rem; font-weight: 700; margin-bottom: 15px; display: flex; justify-content: space-between; align-items: center; }\n" +
                "    .table-container { overflow-x: auto; }\n" +
                "    table { width: 100%; border-collapse: collapse; text-align: left; font-size: 0.9rem; }\n" +
                "    th { background-color: #f1f5f9; padding: 12px 14px; font-weight: 600; color: var(--text-muted); border-bottom: 1px solid var(--border); }\n" +
                "    td { padding: 12px 14px; border-bottom: 1px solid var(--border); }\n" +
                "    tr:hover td { background-color: #f8fafc; }\n" +
                "    .btn {\n" +
                "      padding: 9px 16px; border-radius: 8px; border: none; font-size: 0.88rem; font-weight: 600;\n" +
                "      cursor: pointer; transition: background 0.2s; display: inline-flex; align-items: center; gap: 6px;\n" +
                "    }\n" +
                "    .btn-primary { background-color: var(--primary); color: #fff; }\n" +
                "    .btn-primary:hover { background-color: var(--primary-hover); }\n" +
                "    .btn-danger { background-color: var(--danger); color: #fff; }\n" +
                "    .btn-danger:hover { background-color: #dc2626; }\n" +
                "    .btn-success { background-color: var(--success); color: #fff; }\n" +
                "    .badge {\n" +
                "      padding: 4px 10px; border-radius: 999px; font-size: 0.75rem; font-weight: 600; display: inline-block;\n" +
                "    }\n" +
                "    .badge-avail { background-color: #dcfce7; color: #15803d; }\n" +
                "    .badge-out { background-color: #fee2e2; color: #b91c1c; }\n" +
                "    .badge-active { background-color: #e0f2fe; color: #0369a1; }\n" +
                "    .badge-overdue { background-color: #fef3c7; color: #b45309; }\n" +
                "    .form-group { margin-bottom: 16px; }\n" +
                "    .form-group label { display: block; font-size: 0.85rem; font-weight: 600; margin-bottom: 6px; }\n" +
                "    .form-control {\n" +
                "      width: 100%; padding: 10px 14px; border-radius: 8px; border: 1px solid var(--border);\n" +
                "      font-size: 0.9rem; outline: none; transition: border 0.2s;\n" +
                "    }\n" +
                "    .form-control:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(37,99,235,0.15); }\n" +
                "    .modal-overlay {\n" +
                "      position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15,23,42,0.6);\n" +
                "      display: none; align-items: center; justify-content: center; z-index: 100;\n" +
                "    }\n" +
                "    .modal-overlay.active { display: flex; }\n" +
                "    .modal-box {\n" +
                "      background: #fff; width: 480px; max-width: 90%; border-radius: 12px; padding: 25px;\n" +
                "      box-shadow: 0 20px 25px -5px rgba(0,0,0,0.1);\n" +
                "    }\n" +
                "    .pre-report {\n" +
                "      background-color: #0f172a; color: #f8fafc; padding: 20px; border-radius: 8px;\n" +
                "      font-family: monospace; font-size: 0.85rem; line-height: 1.5; max-height: 480px; overflow-y: auto;\n" +
                "    }\n" +
                "    .alert-banner {\n" +
                "      padding: 12px 18px; border-radius: 8px; margin-bottom: 18px; font-size: 0.9rem; display: none;\n" +
                "    }\n" +
                "    .alert-success { background: #dcfce7; color: #166534; border: 1px solid #bbf7d0; }\n" +
                "    .alert-error { background: #fee2e2; color: #991b1b; border: 1px solid #fecaca; }\n" +
                "  </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "  <aside id=\"sidebar\">\n" +
                "    <div class=\"brand\">📚 Library<span>System</span></div>\n" +
                "    <ul class=\"nav-list\">\n" +
                "      <li class=\"nav-item active\" onclick=\"showTab('dashboard')\">📊 Dashboard</li>\n" +
                "      <li class=\"nav-item\" onclick=\"showTab('books')\">📖 Book Catalog</li>\n" +
                "      <li class=\"nav-item\" onclick=\"showTab('members')\">👥 Members</li>\n" +
                "      <li class=\"nav-item\" onclick=\"showTab('issues')\">🔄 Issue & Return</li>\n" +
                "      <li class=\"nav-item\" onclick=\"showTab('transactions')\">📜 Transactions & Fines</li>\n" +
                "      <li class=\"nav-item\" onclick=\"showTab('reports')\">📈 Analytics Report</li>\n" +
                "      <li class=\"nav-item\" onclick=\"showTab('threads')\">⚡ Multithreading Demos</li>\n" +
                "    </ul>\n" +
                "    <div class=\"badge-status\" id=\"db-badge\">Database: Connected</div>\n" +
                "  </aside>\n" +
                "  <main id=\"main-content\">\n" +
                "    <header>\n" +
                "      <h2 class=\"header-title\" id=\"page-title\">Library Dashboard</h2>\n" +
                "      <div><button class=\"btn btn-primary\" onclick=\"refreshCurrentTab()\">⟳ Refresh</button></div>\n" +
                "    </header>\n" +
                "    <div class=\"content-area\">\n" +
                "      <div id=\"alert-box\" class=\"alert-banner\"></div>\n" +
                "      <!-- DASHBOARD TAB -->\n" +
                "      <div id=\"tab-dashboard\" class=\"tab-view active\">\n" +
                "        <div class=\"grid-stats\" id=\"stats-grid\"></div>\n" +
                "        <div class=\"card\">\n" +
                "          <h3 class=\"card-title\">Recent Activity Log (Live FIFO Tracking)</h3>\n" +
                "          <ul id=\"recent-actions-list\" style=\"list-style: none; font-size: 0.9rem;\"></ul>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      <!-- BOOKS TAB -->\n" +
                "      <div id=\"tab-books\" class=\"tab-view\">\n" +
                "        <div class=\"card\">\n" +
                "          <div class=\"card-title\">\n" +
                "            <span>Book Inventory</span>\n" +
                "            <button class=\"btn btn-primary\" onclick=\"openAddBookModal()\">+ Add New Book</button>\n" +
                "          </div>\n" +
                "          <div style=\"margin-bottom: 15px;\">\n" +
                "            <input type=\"text\" id=\"book-search\" class=\"form-control\" placeholder=\"Search books by title, author, or ISBN...\" onkeyup=\"filterBooks()\">\n" +
                "          </div>\n" +
                "          <div class=\"table-container\">\n" +
                "            <table>\n" +
                "              <thead>\n" +
                "                <tr><th>ID</th><th>Title</th><th>Author</th><th>Category</th><th>ISBN</th><th>Price</th><th>Availability</th><th>Actions</th></tr>\n" +
                "              </thead>\n" +
                "              <tbody id=\"books-table-body\"></tbody>\n" +
                "            </table>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      <!-- MEMBERS TAB -->\n" +
                "      <div id=\"tab-members\" class=\"tab-view\">\n" +
                "        <div class=\"card\">\n" +
                "          <div class=\"card-title\">\n" +
                "            <span>Registered Members</span>\n" +
                "            <button class=\"btn btn-primary\" onclick=\"openRegisterMemberModal()\">+ Register Member</button>\n" +
                "          </div>\n" +
                "          <div class=\"table-container\">\n" +
                "            <table>\n" +
                "              <thead>\n" +
                "                <tr><th>ID</th><th>Name</th><th>Email</th><th>Phone</th><th>Registered</th><th>Status</th><th>Active Loans</th><th>Actions</th></tr>\n" +
                "              </thead>\n" +
                "              <tbody id=\"members-table-body\"></tbody>\n" +
                "            </table>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      <!-- ISSUE & RETURN TAB -->\n" +
                "      <div id=\"tab-issues\" class=\"tab-view\">\n" +
                "        <div style=\"display: grid; grid-template-columns: 1fr 1fr; gap: 20px;\">\n" +
                "          <div class=\"card\">\n" +
                "            <h3 class=\"card-title\">Issue Book Transaction</h3>\n" +
                "            <form onsubmit=\"submitIssueBook(event)\">\n" +
                "              <div class=\"form-group\">\n" +
                "                <label>Member ID</label>\n" +
                "                <input type=\"number\" id=\"issue-member-id\" class=\"form-control\" required placeholder=\"e.g. 1\">\n" +
                "              </div>\n" +
                "              <div class=\"form-group\">\n" +
                "                <label>Book ID</label>\n" +
                "                <input type=\"number\" id=\"issue-book-id\" class=\"form-control\" required placeholder=\"e.g. 1\">\n" +
                "              </div>\n" +
                "              <button type=\"submit\" class=\"btn btn-primary\" style=\"width:100%;\">Confirm Issue (Atomic ACID Transaction)</button>\n" +
                "            </form>\n" +
                "          </div>\n" +
                "          <div class=\"card\">\n" +
                "            <h3 class=\"card-title\">Return Book Transaction</h3>\n" +
                "            <form onsubmit=\"submitReturnBook(event)\">\n" +
                "              <div class=\"form-group\">\n" +
                "                <label>Issue ID</label>\n" +
                "                <input type=\"number\" id=\"return-issue-id\" class=\"form-control\" required placeholder=\"e.g. 1\">\n" +
                "              </div>\n" +
                "              <div class=\"form-group\" style=\"margin-top: 20px;\">\n" +
                "                <label><input type=\"checkbox\" id=\"return-pay-now\" checked> Collect Accrued Late Fines Immediately ($5.00/day)</label>\n" +
                "              </div>\n" +
                "              <button type=\"submit\" class=\"btn btn-success\" style=\"width:100%; margin-top: 15px;\">Confirm Return & Restock</button>\n" +
                "            </form>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      <!-- TRANSACTIONS & FINES TAB -->\n" +
                "      <div id=\"tab-transactions\" class=\"tab-view\">\n" +
                "        <div class=\"card\">\n" +
                "          <h3 class=\"card-title\">Transaction History & Fines</h3>\n" +
                "          <div class=\"table-container\">\n" +
                "            <table>\n" +
                "              <thead>\n" +
                "                <tr><th>Issue#</th><th>Member</th><th>Book Title</th><th>Issued</th><th>Due Date</th><th>Returned</th><th>Fine</th><th>Status</th><th>Action</th></tr>\n" +
                "              </thead>\n" +
                "              <tbody id=\"transactions-table-body\"></tbody>\n" +
                "            </table>\n" +
                "          </div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      <!-- AUDIT REPORT TAB -->\n" +
                "      <div id=\"tab-reports\" class=\"tab-view\">\n" +
                "        <div class=\"card\">\n" +
                "          <div class=\"card-title\">\n" +
                "            <span>Comprehensive Analytics Audit Report (Java Stream API)</span>\n" +
                "            <button class=\"btn btn-primary\" onclick=\"loadAuditReport()\">Regenerate</button>\n" +
                "          </div>\n" +
                "          <pre id=\"report-output\" class=\"pre-report\">Loading report...</pre>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "      <!-- MULTITHREADING DEMOS TAB -->\n" +
                "      <div id=\"tab-threads\" class=\"tab-view\">\n" +
                "        <div class=\"card\" style=\"margin-bottom: 20px;\">\n" +
                "          <h3 class=\"card-title\">Inter-Thread Communication: Producer-Consumer Simulation</h3>\n" +
                "          <p style=\"color: var(--text-muted); font-size: 0.9rem; margin-bottom: 15px;\">Executes NotificationProducer and NotificationConsumer using a bounded queue with wait() and notifyAll() synchronization.</p>\n" +
                "          <button class=\"btn btn-primary\" onclick=\"runProducerConsumer()\">Run Producer-Consumer Simulation</button>\n" +
                "          <div id=\"thread-status\" style=\"margin-top: 15px; font-size: 0.9rem; font-weight: 600;\"></div>\n" +
                "        </div>\n" +
                "      </div>\n" +
                "    </div>\n" +
                "  </main>\n" +
                "  <!-- ADD BOOK MODAL -->\n" +
                "  <div id=\"modal-add-book\" class=\"modal-overlay\">\n" +
                "    <div class=\"modal-box\">\n" +
                "      <h3 style=\"margin-bottom: 15px;\">Add New Book to Catalog</h3>\n" +
                "      <form onsubmit=\"submitAddBook(event)\">\n" +
                "        <div class=\"form-group\"><label>Title</label><input type=\"text\" id=\"m-title\" class=\"form-control\" required></div>\n" +
                "        <div class=\"form-group\"><label>Author</label><input type=\"text\" id=\"m-author\" class=\"form-control\" required></div>\n" +
                "        <div class=\"form-group\"><label>Category</label><input type=\"text\" id=\"m-category\" class=\"form-control\" required></div>\n" +
                "        <div class=\"form-group\"><label>ISBN</label><input type=\"text\" id=\"m-isbn\" class=\"form-control\" required placeholder=\"e.g. 978-0134685991\"></div>\n" +
                "        <div class=\"form-group\"><label>Publisher</label><input type=\"text\" id=\"m-publisher\" class=\"form-control\" required></div>\n" +
                "        <div style=\"display:grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px;\">\n" +
                "          <div class=\"form-group\"><label>Year</label><input type=\"number\" id=\"m-year\" class=\"form-control\" value=\"2024\" required></div>\n" +
                "          <div class=\"form-group\"><label>Price ($)</label><input type=\"number\" step=\"0.01\" id=\"m-price\" class=\"form-control\" value=\"45.00\" required></div>\n" +
                "          <div class=\"form-group\"><label>Copies</label><input type=\"number\" id=\"m-copies\" class=\"form-control\" value=\"3\" required></div>\n" +
                "        </div>\n" +
                "        <div style=\"display:flex; justify-content: flex-end; gap: 10px; margin-top: 15px;\">\n" +
                "          <button type=\"button\" class=\"btn\" onclick=\"closeModals()\">Cancel</button>\n" +
                "          <button type=\"submit\" class=\"btn btn-primary\">Save Book</button>\n" +
                "        </div>\n" +
                "      </form>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <!-- REGISTER MEMBER MODAL -->\n" +
                "  <div id=\"modal-register-member\" class=\"modal-overlay\">\n" +
                "    <div class=\"modal-box\">\n" +
                "      <h3 style=\"margin-bottom: 15px;\">Register Library Member</h3>\n" +
                "      <form onsubmit=\"submitRegisterMember(event)\">\n" +
                "        <div class=\"form-group\"><label>Full Name</label><input type=\"text\" id=\"mem-name\" class=\"form-control\" required></div>\n" +
                "        <div class=\"form-group\"><label>Email Address</label><input type=\"email\" id=\"mem-email\" class=\"form-control\" required placeholder=\"student@university.edu\"></div>\n" +
                "        <div class=\"form-group\"><label>Phone Number</label><input type=\"text\" id=\"mem-phone\" class=\"form-control\" required placeholder=\"e.g. 9876543210\"></div>\n" +
                "        <div class=\"form-group\"><label>Address / Campus Hostel</label><input type=\"text\" id=\"mem-address\" class=\"form-control\" required></div>\n" +
                "        <div style=\"display:flex; justify-content: flex-end; gap: 10px; margin-top: 15px;\">\n" +
                "          <button type=\"button\" class=\"btn\" onclick=\"closeModals()\">Cancel</button>\n" +
                "          <button type=\"submit\" class=\"btn btn-primary\">Register</button>\n" +
                "        </div>\n" +
                "      </form>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  <script>\n" +
                "    let allBooks = [];\n" +
                "    let currentTab = 'dashboard';\n" +
                "    function showTab(tabId) {\n" +
                "      currentTab = tabId;\n" +
                "      document.querySelectorAll('.tab-view').forEach(el => el.classList.remove('active'));\n" +
                "      document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));\n" +
                "      document.getElementById('tab-' + tabId).classList.add('active');\n" +
                "      const navIdx = ['dashboard','books','members','issues','transactions','reports','threads'].indexOf(tabId);\n" +
                "      if (navIdx >= 0) document.querySelectorAll('.nav-item')[navIdx].classList.add('active');\n" +
                "      const titles = {\n" +
                "        dashboard: 'Library Dashboard',\n" +
                "        books: 'Book Catalog & Inventory',\n" +
                "        members: 'Member Management',\n" +
                "        issues: 'Book Issue & Return Module',\n" +
                "        transactions: 'Transaction History & Fines',\n" +
                "        reports: 'Analytics Audit Reports',\n" +
                "        threads: 'Multithreading & Synchronization Demos'\n" +
                "      };\n" +
                "      document.getElementById('page-title').innerText = titles[tabId];\n" +
                "      refreshCurrentTab();\n" +
                "    }\n" +
                "    function refreshCurrentTab() {\n" +
                "      if (currentTab === 'dashboard') loadDashboard();\n" +
                "      else if (currentTab === 'books') loadBooks();\n" +
                "      else if (currentTab === 'members') loadMembers();\n" +
                "      else if (currentTab === 'transactions') loadTransactions();\n" +
                "      else if (currentTab === 'reports') loadAuditReport();\n" +
                "    }\n" +
                "    function showAlert(msg, isSuccess = true) {\n" +
                "      const box = document.getElementById('alert-box');\n" +
                "      box.innerText = msg;\n" +
                "      box.className = 'alert-banner ' + (isSuccess ? 'alert-success' : 'alert-error');\n" +
                "      box.style.display = 'block';\n" +
                "      setTimeout(() => { box.style.display = 'none'; }, 4000);\n" +
                "    }\n" +
                "    async function loadDashboard() {\n" +
                "      const res = await fetch('/api/dashboard');\n" +
                "      const data = await res.json();\n" +
                "      const grid = document.getElementById('stats-grid');\n" +
                "      grid.innerHTML = '';\n" +
                "      for (const [key, val] of Object.entries(data.stats)) {\n" +
                "        if (key === 'Database Engine') {\n" +
                "          document.getElementById('db-badge').innerText = 'DB: ' + val;\n" +
                "          continue;\n" +
                "        }\n" +
                "        grid.innerHTML += `<div class=\"card stat-card\"><div class=\"label\">${key}</div><div class=\"value\">${val}</div></div>`;\n" +
                "      }\n" +
                "      const actions = document.getElementById('recent-actions-list');\n" +
                "      actions.innerHTML = data.recentActions.map(a => `<li style=\"padding: 8px 0; border-bottom: 1px solid var(--border);\">⚡ ${a}</li>`).join('') || '<li style=\"color:var(--text-muted);\">No actions recorded yet.</li>';\n" +
                "    }\n" +
                "    async function loadBooks() {\n" +
                "      const res = await fetch('/api/books');\n" +
                "      allBooks = await res.json();\n" +
                "      renderBooksTable(allBooks);\n" +
                "    }\n" +
                "    function renderBooksTable(books) {\n" +
                "      const tbody = document.getElementById('books-table-body');\n" +
                "      tbody.innerHTML = books.map(b => `\n" +
                "        <tr>\n" +
                "          <td>#${b.id}</td>\n" +
                "          <td><strong>${b.title}</strong></td>\n" +
                "          <td>${b.author}</td>\n" +
                "          <td>${b.category}</td>\n" +
                "          <td><code>${b.isbn}</code></td>\n" +
                "          <td>$${b.price.toFixed(2)}</td>\n" +
                "          <td><span class=\"badge ${b.availableCopies > 0 ? 'badge-avail' : 'badge-out'}\">${b.availableCopies} / ${b.totalCopies} Avail</span></td>\n" +
                "          <td><button class=\"btn btn-danger\" style=\"padding:4px 8px; font-size:0.75rem;\" onclick=\"deleteBook(${b.id})\">Delete</button></td>\n" +
                "        </tr>\n" +
                "      `).join('') || '<tr><td colspan=\"8\" style=\"text-align:center;\">No books found.</td></tr>';\n" +
                "    }\n" +
                "    function filterBooks() {\n" +
                "      const q = document.getElementById('book-search').value.toLowerCase();\n" +
                "      const filtered = allBooks.filter(b => b.title.toLowerCase().includes(q) || b.author.toLowerCase().includes(q) || b.isbn.toLowerCase().includes(q));\n" +
                "      renderBooksTable(filtered);\n" +
                "    }\n" +
                "    async function deleteBook(id) {\n" +
                "      if (!confirm('Are you sure you want to delete book #' + id + '?')) return;\n" +
                "      const res = await fetch('/api/books', { method: 'POST', body: 'action=delete&id=' + id });\n" +
                "      const data = await res.json();\n" +
                "      if (data.success) { showAlert(data.message, true); loadBooks(); }\n" +
                "      else { showAlert(data.error, false); }\n" +
                "    }\n" +
                "    async function loadMembers() {\n" +
                "      const res = await fetch('/api/members');\n" +
                "      const members = await res.json();\n" +
                "      const tbody = document.getElementById('members-table-body');\n" +
                "      tbody.innerHTML = members.map(m => `\n" +
                "        <tr>\n" +
                "          <td>#${m.id}</td>\n" +
                "          <td><strong>${m.name}</strong></td>\n" +
                "          <td>${m.email}</td>\n" +
                "          <td>${m.phone}</td>\n" +
                "          <td>${m.date}</td>\n" +
                "          <td><span class=\"badge ${m.active ? 'badge-avail' : 'badge-out'}\">${m.active ? 'ACTIVE' : 'INACTIVE'}</span></td>\n" +
                "          <td>${m.activeLoans} / ${m.maxAllowed}</td>\n" +
                "          <td><button class=\"btn btn-primary\" style=\"padding:4px 8px; font-size:0.75rem;\" onclick=\"toggleMember(${m.id})\">${m.active ? 'Deactivate' : 'Activate'}</button></td>\n" +
                "        </tr>\n" +
                "      `).join('') || '<tr><td colspan=\"8\" style=\"text-align:center;\">No members found.</td></tr>';\n" +
                "    }\n" +
                "    async function toggleMember(id) {\n" +
                "      const res = await fetch('/api/members', { method: 'POST', body: 'action=toggle&id=' + id });\n" +
                "      const data = await res.json();\n" +
                "      if (data.success) { showAlert(data.message, true); loadMembers(); }\n" +
                "      else { showAlert(data.error, false); }\n" +
                "    }\n" +
                "    async function submitIssueBook(e) {\n" +
                "      e.preventDefault();\n" +
                "      const memberId = document.getElementById('issue-member-id').value;\n" +
                "      const bookId = document.getElementById('issue-book-id').value;\n" +
                "      const res = await fetch('/api/issue', { method: 'POST', body: `memberId=${memberId}&bookId=${bookId}` });\n" +
                "      const data = await res.json();\n" +
                "      if (data.success) {\n" +
                "        showAlert(data.message, true);\n" +
                "        document.getElementById('issue-member-id').value = '';\n" +
                "        document.getElementById('issue-book-id').value = '';\n" +
                "      } else {\n" +
                "        showAlert(data.error, false);\n" +
                "      }\n" +
                "    }\n" +
                "    async function submitReturnBook(e) {\n" +
                "      e.preventDefault();\n" +
                "      const issueId = document.getElementById('return-issue-id').value;\n" +
                "      const payNow = document.getElementById('return-pay-now').checked;\n" +
                "      const res = await fetch('/api/return', { method: 'POST', body: `issueId=${issueId}&payNow=${payNow}` });\n" +
                "      const data = await res.json();\n" +
                "      if (data.success) {\n" +
                "        showAlert(data.message, true);\n" +
                "        document.getElementById('return-issue-id').value = '';\n" +
                "      } else {\n" +
                "        showAlert(data.error, false);\n" +
                "      }\n" +
                "    }\n" +
                "    async function loadTransactions() {\n" +
                "      const res = await fetch('/api/transactions');\n" +
                "      const list = await res.json();\n" +
                "      const tbody = document.getElementById('transactions-table-body');\n" +
                "      tbody.innerHTML = list.map(r => `\n" +
                "        <tr>\n" +
                "          <td>#${r.id}</td>\n" +
                "          <td>${r.memberName} (ID #${r.memberId})</td>\n" +
                "          <td><strong>${r.bookTitle}</strong> (ID #${r.bookId})</td>\n" +
                "          <td>${r.issueDate}</td>\n" +
                "          <td>${r.dueDate}</td>\n" +
                "          <td>${r.returnDate}</td>\n" +
                "          <td>$${r.fine.toFixed(2)}</td>\n" +
                "          <td><span class=\"badge ${r.status === 'RETURNED' ? 'badge-avail' : (r.status === 'OVERDUE' ? 'badge-overdue' : 'badge-active')}\">${r.status}</span></td>\n" +
                "          <td>${r.fine > 0 && !r.finePaid ? `<button class=\"btn btn-primary\" style=\"padding:4px 8px; font-size:0.75rem;\" onclick=\"payFine(${r.id})\">Pay Fine</button>` : '-'}</td>\n" +
                "        </tr>\n" +
                "      `).join('') || '<tr><td colspan=\"9\" style=\"text-align:center;\">No transactions found.</td></tr>';\n" +
                "    }\n" +
                "    async function payFine(issueId) {\n" +
                "      const res = await fetch('/api/fines/pay', { method: 'POST', body: 'issueId=' + issueId });\n" +
                "      const data = await res.json();\n" +
                "      if (data.success) { showAlert(data.message, true); loadTransactions(); }\n" +
                "      else { showAlert(data.error, false); }\n" +
                "    }\n" +
                "    async function loadAuditReport() {\n" +
                "      const res = await fetch('/api/report');\n" +
                "      const data = await res.json();\n" +
                "      document.getElementById('report-output').innerText = data.report;\n" +
                "    }\n" +
                "    async function runProducerConsumer() {\n" +
                "      const status = document.getElementById('thread-status');\n" +
                "      status.innerText = '⚡ Running Producer-Consumer background threads...';\n" +
                "      const res = await fetch('/api/threads/producer-consumer', { method: 'POST' });\n" +
                "      const data = await res.json();\n" +
                "      status.innerText = '✅ ' + data.message;\n" +
                "      showAlert(data.message, true);\n" +
                "    }\n" +
                "    function openAddBookModal() { document.getElementById('modal-add-book').classList.add('active'); }\n" +
                "    function openRegisterMemberModal() { document.getElementById('modal-register-member').classList.add('active'); }\n" +
                "    function closeModals() { document.querySelectorAll('.modal-overlay').forEach(el => el.classList.remove('active')); }\n" +
                "    async function submitAddBook(e) {\n" +
                "      e.preventDefault();\n" +
                "      const params = new URLSearchParams({\n" +
                "        title: document.getElementById('m-title').value,\n" +
                "        author: document.getElementById('m-author').value,\n" +
                "        category: document.getElementById('m-category').value,\n" +
                "        isbn: document.getElementById('m-isbn').value,\n" +
                "        publisher: document.getElementById('m-publisher').value,\n" +
                "        year: document.getElementById('m-year').value,\n" +
                "        price: document.getElementById('m-price').value,\n" +
                "        copies: document.getElementById('m-copies').value\n" +
                "      });\n" +
                "      const res = await fetch('/api/books', { method: 'POST', body: params.toString() });\n" +
                "      const data = await res.json();\n" +
                "      if (data.success) { closeModals(); showAlert(data.message, true); loadBooks(); }\n" +
                "      else { showAlert(data.error, false); }\n" +
                "    }\n" +
                "    async function submitRegisterMember(e) {\n" +
                "      e.preventDefault();\n" +
                "      const params = new URLSearchParams({\n" +
                "        name: document.getElementById('mem-name').value,\n" +
                "        email: document.getElementById('mem-email').value,\n" +
                "        phone: document.getElementById('mem-phone').value,\n" +
                "        address: document.getElementById('mem-address').value\n" +
                "      });\n" +
                "      const res = await fetch('/api/members', { method: 'POST', body: params.toString() });\n" +
                "      const data = await res.json();\n" +
                "      if (data.success) { closeModals(); showAlert(data.message, true); loadMembers(); }\n" +
                "      else { showAlert(data.error, false); }\n" +
                "    }\n" +
                "    // Initial load\n" +
                "    loadDashboard();\n" +
                "  </script>\n" +
                "</body>\n" +
                "</html>";
    }
}
