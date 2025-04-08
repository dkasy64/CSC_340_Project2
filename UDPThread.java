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
            String[] parts = message.split(":");
            if (parts.length >= 3) {
                String clientId = parts[1];
                int questionNumber = Integer.parseInt(parts[2]);
    
                synchronized (activeClients) {
                    // Check if anyone has already buzzed in for this question
                    boolean alreadyBuzzed = false;
                    for (ClientThread client : activeClients) {
                        if (client.hasBuzzedInFirst()) {
                            alreadyBuzzed = true;
                            break;
                        }
                    }
                    
                    ClientThread buzzedClient = null;
                    // Find the client who buzzed in
                    for (ClientThread client : activeClients) {
                        if (client.getClientId().equals(clientId)) {
                            buzzedClient = client;
                            break;
                        }
                    }
    
                    if (buzzedClient != null) {
                        // If no one has buzzed in yet, this client is first
                        if (!alreadyBuzzed) {
                            buzzedClient.setBuzzedInFirst(true);
                            buzzedClient.getWriter().println("ACK");
                            System.out.println(clientId + " buzzed in first for question " + questionNumber);
                            
                            // Send NEGATIVE-ACK to all other clients
                            for (ClientThread client : activeClients) {
                                if (!client.getClientId().equals(clientId)) {
                                    client.getWriter().println("NEGATIVE-ACK");
                                }
                            }
                        } else {
                            // Someone else already buzzed in
                            buzzedClient.getWriter().println("NEGATIVE-ACK");
                            System.out.println(clientId + " buzzed in but was not first");
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing buzz: " + e.getMessage());
            e.printStackTrace();
        }
    }
}