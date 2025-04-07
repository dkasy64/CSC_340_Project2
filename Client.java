import java.io.*; ///debuuuuug
import java.net.Socket;
import javax.swing.*;

public class Client {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ClientWindow clientWindow;
    
    public Client(String serverIP, int serverPort) {
        try {
            socket = new Socket(serverIP, serverPort);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Create a temporary client ID that will be replaced by server-assigned ID
            String tempClientId = "Client-" + System.currentTimeMillis() % 1000;
            System.out.println("Connected to server with temporary ID: " + tempClientId);
            
            // The ClientWindow will receive and use the correct server-assigned ID
            clientWindow = new ClientWindow(out, in, tempClientId);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(null, "Cannot connect to server: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String serverIP = "10.111.145.237"; // Default to localhost
            int serverPort = 9191; // Default port
            
            if (args.length == 2) {
                serverIP = args[0];
                serverPort = Integer.parseInt(args[1]);
            }
            
            new Client(serverIP, serverPort);
        });
    }
}