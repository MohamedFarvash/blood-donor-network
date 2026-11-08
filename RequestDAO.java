import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RequestDAO {

    public static boolean addRequest(String patientName, String bloodGroup, String city, String contact) {
        String sql = "INSERT INTO requests (patient_name, blood_group, city, contact) VALUES (?, ?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, patientName);
            ps.setString(2, bloodGroup);
            ps.setString(3, city);
            ps.setString(4, contact);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            return false;
        }
    }

    public static List<String[]> listOpen() {
        List<String[]> rows = new ArrayList<>();
        String sql = "SELECT id, patient_name, blood_group, city, contact "
                   + "FROM requests WHERE status = 'Open' ORDER BY id DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(new String[]{
                    String.valueOf(rs.getInt("id")), rs.getString("patient_name"),
                    rs.getString("blood_group"), rs.getString("city"), rs.getString("contact")});
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
        return rows;
    }

    public static boolean markFulfilled(int id) {
        String sql = "UPDATE requests SET status = 'Fulfilled' WHERE id = ?";
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
