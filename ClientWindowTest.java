import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientWindowTest {
    public static void main(String[] args) {
        try {
            Socket socket = new Socket("10.111.145.237", 9191);
            //10.111.145.237
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            // Add a client ID parameter
            String clientId = "Test-Client";
            String serverIP = "localhost";
            new ClientWindow(out, in, clientId,serverIP);
            
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
        }
    }
}