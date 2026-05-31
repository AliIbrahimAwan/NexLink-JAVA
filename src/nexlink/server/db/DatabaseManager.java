package nexlink.server.db;

/**
 *
 * @author ALI
 */
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseManager {

    private static volatile DatabaseManager instance;

    private Connection connection;

    private static final String url = "jdbc:mysql://localhost:3306/nexlink";
    private static final String username = "root";
    private static final String password = "nexlink@555@";

    //private Constructor
    
    private DatabaseManager() {
        try {
            connection = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    //Double Checked Locking

    public static DatabaseManager getInstance() {
        DatabaseManager result = instance;

        if (result == null) {
            synchronized (DatabaseManager.class) {
                result = instance;
                if (result == null) {
                    instance = result = new DatabaseManager();
                }
            }
        }
        return result;
    }
    
    public Connection getConnection(){
        return connection;
    }
    
}
