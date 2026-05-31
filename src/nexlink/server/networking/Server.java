package nexlink.server.networking;

/**
 *
 * @author ALI
 */
import java.net.Socket;
import java.net.ServerSocket;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import nexlink.server.gui.ServerConsole;

public class Server {

    public static ConcurrentHashMap<String, ClientHandler> activeClients = new ConcurrentHashMap<>();

    public static void broadcastUserListUpdate() {
        System.out.println(" Broadcasting fresh user list updates to all active clients...");

        // Loop through every single logged-in client handler in memory
        for (ClientHandler client : activeClients.values()) {
            try {
                // Trigger the existing list packet building code for each client
                client.sendLiveUserList();
            } catch (Exception e) {
                System.out.println("Error broadcasting to a client: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {

        ServerSocket serverSocket = null;

        try {
            serverSocket = new ServerSocket(2222);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        java.awt.EventQueue.invokeLater(() -> {
        ServerConsole consoleFrame = new ServerConsole();
        consoleFrame.setLocationRelativeTo(null); // Centers it on screen
        consoleFrame.setVisible(true);
    });

    // Short pause to ensure Swing constructs the window UI layout tree properly
    try { Thread.sleep(250); } catch (Exception e) {}

    // 2. Start streaming core environment updates right to your window
    ServerConsole.log("SYSTEM", "Initializing NexLink Core Instance on Port 12345...");
    

    ServerConsole.log("SYSTEM", "Ready and listening for active client cluster channels...");
        
        
        
        while (true) {
            try {
                Socket socket = serverSocket.accept();

ServerConsole.log("CONNECT", "The client has connected");
                ClientHandler clientHandler = new ClientHandler(socket);
                Thread thread = new Thread(clientHandler);
                thread.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
