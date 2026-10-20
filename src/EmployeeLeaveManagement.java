
import java.sql.*;
import java.util.Scanner;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class EmployeeLeaveManagement {

    static Scanner sc = new Scanner(System.in);

    static Connection con;

    public static void main(String[] args) {

        try {
            con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/employee_leave_db",
                    "root",
                    "Sivachandran04@"
            );

            int choice;

            do {
                System.out.println("\n--- EMPLOYEE LEAVE MANAGEMENT ---");
                System.out.println("1. Register Employee");
                System.out.println("2. View Employees");
                System.out.println("3. Apply Leave");
                System.out.println("4. Approve / Reject Leave");
                System.out.println("5. View Leave Balance");
                System.out.println("6. Delete Pending Leave Request");
                System.out.println("7. Exit");
                System.out.print("Enter choice: ");

                choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1:
                        registerEmployee();
                        break;

                    case 2:
                        viewEmployees();
                        break;

                    case 3:
                        applyLeave();
                        break;

                    case 4:
                        processLeave();
                        break;

                    case 5:
                        viewLeaveBalance();
                        break;

                    case 6:
                        deletePendingRequest();
                        break;

                    case 7:
                        System.out.println("Thank you!");
                        break;

                    default:
                        System.out.println("Invalid choice!");
                }

            } while (choice != 7);

            con.close();

        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }


    static void registerEmployee() {

        try {
            System.out.print("Enter employee name: ");
            String name = sc.nextLine();

            System.out.print("Enter department: ");
            String department = sc.nextLine();

            System.out.print("Enter email: ");
            String email = sc.nextLine();

            con.setAutoCommit(false);

            String sql = "INSERT INTO employees " +
                    "(employee_name, department, email) VALUES (?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(
                    sql, Statement.RETURN_GENERATED_KEYS);

            ps.setString(1, name);
            ps.setString(2, department);
            ps.setString(3, email);

            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();

            if (rs.next()) {
                int employeeId = rs.getInt(1);

                String balanceSql =
                        "INSERT INTO leave_balance " +
                                "(employee_id, total_leaves, used_leaves) " +
                                "VALUES (?, 12, 0)";

                PreparedStatement ps2 = con.prepareStatement(balanceSql);
                ps2.setInt(1, employeeId);
                ps2.executeUpdate();
            }

            con.commit();
            con.setAutoCommit(true);

            System.out.println("Employee registered successfully!");

        } catch (SQLException e) {
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }

            System.out.println("Registration failed: " + e.getMessage());
        }
    }


    static void viewEmployees() {

        try {
            String sql = "SELECT * FROM employees";
            PreparedStatement ps = con.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            System.out.println("\nID\tName\t\tDepartment\tEmail");

            while (rs.next()) {
                System.out.println(
                        rs.getInt("employee_id") + "\t" +
                                rs.getString("employee_name") + "\t\t" +
                                rs.getString("department") + "\t" +
                                rs.getString("email")
                );
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    static void applyLeave() {

        try {
            System.out.print("Enter employee ID: ");
            int employeeId = sc.nextInt();
            sc.nextLine();

            System.out.print("Enter start date (YYYY-MM-DD): ");
            LocalDate startDate = LocalDate.parse(sc.nextLine());

            System.out.print("Enter end date (YYYY-MM-DD): ");
            LocalDate endDate = LocalDate.parse(sc.nextLine());

            if (endDate.isBefore(startDate)) {
                System.out.println("End date cannot be before start date.");
                return;
            }

            int days = (int) ChronoUnit.DAYS.between(
                    startDate, endDate) + 1;

            System.out.print("Enter reason: ");
            String reason = sc.nextLine();

            String checkSql =
                    "SELECT employee_id FROM employees WHERE employee_id = ?";

            PreparedStatement check = con.prepareStatement(checkSql);
            check.setInt(1, employeeId);

            ResultSet rs = check.executeQuery();

            if (!rs.next()) {
                System.out.println("Employee not found!");
                return;
            }

            String sql = "INSERT INTO leave_requests " +
                    "(employee_id, start_date, end_date, leave_days, reason) " +
                    "VALUES (?, ?, ?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, employeeId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            ps.setInt(4, days);
            ps.setString(5, reason);

            ps.executeUpdate();

            System.out.println("Leave request submitted!");
            System.out.println("Leave days: " + days);
            System.out.println("Status: Pending");

        } catch (Exception e) {
            System.out.println("Unable to apply leave: " + e.getMessage());
        }
    }


    static void processLeave() {

        try {
            System.out.print("Enter leave request ID: ");
            int requestId = sc.nextInt();

            System.out.print("1. Approve  2. Reject: ");
            int action = sc.nextInt();

            if (action != 1 && action != 2) {
                System.out.println("Invalid action!");
                return;
            }

            con.setAutoCommit(false);

            String sql = "SELECT employee_id, leave_days, status " +
                    "FROM leave_requests WHERE request_id = ? FOR UPDATE";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, requestId);

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                con.rollback();
                con.setAutoCommit(true);
                System.out.println("Request not found!");
                return;
            }

            int employeeId = rs.getInt("employee_id");
            int days = rs.getInt("leave_days");
            String status = rs.getString("status");

            if (!status.equals("Pending")) {
                con.rollback();
                con.setAutoCommit(true);
                System.out.println("Request already processed!");
                return;
            }

            if (action == 1) {

                String balanceSql =
                        "SELECT total_leaves, used_leaves FROM leave_balance " +
                                "WHERE employee_id = ? FOR UPDATE";

                PreparedStatement ps2 = con.prepareStatement(balanceSql);
                ps2.setInt(1, employeeId);

                ResultSet balance = ps2.executeQuery();

                if (!balance.next()) {
                    con.rollback();
                    con.setAutoCommit(true);
                    System.out.println("Leave balance not found!");
                    return;
                }

                int total = balance.getInt("total_leaves");
                int used = balance.getInt("used_leaves");

                if (days > total - used) {
                    con.rollback();
                    con.setAutoCommit(true);
                    System.out.println("Insufficient leave balance!");
                    return;
                }

                String updateBalance =
                        "UPDATE leave_balance SET used_leaves = used_leaves + ? " +
                                "WHERE employee_id = ?";

                PreparedStatement ps3 = con.prepareStatement(updateBalance);
                ps3.setInt(1, days);
                ps3.setInt(2, employeeId);
                ps3.executeUpdate();

                updateRequestStatus(requestId, "Approved");

            } else {
                updateRequestStatus(requestId, "Rejected");
            }

            con.commit();
            con.setAutoCommit(true);

            System.out.println("Leave request processed successfully!");

        } catch (SQLException e) {
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }

            System.out.println("Processing failed: " + e.getMessage());
        }
    }

    static void updateRequestStatus(int requestId, String status)
            throws SQLException {

        String sql = "UPDATE leave_requests SET status = ? " +
                "WHERE request_id = ?";

        PreparedStatement ps = con.prepareStatement(sql);
        ps.setString(1, status);
        ps.setInt(2, requestId);
        ps.executeUpdate();
    }


    static void viewLeaveBalance() {

        try {
            System.out.print("Enter employee ID: ");
            int employeeId = sc.nextInt();

            String sql = "SELECT e.employee_name, b.total_leaves, " +
                    "b.used_leaves FROM employees e " +
                    "JOIN leave_balance b ON e.employee_id = b.employee_id " +
                    "WHERE e.employee_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, employeeId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                int total = rs.getInt("total_leaves");
                int used = rs.getInt("used_leaves");

                System.out.println("Employee: " +
                        rs.getString("employee_name"));
                System.out.println("Total leaves: " + total);
                System.out.println("Used leaves: " + used);
                System.out.println("Remaining leaves: " + (total - used));

            } else {
                System.out.println("Employee not found!");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }


    static void deletePendingRequest() {

        try {
            System.out.print("Enter request ID: ");
            int requestId = sc.nextInt();

            String sql = "DELETE FROM leave_requests " +
                    "WHERE request_id = ? AND status = 'Pending'";

            PreparedStatement ps = con.prepareStatement(sql);
            ps.setInt(1, requestId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Pending request deleted!");
            } else {
                System.out.println(
                        "Request not found or already processed.");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
