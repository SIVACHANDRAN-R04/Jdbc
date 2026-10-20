
import java.sql.*;
import java.util.Scanner;

public class InventoryManagement {

    static Scanner sc = new Scanner(System.in);
    static Connection con;

    public static void main(String[] args) {

        try {
            con = DriverManager.getConnection(
                    "jdbc:mysql://localhost:3306/inventory_db",
                    "root",
                    "Sivachandran04@"
            );

            int choice;

            do {
                System.out.println("\n--- INVENTORY MANAGEMENT SYSTEM ---");
                System.out.println("1. Add Product");
                System.out.println("2. View Products");
                System.out.println("3. Update Stock");
                System.out.println("4. Record Sale");
                System.out.println("5. Generate Reports");
                System.out.println("6. Exit");
                System.out.print("Enter choice: ");

                choice = sc.nextInt();
                sc.nextLine();

                switch (choice) {
                    case 1:
                        addProduct();
                        break;

                    case 2:
                        viewProducts();
                        break;

                    case 3:
                        updateStock();
                        break;

                    case 4:
                        recordSale();
                        break;

                    case 5:
                        generateReports();
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


    static void addProduct() {

        try {
            System.out.print("Enter product name: ");
            String name = sc.nextLine();

            System.out.print("Enter product price: ");
            double price = sc.nextDouble();

            System.out.print("Enter initial quantity: ");
            int quantity = sc.nextInt();

            System.out.print("Enter supplier ID: ");
            int supplierId = sc.nextInt();
            sc.nextLine();

            if (price <= 0 || quantity < 0) {
                System.out.println("Invalid price or quantity!");
                return;
            }

            String sql = "INSERT INTO products " +
                    "(product_name, price, quantity, supplier_id) " +
                    "VALUES (?, ?, ?, ?)";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setString(1, name);
            ps.setDouble(2, price);
            ps.setInt(3, quantity);
            ps.setInt(4, supplierId);

            ps.executeUpdate();

            System.out.println("Product added successfully!");

        } catch (SQLException e) {
            System.out.println("Unable to add product: " + e.getMessage());
        }
    }


    static void viewProducts() {

        String sql = "SELECT p.product_id, p.product_name, p.price, " +
                "p.quantity, s.supplier_name " +
                "FROM products p " +
                "LEFT JOIN suppliers s ON p.supplier_id = s.supplier_id " +
                "ORDER BY p.product_id";

        try (PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\nID\tProduct\t\tPrice\tStock\tSupplier");

            while (rs.next()) {
                System.out.println(
                        rs.getInt("product_id") + "\t" +
                                rs.getString("product_name") + "\t\t" +
                                rs.getBigDecimal("price") + "\t" +
                                rs.getInt("quantity") + "\t" +
                                rs.getString("supplier_name")
                );
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    static void updateStock() {

        try {
            System.out.print("Enter product ID: ");
            int productId = sc.nextInt();

            System.out.print("Enter quantity to add: ");
            int addedQuantity = sc.nextInt();
            sc.nextLine();

            if (addedQuantity <= 0) {
                System.out.println("Quantity must be positive!");
                return;
            }

            String sql = "UPDATE products " +
                    "SET quantity = quantity + ? WHERE product_id = ?";

            PreparedStatement ps = con.prepareStatement(sql);

            ps.setInt(1, addedQuantity);
            ps.setInt(2, productId);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Stock updated successfully!");
            } else {
                System.out.println("Product not found!");
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
    static void recordSale() {

        try {
            System.out.print("Enter product ID: ");
            int productId = sc.nextInt();

            System.out.print("Enter quantity sold: ");
            int soldQuantity = sc.nextInt();
            sc.nextLine();

            if (soldQuantity <= 0) {
                System.out.println("Quantity must be positive!");
                return;
            }

            con.setAutoCommit(false);


            String checkSql =
                    "SELECT price, quantity FROM products " +
                            "WHERE product_id = ? FOR UPDATE";

            PreparedStatement checkPs = con.prepareStatement(checkSql);
            checkPs.setInt(1, productId);

            ResultSet rs = checkPs.executeQuery();

            if (!rs.next()) {
                con.rollback();
                con.setAutoCommit(true);
                System.out.println("Product not found!");
                return;
            }

            double price = rs.getDouble("price");
            int stock = rs.getInt("quantity");

            if (soldQuantity > stock) {
                con.rollback();
                con.setAutoCommit(true);
                System.out.println("Insufficient stock!");
                return;
            }

            double totalAmount = price * soldQuantity;


            String updateSql =
                    "UPDATE products SET quantity = quantity - ? " +
                            "WHERE product_id = ?";

            PreparedStatement updatePs = con.prepareStatement(updateSql);
            updatePs.setInt(1, soldQuantity);
            updatePs.setInt(2, productId);
            updatePs.executeUpdate();


            String saleSql =
                    "INSERT INTO sales " +
                            "(product_id, quantity_sold, total_amount, sale_date) " +
                            "VALUES (?, ?, ?, ?)";

            PreparedStatement salePs = con.prepareStatement(saleSql);
            salePs.setInt(1, productId);
            salePs.setInt(2, soldQuantity);
            salePs.setDouble(3, totalAmount);
            salePs.setDate(4, Date.valueOf(java.time.LocalDate.now()));

            salePs.executeUpdate();

            con.commit();
            con.setAutoCommit(true);

            System.out.println("Sale recorded successfully!");
            System.out.printf("Total amount: Rs. %.2f%n", totalAmount);

        } catch (SQLException e) {
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException ex) {
                System.out.println(ex.getMessage());
            }

            System.out.println("Sale failed: " + e.getMessage());
        }
    }


    static void generateReports() {

        try {

            String salesSql =
                    "SELECT COUNT(*) AS total_sales, " +
                            "COALESCE(SUM(quantity_sold), 0) AS items_sold, " +
                            "COALESCE(SUM(total_amount), 0) AS revenue " +
                            "FROM sales";

            PreparedStatement ps = con.prepareStatement(salesSql);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("\n--- SALES REPORT ---");
                System.out.println("Number of sales: " +
                        rs.getInt("total_sales"));
                System.out.println("Items sold: " +
                        rs.getInt("items_sold"));
                System.out.println("Total revenue: Rs. " +
                        rs.getBigDecimal("revenue"));
            }

            System.out.println("\n--- STOCK REPORT ---");

            String stockSql =
                    "SELECT product_name, quantity FROM products " +
                            "ORDER BY quantity ASC";

            PreparedStatement stockPs = con.prepareStatement(stockSql);
            ResultSet stockRs = stockPs.executeQuery();

            while (stockRs.next()) {
                System.out.println(
                        stockRs.getString("product_name") +
                                " - Stock: " + stockRs.getInt("quantity")
                );
            }


            String productSql =
                    "SELECT p.product_name, " +
                            "COALESCE(SUM(s.quantity_sold), 0) AS sold, " +
                            "COALESCE(SUM(s.total_amount), 0) AS revenue " +
                            "FROM products p LEFT JOIN sales s " +
                            "ON p.product_id = s.product_id " +
                            "GROUP BY p.product_id, p.product_name " +
                            "ORDER BY revenue DESC";

            PreparedStatement productPs =
                    con.prepareStatement(productSql);

            ResultSet productRs = productPs.executeQuery();

            System.out.println("\n--- PRODUCT SALES REPORT ---");

            while (productRs.next()) {
                System.out.println(
                        productRs.getString("product_name") +
                                " | Sold: " + productRs.getInt("sold") +
                                " | Revenue: Rs. " +
                                productRs.getBigDecimal("revenue")
                );
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }
}
