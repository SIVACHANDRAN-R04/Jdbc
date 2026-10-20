
import java.sql.*;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class LibraryManagement {

    static Scanner sc = new Scanner(System.in);
    static Connection con;

    static final double FINE_PER_DAY = 5.0;

    public static void main(String[] args) {

        try {
            con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/library_db",
                    "root",
                    "Sivachandran04@"
            );

            int choice;

            do {
                System.out.println("\n--- LIBRARY MANAGEMENT SYSTEM ---");
                System.out.println("1. Add Book");
                System.out.println("2. Search Book");
                System.out.println("3. Issue Book");
                System.out.println("4. Return Book");
                System.out.println("5. View Issue Records");
                System.out.println("6. Exit");
                System.out.print("Enter choice: ");

                choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1:
                        addBook();
                        break;

                    case 2:
                        searchBook();
                        break;

                    case 3:
                        issueBook();
                        break;

                    case 4:
                        returnBook();
                        break;

                    case 5:
                        viewIssueRecords();
                        break;

                    case 6:
                        System.out.println("Thank you!");
                        break;

                    default:
                        System.out.println("Invalid choice!");
                }

            } while (choice != 6);

            con.close();

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }


    static void addBook() {

        try {
            System.out.print("Enter book title: ");
            String title = sc.nextLine();

            System.out.print("Enter author name: ");
            String author = sc.nextLine();

            System.out.print("Enter quantity: ");
            int quantity = sc.nextInt();
            sc.nextLine();

            if (quantity <= 0) {
                System.out.println("Quantity must be greater than zero.");
                return;
            }

            String sql = "INSERT INTO books (title, author, quantity) " +
                    "VALUES (?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, title);
            ps.setString(2, author);
            ps.setInt(3, quantity);

            ps.executeUpdate();

            System.out.println("Book added successfully!");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    static void searchBook() {

        try {
            System.out.print("Enter book title to search: ");
            String title = sc.nextLine();

            String sql = "SELECT * FROM books WHERE title LIKE ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, "%" + title + "%");

            ResultSet rs = ps.executeQuery();

            boolean found = false;

            while (rs.next()) {
                found = true;

                System.out.println("\nBook ID: " +
                        rs.getInt("book_id"));
                System.out.println("Title: " +
                        rs.getString("title"));
                System.out.println("Author: " +
                        rs.getString("author"));
                System.out.println("Available quantity: " +
                        rs.getInt("quantity"));
            }

            if (!found) {
                System.out.println("No books found!");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    static void issueBook() {

        try {
            System.out.print("Enter book ID: ");
            int bookId = sc.nextInt();

            System.out.print("Enter member ID: ");
            int memberId = sc.nextInt();
            sc.nextLine();

            LocalDate issueDate = LocalDate.now();
            LocalDate dueDate = issueDate.plusDays(14);

            con.setAutoCommit(false);


            String memberSql =
                    "SELECT member_id FROM members WHERE member_id = ?";

            PreparedStatement memberPs =
                    con.prepareStatement(memberSql);

            memberPs.setInt(1, memberId);

            ResultSet memberRs = memberPs.executeQuery();

            if (!memberRs.next()) {
                con.rollback();
                con.setAutoCommit(true);
                System.out.println("Member not found!");
                return;
            }


            String bookSql =
                    "SELECT quantity FROM books WHERE book_id = ? FOR UPDATE";

            PreparedStatement bookPs =
                    con.prepareStatement(bookSql);

            bookPs.setInt(1, bookId);

            ResultSet bookRs = bookPs.executeQuery();

            if (!bookRs.next()) {
                con.rollback();
                con.setAutoCommit(true);
                System.out.println("Book not found!");
                return;
            }

            if (bookRs.getInt("quantity") <= 0) {
                con.rollback();
                con.setAutoCommit(true);
                System.out.println("Book is not available!");
                return;
            }

            String issueSql =
                    "INSERT INTO book_issues " +
                            "(book_id, member_id, issue_date, due_date, status) " +
                            "VALUES (?, ?, ?, ?, 'Issued')";

            PreparedStatement issuePs =
                    con.prepareStatement(issueSql);

            issuePs.setInt(1, bookId);
            issuePs.setInt(2, memberId);
            issuePs.setDate(3, Date.valueOf(issueDate));
            issuePs.setDate(4, Date.valueOf(dueDate));

            issuePs.executeUpdate();


            String updateSql =
                    "UPDATE books SET quantity = quantity - 1 " +
                            "WHERE book_id = ?";

            PreparedStatement updatePs =
                    con.prepareStatement(updateSql);

            updatePs.setInt(1, bookId);
            updatePs.executeUpdate();

            con.commit();
            con.setAutoCommit(true);

            System.out.println("Book issued successfully!");
            System.out.println("Issue date: " + issueDate);
            System.out.println("Due date: " + dueDate);

        } catch (SQLException e) {
            rollbackTransaction();
            System.out.println("Issue failed: " + e.getMessage());
        }
    }


    static void returnBook() {

        try {
            System.out.print("Enter issue ID: ");
            int issueId = sc.nextInt();
            sc.nextLine();

            LocalDate returnDate = LocalDate.now();

            con.setAutoCommit(false);

            String sql =
                    "SELECT book_id, due_date, status " +
                            "FROM book_issues WHERE issue_id = ? FOR UPDATE";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, issueId);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                con.rollback();
                con.setAutoCommit(true);
                System.out.println("Issue record not found!");
                return;
            }

            int bookId = rs.getInt("book_id");
            LocalDate dueDate = rs.getDate("due_date").toLocalDate();
            String status = rs.getString("status");

            if (!status.equals("Issued")) {
                con.rollback();
                con.setAutoCommit(true);
                System.out.println("This book has already been returned!");
                return;
            }

            long lateDays = Math.max(
                    0, ChronoUnit.DAYS.between(dueDate, returnDate));

            double fine = lateDays * FINE_PER_DAY;

            String updateIssue =
                    "UPDATE book_issues SET return_date = ?, " +
                            "fine = ?, status = 'Returned' WHERE issue_id = ?";

            PreparedStatement updatePs =
                    con.prepareStatement(updateIssue);

            updatePs.setDate(1, Date.valueOf(returnDate));
            updatePs.setDouble(2, fine);
            updatePs.setInt(3, issueId);

            updatePs.executeUpdate();

            String updateBook =
                    "UPDATE books SET quantity = quantity + 1 " +
                            "WHERE book_id = ?";

            PreparedStatement bookPs =
                    con.prepareStatement(updateBook);

            bookPs.setInt(1, bookId);
            bookPs.executeUpdate();

            con.commit();
            con.setAutoCommit(true);

            System.out.println("Book returned successfully!");
            System.out.println("Late days: " + lateDays);
            System.out.printf("Fine amount: Rs. %.2f%n", fine);

        } catch (SQLException e) {
            rollbackTransaction();
            System.out.println("Return failed: " + e.getMessage());
        }
    }


    static void viewIssueRecords() {

        try {
            String sql =
                    "SELECT bi.issue_id, b.title, m.member_name, " +
                            "bi.issue_date, bi.due_date, bi.return_date, " +
                            "bi.fine, bi.status " +
                            "FROM book_issues bi " +
                            "JOIN books b ON bi.book_id = b.book_id " +
                            "JOIN members m ON bi.member_id = m.member_id " +
                            "ORDER BY bi.issue_id";

            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                System.out.println("\nIssue ID: " +
                        rs.getInt("issue_id"));
                System.out.println("Book: " +
                        rs.getString("title"));
                System.out.println("Member: " +
                        rs.getString("member_name"));
                System.out.println("Issue date: " +
                        rs.getDate("issue_date"));
                System.out.println("Due date: " +
                        rs.getDate("due_date"));
                System.out.println("Return date: " +
                        rs.getDate("return_date"));
                System.out.println("Fine: Rs. " +
                        rs.getBigDecimal("fine"));
                System.out.println("Status: " +
                        rs.getString("status"));
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    static void rollbackTransaction() {

        try {
            if (con != null) {
                con.rollback();
                con.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.out.println("Rollback failed: " + e.getMessage());
        }
    }
}
