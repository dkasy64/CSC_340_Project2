import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

public class ClientNetwork {
    private DatagramSocket udpSocket;
    private String clientID;
    
    public ClientNetwork(String clientID) throws SocketException {
        this.clientID = clientID;
        this.udpSocket = new DatagramSocket();
    }
    
    // Add method to update client ID when server assigns one
    public void updateClientID(String newClientID) {
        this.clientID = newClientID;
        System.out.println("Updated client ID to server-assigned ID: " + newClientID);
    }
    
    public void sendBuzz(int questionNumber) throws IOException {
        String message = "BUZZ:" + clientID + ":" + questionNumber;
        byte[] buffer = message.getBytes();
        DatagramPacket packet = new DatagramPacket(
            buffer,
            buffer.length,
            InetAddress.getByName("localhost"),
            9192  // Match server's UDP port
        );
        System.out.println("Sending UDP buzz: " + message);
        udpSocket.send(packet);
    }
    
    public void close() {
        if (udpSocket != null && !udpSocket.isClosed()) {
            udpSocket.close();
        }
    }
}