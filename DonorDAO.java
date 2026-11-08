import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class DonorDAO {

    public static boolean addDonor(String name, int age, String bloodGroup, String city, String phone) {
        String sql = "INSERT INTO donors (name, age, blood_group, city, phone) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setInt(2, age);
            ps.setString(3, bloodGroup);
            ps.setString(4, city);
            ps.setString(5, phone);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    public static List<String[]> search(String bloodGroup, String city) {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT id, name, blood_group, city, phone, last_donated "
                   + "FROM donors WHERE blood_group = ? AND city = ?";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, bloodGroup);
            ps.setString(2, city);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Date last = rs.getDate("last_donated");
                String lastText = "Never";
                String status = "Eligible";
                if (last != null) {
                    lastText = last.toString();
                    long days = ChronoUnit.DAYS.between(last.toLocalDate(), LocalDate.now());
                    if (days < 90) status = "Not eligible (" + (90 - days) + " days left)";
                }
                rows.add(new String[]{
                    String.valueOf(rs.getInt("id")), rs.getString("name"),
                    rs.getString("blood_group"), rs.getString("city"),
                    rs.getString("phone"), lastText, status});
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return rows;
    }
    public static boolean markDonated(int id) {
        String sql = "UPDATE donors SET last_donated = CURDATE() WHERE id = ? "
                   + "AND (last_donated IS NULL OR last_donated <= DATE_SUB(CURDATE(), INTERVAL 90 DAY))";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }
}