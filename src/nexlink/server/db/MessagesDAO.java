package nexlink.server.db;

import nexlink.server.models.Message;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
public class MessagesDAO {

   
   
    // INSERT — save a new message to DB
    public void saveMessage(Message msg) {
        String sql = "INSERT INTO messages (sender, receiver, message_text) VALUES (?, ?, ?)";

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, msg.getSender());
            ps.setString(2, msg.getReceiver());
            ps.setString(3, msg.getMessage());

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // SELECT — get chat history between two users
    public ArrayList<Message> getHistory(String sender, String receiver) {
        String sql = "SELECT * FROM messages WHERE sender = ? AND receiver = ?";
        ArrayList<Message> messages = new ArrayList<>();

        try {
            Connection conn = DatabaseManager.getInstance().getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);

            ps.setString(1, sender);
            ps.setString(2, receiver);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Message msg = new Message(rs.getInt("id"),rs.getString("sender"),rs.getString("receiver"),rs.getString("message"));
                messages.add(msg);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return messages;
    }
   
    public List<String> getChatHistory(String user1, String user2) {
    List<String> history = new ArrayList<>(); // 💡 Starts empty!
   
    String query = "SELECT sender, message_text FROM messages WHERE " +
                   "(sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?) " +
                   "ORDER BY timestamp ASC";
                   
    try {
        Connection conn = DatabaseManager.getInstance().getConnection();
       
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, user1);
        stmt.setString(2, user2);
        stmt.setString(3, user2);
        stmt.setString(4, user1);
       
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String sender = rs.getString("sender");
                String msgText = rs.getString("message_text");
                // e.g., "ali: Salam wardan!"
                history.add(sender + ": " + msgText);
            }
        }
    } catch (SQLException e) {
        System.out.println("❌ Error loading chat history: " + e.getMessage());
    }
   
    return history; // Returns the clean empty arraylist if no rows match!
}
   
}