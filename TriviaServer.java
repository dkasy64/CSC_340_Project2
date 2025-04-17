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
            // Load questions directly in code instead of from file
            questions = createQuestions();
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
  
    // Create questions directly in code
    private static List<String> createQuestions() {
        List<String> questionList = new ArrayList<>();
      
        // Add all 20 questions from the provided list
        questionList.add("Q1|What is Dr. J's first name?|Cheetah|Chaten|Jaiswal|Chetan|Chetan");
        questionList.add("Q2|How many lunches are owned in Section 2 of CSC340? (Roughly)|2|3|2.5|1.5|2.5");
        questionList.add("Q3|What is Dr. J's Rate My Professor rating?|5.0|2.98|4.2|3.8|3.8");
        questionList.add("Q4|In terms of di, si, Ri (i = 1,2,3), and L, what is the total end-to-end delay for packet L?|64 ms|102 ms|54 ms|25 ms|64 ms");
        questionList.add("Q5|What's Dr. J's favorite football team?|Detroit Lions|Philadelphia Eagles|Kansas City Chiefs|NY Giants|Kansas City Chiefs");
        questionList.add("Q6|Select the correct pair:|UCP & TDP|UDT & TCP|UPC & TPC|UDP & TCP|UDP & TCP");
        questionList.add("Q7|What's the minimum amount of time required to pass this class? (as listed in the syllabus)|5 hours every day|2 hours every day|3 hours every day|25 hours every day|2 hours every day");
        questionList.add("Q8|What does RDR stand for?|Red Dead Rising|Read Deed Redeemption|Red Dead Redemption|Rum Dum Reddening|Red Dead Redemption");
        questionList.add("Q9|Dr. J wants to go on vacation. Where would he choose to go?|Kansas City|Houston|Las Vegas|New York|Las Vegas");
        questionList.add("Q10|What's Dr. J's favorite type of computer?|Mac|Mac|Linux|Mac|Linux");
        questionList.add("Q11|Peer-Peer nodes can act as both a client and a server|True|True|True|False|True");
        questionList.add("Q12|What is the purpose of the Transportation Layer?|End to end delivery|App to app delivery|Node to node delivery|Ask someone to help you out|App to app delivery");
        questionList.add("Q13|What does \"pixel\" stand for?|Picture excellent|Picture matrix|Picture element|Picture perfect|Picture element");
        questionList.add("Q14|What is HTTP v.2 called?|SPDY|QUIC|LGHTNG|SPDR|SPDY");
        questionList.add("Q15|TCP and UDP are protocols of what layer?|Application|Transport|Network|Physical|Transport");
        questionList.add("Q16|What does IETF stand for?|Internal Engineering Tech Field|Internet Engineering Task Force|Internet Engaging Track Force|Internet Engineering Tracking Forum|Internet Engineering Task Force");
        questionList.add("Q17|Which of these movies has Dr. J NOT talked about in class?|Antman & the Wasp|John Wick|Mad Max|Matrix|Antman & the Wasp");
        questionList.add("Q18|Yo bro, if you wanna make it into someone's top 4 on BitTorrent, what's the move?|Just vibe and hope they notice you|Seed their torrents like a legend|Slide into their DMs|...Is this written by ChatGPT?|Seed their torrents like a legend");
        questionList.add("Q19|What does HTTP stand for?|Hyper text transfer portal|Hyper transfer text portal|Hype text transfer portal|Hyper tranfer text protocol|Hyper text transfer portal");
        questionList.add("Q20|There's no way you're still playing this game... are you??|Unfortunately, yes|Dr. J is forcing me against my will|Part of my top 5 favorite games tbh|Ofc! Team Lebron the best!|Part of my top 5 favorite games tbh");
      
        return questionList;
    }
}