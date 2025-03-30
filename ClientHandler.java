import java.io.*;
import java.net.Socket;

public class ClientHandler {
    private Socket clientSocket;
    private int clientID;
    private PrintWriter out;
    private BufferedReader in;

    public ClientHandler(Socket clientSocket, int clientID) {
        this.clientSocket = clientSocket;
        this.clientID = clientID;
    }

    //@Override
    public void start() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            out.println("Welcome! You're Client " + clientID);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Recieved from client " + clientID + ": " + message);
                out.println("You sent in answer: " + message);
            }
        } catch (IOException e) {
            System.out.println("Client " + clientID + " disconnected");
            e.printStackTrace();
        }
    }
}
