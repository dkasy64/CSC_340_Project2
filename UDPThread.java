import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.Queue;

public class UDPThread implements Runnable {
    private final int port;
    private final Queue<String> udpQueue;
    private final List<ClientThread> activeClients;
    
    public UDPThread(int port, Queue<String> udpQueue, List<ClientThread> activeClients) {
        this.port = port;
        this.udpQueue = udpQueue;
        this.activeClients = activeClients;
    }
    
    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("UDP listener started on port " + port);
            byte[] buffer = new byte[1024];
            
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                
                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("UDP received: " + message);
                
                // Process BUZZ messages
                if (message.startsWith("BUZZ:")) {
                    handleBuzz(message);
                }
            }
        } catch (IOException e) {
            System.err.println("UDP thread error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void handleBuzz(String message) {
        System.out.println("DEBUG - Handling buzz: " + message);
        try {
            // Format expected: BUZZ:ClientID:QuestionNumber
            String[] parts = message.split(":");
            if (parts.length >= 3) {
                String clientId = parts[1];
                int questionNumber = Integer.parseInt(parts[2]);
                
                System.out.println("Processing buzz from " + clientId + " for question " + questionNumber);
                
                // Print all active client IDs for debugging
                System.out.println("Currently active clients:");
                for (ClientThread client : activeClients) {
                    System.out.println("- " + client.getClientId() + " (on question " + client.getCurrentQuestionIndex() + ")");
                }
                
                // Find the client who buzzed in first for this question
                synchronized (activeClients) {
                    ClientThread buzzedClient = null;
                    
                    // Find the client who buzzed in
                    for (ClientThread client : activeClients) {
                        if (client.getClientId().equals(clientId)) {
                            buzzedClient = client;
                            System.out.println("Found client thread for " + clientId);
                            System.out.println("Client question index: " + client.getCurrentQuestionIndex());
                            break;
                        }
                    }
                    
                    if (buzzedClient != null) {
                        // Send ACK to the client who buzzed in first
                        buzzedClient.getWriter().println("ACK");
                        System.out.println("Sent ACK to " + clientId + " for question " + questionNumber);
                        
                        // Send NEGATIVE-ACK to all other clients on the same question
                        for (ClientThread client : activeClients) {
                            if (!client.getClientId().equals(clientId) && 
                                client.getCurrentQuestionIndex() == questionNumber) {
                                client.getWriter().println("NEGATIVE-ACK");
                                System.out.println("Sent NEGATIVE-ACK to " + client.getClientId());
                            }
                        }
                    } else {
                        System.out.println("Could not find client thread for " + clientId);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing buzz: " + e.getMessage());
            e.printStackTrace();
        }
    }
}