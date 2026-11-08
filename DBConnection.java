import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {
    public static Connection getConnection() {
        String password = System.getenv("BLOODDONOR_DB_PASSWORD");
        if (password == null) {
            System.out.println("Set the BLOODDONOR_DB_PASSWORD environment variable first.");
            return null;
        }
        try {
            return DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/blood_bank", "root", password);
        } catch (Exception e) {
            System.out.println("Connection failed: " + e.getMessage());
            return null;
        }
    }
}
