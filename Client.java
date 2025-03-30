import java.io.*;
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
            clientWindow = new ClientWindow(out, in);
            new Thread(this::listenToServer).start();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenToServer() {
        try {
            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Server: " + message);
                // Handle server messages (e.g., update GUI with questions)
            }
        } catch (IOException e) {
            System.out.println("Disconnected from server.");
        }
    }
    public static void main(String[] args) {
        String serverIP = "127.0.0.1"; // Default to localhost
        int serverPort = 5001; // Default port

        if (args.length == 2) {
            serverIP = args[0];
            serverPort = Integer.parseInt(args[1]);
        }

        new Client(serverIP, serverPort);
    }
}