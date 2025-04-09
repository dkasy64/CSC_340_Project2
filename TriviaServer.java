import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class TriviaServer {
    private static final int TCP_PORT = 9191;
    private static final int UDP_PORT = 9192;
    private static Queue<String> udpQueue = new ConcurrentLinkedQueue<>();
    private static List<ClientThread> activeClients = Collections.synchronizedList(new ArrayList<>());
    private static List<String> questions;
    public static volatile boolean gameStarted = false;
    public static volatile boolean gameKilled = false;
    
    public static void main(String[] args) {
        try {
            // Load questions from file
            questions = loadQuestions();
            System.out.println("Loaded " + questions.size() + " questions");
            
            // Start UDP Thread for handling buzz messages
            UDPThread udpThread = new UDPThread(UDP_PORT, udpQueue, activeClients);
            new Thread(udpThread).start();
            System.out.println("UDP service started on port " + UDP_PORT);
            
            ExecutorService executor = Executors.newFixedThreadPool(2);
            
            // Thread to start the game
            executor.submit(() -> {
                try {
                    System.out.println("Press ENTER to start the game...");
                    System.in.read(); // Wait for ENTER
                    synchronized (activeClients) {
                        if (!gameKilled) {
                            gameStarted = true; // Start the game
                            activeClients.notifyAll(); // Wake up all waiting clients
                            System.out.println("Game started!");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            
            // Thread to monitor for kill switch
            executor.submit(() -> {
                Scanner scanner = new Scanner(System.in);
                while (true) {
                    String command = scanner.nextLine().trim();
                    if (command.equalsIgnoreCase("kill") || command.equalsIgnoreCase("end")) {
                        System.out.println("Kill switch activated! Ending game for all clients...");
                        killGame("Game terminated by host");
                        break;
                    } else if (command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("quit")) {
                        System.out.println("Shutting down server...");
                        System.exit(0);
                    } else if (!command.isEmpty()) {
                        System.out.println("Available commands: 'kill' or 'end' to end the game, 'exit' or 'quit' to shut down the server");
                    }
                }
            });

            // Start TCP server
            try (ServerSocket serverSocket = new ServerSocket(TCP_PORT)) {
                System.out.println("Trivia Server started on port " + TCP_PORT);
                System.out.println("Type 'kill' or 'end' to end the game for all clients");
                
                while (!gameKilled) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        System.out.println("New client connected from " + clientSocket.getInetAddress());
                        
                        // Create and start a new thread for this client
                        ClientThread clientThread = new ClientThread(clientSocket, udpQueue, activeClients, questions);
                        activeClients.add(clientThread);
                        new Thread(clientThread).start();
                    } catch (SocketException e) {
                        if (gameKilled) {
                            break;
                        } else {
                            throw e;
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Kill the game for all connected clients
     * @param reason The reason to display to clients
     */
    public static void killGame(String reason) {
        synchronized (activeClients) {
            gameKilled = true;
            gameStarted = false;
            
            // Notify all clients that the game has been killed
            for (ClientThread client : activeClients) {
                client.getWriter().println("GAME_KILLED:" + reason);
            }
            
            activeClients.notifyAll(); // Wake up any waiting threads
        }
    }
    
    // Load questions from questions.txt
    private static List<String> loadQuestions() throws IOException {
        List<String> questionList = new ArrayList<>();
        
        // Try to find the file in different locations
        String[] possiblePaths = {
            "questions.txt",                              // Current directory
            "src/questions.txt",                          // src folder
            System.getProperty("user.dir") + "/questions.txt"  // paste path to question.txt here
        };
        
        File questionFile = null;
        for (String path : possiblePaths) {
            File f = new File(path);
            if (f.exists() && f.isFile()) {
                questionFile = f;
                break;
            }
        }
        
        //If file is not found
        if (questionFile == null) {
            System.err.println("WARNING: questions.txt file not found! Creating a sample file with test questions.");
            
            // Create a sample questions file
            questionFile = new File("questions.txt");
            try (PrintWriter writer = new PrintWriter(questionFile)) {
                writer.println("Q1|What is Dr. J's first name?|Cheetah|Chaten|Jaiswal|Chetan|Chetan");
                writer.println("Q2|How many lunches are owned in Section 2 of CSC340?|2|3|2.5|1.5|2.5");
                writer.println("Q3|What is the capital of France?|London|Berlin|Paris|Madrid|Paris");
            }
            System.out.println("Created sample questions file at: " + questionFile.getAbsolutePath());
        }
        
        // Now read the questions
        try (BufferedReader reader = new BufferedReader(new FileReader(questionFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    questionList.add(line);
                }
            }
        }
        
        if (questionList.isEmpty()) {
            throw new IOException("No questions found in questions.txt");
        }
        
        return questionList;
    }
}