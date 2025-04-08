import java.io.*;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class ClientThread implements Runnable {
    private Socket clientSocket;
    private Queue<String> udpQueue;
    private List<ClientThread> activeClients;
    private List<String> questions;
    private PrintWriter out;
    private BufferedReader in;
    private int currentQuestionIndex = 0;
    private static Map<String, Integer> clientScores = new HashMap<>();
    private String clientId;
    private boolean buzzedInFirst = false;
    private boolean gameOver = false;

    public ClientThread(Socket socket, Queue<String> udpQueue, List<ClientThread> activeClients, List<String> questions) throws IOException {
        this.clientSocket = socket;
        this.udpQueue = udpQueue;
        this.activeClients = activeClients;
        this.questions = questions;
        this.out = new PrintWriter(clientSocket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        
        // Generate client ID and add to scores map
        this.clientId = "Client-" + (activeClients.size() + 1);
        clientScores.put(clientId, 0);
        System.out.println(clientId + " connected from: " + socket.getInetAddress());
        printScores(); // Show current scores
    }
    
    public String getClientId() {
        return clientId;
    }
    
    public PrintWriter getWriter() {
        return out;
    }
    
    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }
    
    public void setBuzzedInFirst(boolean status) {
        this.buzzedInFirst = status;
    }
    
    public boolean hasBuzzedInFirst() {
        return buzzedInFirst;
    }

    @Override
    public void run() {
        try {
            // Send welcome message with client ID
            out.println("WELCOME:" + clientId);
            System.out.println("Sent welcome message to " + clientId);
            
            // Send first question
            sendNextQuestion();
            
            // Main game loop
            String clientMessage;
            while ((clientMessage = in.readLine()) != null) {
                System.out.println("Received from " + clientId + ": " + clientMessage);
                if(gameOver) {
                    break;
                 }
                // Process client messages
                if (clientMessage.startsWith("ANSWER:")) {
                    handleAnswer(clientMessage);
                } else if (clientMessage.equals("BUZZ")) {
                    System.out.println(clientId + " buzzed in! (TCP message)");
                    // This is unlikely to happen as buzz should come via UDP
                }
            }
        } catch (IOException e) {
            System.out.println(clientId + " disconnected unexpectedly: " + e.getMessage());
        } finally {
            try {
                // Clean up resources
                clientSocket.close();
                activeClients.remove(this);
                System.out.println(clientId + " connection closed");
                printScores();
            } catch (IOException e) {
                System.err.println("Error closing client socket: " + e.getMessage());
            }
        }
    }

    private void sendNextQuestion() {
        if(gameOver){
            return;
        }
        if (currentQuestionIndex < questions.size()) {
            out.println("QUESTION:" + questions.get(currentQuestionIndex));
            System.out.println("Sent question " + (currentQuestionIndex + 1) + " to " + clientId);
            currentQuestionIndex++;
            // Reset buzz status for new question
            buzzedInFirst = false;
        } else {
            gameOver = true;

            String winnerID = null;
            int winnerScore = Integer.MIN_VALUE;

            synchronized (activeClients) {
                for (ClientThread client : activeClients) {
                    // Check if the client has a higher score
                    if (clientScores.containsKey(client.getClientId())) {
                        int clientScore = clientScores.get(client.getClientId());
                        if (clientScore > winnerScore) {
                            winnerScore = clientScore;
                            winnerID = client.getClientId();
                        }
                    }
                }

                String gameOverMessage = "GAME_OVER:WINNER:" + winnerID + ":" + winnerScore;

                synchronized (activeClients) {
                    for (ClientThread client : activeClients) {
                        client.getWriter().println(gameOverMessage);
                    }
                }

                System.out.println("Game over. Winner: " + winnerID + " with score: " + winnerScore);
            }

            // out.println("GAME_OVER:Final score: " + clientScores.get(clientId));
            // System.out.println(clientId + " completed all questions with score: " + 
            //     clientScores.get(clientId));
        }
    }

    public static void printScores() {
        System.out.println("\nCurrent Scores:");
        clientScores.forEach((client, score) -> 
            System.out.println(client + ": " + score));
        System.out.println();
    }

    private void handleAnswer(String message) {
        if (currentQuestionIndex <= 0 || currentQuestionIndex > questions.size()) {
            out.println("ERROR:Invalid question index");
            return;
        }
        
        // Extract answer
        String answer;
        if (message.equals("ANSWER:TIMEOUT")) {
            // Handle timeout penalty
            clientScores.put(clientId, clientScores.get(clientId) - 20);
            out.println("WRONG:Your score is now " + clientScores.get(clientId));
            printScores();
            sendNextQuestion();
            return;
        } else {
            answer = message.substring("ANSWER:".length());
        }
        
        // Get correct answer from the question (format: Q#|Question|Option1|Option2|Option3|Option4|CorrectAnswer)
        String[] questionParts = questions.get(currentQuestionIndex - 1).split("\\|");
        String correctAnswer = questionParts[6];  // The 7th part is the correct answer (index 6)
        
        System.out.println(clientId + " answered: " + answer);
        System.out.println("Correct answer was: " + correctAnswer);
        
        // Check if answer is correct
        if (answer.trim().equals(correctAnswer.trim())) {
            clientScores.put(clientId, clientScores.get(clientId) + 10);
            out.println("CORRECT:Your score is now " + clientScores.get(clientId));
            System.out.println(clientId + " answered correctly (+10 points)");
        } else {
            clientScores.put(clientId, clientScores.get(clientId) - 10);
            out.println("WRONG:Your score is now " + clientScores.get(clientId));
            System.out.println(clientId + " answered incorrectly (-10 points)");
        }
        
        printScores(); // Update scores after each answer
        sendNextQuestion();
    }
    
    public void notifyTimeout() {
        // Notify all clients to move to next question
        out.println("NEXT");
    }
}