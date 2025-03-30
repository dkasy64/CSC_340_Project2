import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private int port;
    private int clientCounter = 1;

    //Cosmetics to make it *pretty*
    // public static final String ANSI_RED = "\u001B[31m";
    // public static final String ANSI_GREEN = "\u001B[32m";
    // public static final String ANSI_YELLOW = "\u001B[33m";
    // public static final String ANSI_BOLD = "\u001B[1m";
    // public static final String ANSI_RESET = "\u001B[0m";


    //making all the maps and setting the port
    public Server(int port) {
        this.port = port;
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server is on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                ClientHandler clientHandler = new ClientHandler(clientSocket, clientCounter++);
                clientHandler.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

  
    //main method to run the server
    public static void main(String[] args) throws IOException {
        int port = 5001;

        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        Server server = new Server(port);
        server.start();
    }
}
