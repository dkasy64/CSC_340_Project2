import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientWindowTest {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 9191);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Add a client ID parameter
            String clientId = "Test-Client";
            new ClientWindow(out, in, clientId);
            
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
        }
    }
}