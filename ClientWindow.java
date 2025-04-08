import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketException;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.*;

public class ClientWindow implements ActionListener {
    // GUI Components
    private JButton poll;
    private JButton submit;
    private JRadioButton[] options;
    private ButtonGroup optionGroup;
    private JLabel question;
    private JLabel timer;
    private JLabel score;
    private JFrame window;
    
    // Game Logic
    private TimerTask clock;
    private Timer timerInstance;
    private int currentQuestionIndex = 0;
    private int currentScore = 0;
    
    // Network
    private PrintWriter out;
    private BufferedReader in;
    private ClientNetwork clientNetwork;
    private String clientID;  // Changed from final to modifiable

    public ClientWindow(PrintWriter out, BufferedReader in, String clientID, String serverIP) {
        this.out = out;
        this.in = in;
        this.clientID = clientID;  // This will be updated when server sends WELCOME message
        
        try {
            this.clientNetwork = new ClientNetwork(clientID, serverIP);
            System.out.println("Client network initialized with ID: " + clientID);
        } catch (SocketException e) {
            JOptionPane.showMessageDialog(null, "Network error: " + e.getMessage());
            e.printStackTrace();
        }
        
        initializeGUI();
        startListeningToServer();
    }

    private void initializeGUI() {
        window = new JFrame("Trivia Game - " + clientID);
        window.setLayout(null);
        window.setSize(400, 400);
        window.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Add window close listener
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                cleanupResources();
            }
        });

        // Question Label
        question = new JLabel("Waiting for question...");
        question.setBounds(10, 5, 350, 100);
        window.add(question);

        // Radio Buttons
        options = new JRadioButton[4];
        optionGroup = new ButtonGroup();
        for (int i = 0; i < options.length; i++) {
            options[i] = new JRadioButton();
            options[i].setBounds(10, 110 + (i * 30), 350, 20);
            options[i].addActionListener(this);
            optionGroup.add(options[i]);
            window.add(options[i]);
            options[i].setEnabled(false);  // Disabled initially
        }

        // Timer
        timer = new JLabel("--");
        timer.setBounds(250, 250, 100, 20);
        window.add(timer);

        // Score
        score = new JLabel("SCORE: 0");
        score.setBounds(50, 250, 100, 20);
        window.add(score);

        // Buttons
        poll = new JButton("Poll (Buzz)");
        poll.setBounds(10, 300, 120, 20);
        poll.addActionListener(this);
        poll.setEnabled(false);  // Disabled until first question arrives
        window.add(poll);

        submit = new JButton("Submit");
        submit.setBounds(200, 300, 100, 20);
        submit.addActionListener(this);
        submit.setEnabled(false);  // Disabled initially
        window.add(submit);

        window.setVisible(true);
    }

    private void startListeningToServer() {
        new Thread(() -> {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received from server: " + message);
                    handleServerMessage(message);
                }
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(window, "Disconnected from server");
                    window.dispose();
                });
            }
        }).start();
    }

    private void handleServerMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message == null) {
                JOptionPane.showMessageDialog(window, "Disconnected from server");
                window.dispose();
                return;
            }else if (message.equals("NEXT")) {
                // Handle next question notification
                JOptionPane.showMessageDialog(window, "Moving to next question...");
                stopTimer();
                poll.setEnabled(true); // Re-enable the poll button for the next question
            }
    
            // Handle different message types
            if (message.startsWith("WELCOME:")) {
                String[] parts = message.split(":");
                if (parts.length > 1) {
                    // Update the client ID to use the server-assigned ID
                    this.clientID = parts[1];
                    // Update ClientNetwork with new ID for UDP messages
                    clientNetwork.updateClientID(this.clientID);
                    // Update window title
                    window.setTitle("Trivia Game - " + this.clientID);
                    System.out.println("Server assigned ID: " + this.clientID);
                }
            }
            else if (message.startsWith("QUESTION:")) {
                // Extract and display question with options
                String questionData = message.substring("QUESTION:".length());
                String[] parts = questionData.split("\\|");
                
                if (parts.length >= 6) {
                    question.setText("<html>" + parts[0] + ". " + parts[1] + "</html>");
                    for (int i = 0; i < options.length && i < 4; i++) {
                        options[i].setText(parts[i + 2]);
                        options[i].setEnabled(false); // Disable until buzzed in
                    }
                    optionGroup.clearSelection();
                    stopTimer();
                    startTimer(15); // Start question timer
                    poll.setEnabled(true);
                    submit.setEnabled(false);
                    currentQuestionIndex++;
                }
            }
            else if (message.startsWith("CORRECT:")) {
                // Handle correct answer
                String[] parts = message.split(":");
                if (parts.length > 1) {
                    currentScore = Integer.parseInt(parts[1].trim().split(" ")[4]);
                } else {
                    currentScore += 10;
                }
                score.setText("SCORE: " + currentScore);
                JOptionPane.showMessageDialog(window, "Correct! Your score is now " + currentScore);
                optionGroup.clearSelection();
                stopTimer();
            }
            else if (message.startsWith("WRONG:")) {
                // Handle wrong answer
                String[] parts = message.split(":");
                if (parts.length > 1) {
                    currentScore = Integer.parseInt(parts[1].trim().split(" ")[4]);
                } else {
                    currentScore = Math.max(0, currentScore - 10); // Prevent negative score
                }
                score.setText("SCORE: " + currentScore);
                JOptionPane.showMessageDialog(window, "Wrong answer! Your score is now " + currentScore);
                optionGroup.clearSelection();
                stopTimer();
            }
            else if (message.startsWith("GAME_OVER")) {
                String finalMessage = "Game Over!";
                if (message.contains(":")) {
					String[] parts = message.split(":");
					if (parts.length >= 3 && parts[1].equals("WINNER")) {
						String winnerID = parts[2];
						String winnerScore = parts.length > 3 ? parts[3] : "0";
						JPanel gameOverMessage = new JPanel();

						if(winnerID.equals(clientID)) {
							//finalMessage = "Winner winner, chicken dinner!\n You won the game with a score of " + winnerScore + "!";
							//JLabel finalMessage = new JLabel("<html><center>Winner winner, chicken dinner!<br>You won the game with a score of \" + winnerScore + \"!</center></html>");
							finalMessage = "<html><center>Winner winner, chicken dinner!<br>You won the game with a score of " + winnerScore + "!</center></html>";
							//finalMessage.setFont(boldCustomFont.deriveFont(18f));
						} else {
							//finalMessage = "L is for Loser! \n You lost the game with a score of " + currentScore + "!\n The winner is " + winnerID + " with a score of " + winnerScore + "!";
							finalMessage = "<html><center>L is for Loser!<br>You lost the game with a score of " + currentScore + "<br>The winner is " + winnerID + " with a score of " + winnerScore + "!</center></html>";

						}
					}

                    //finalMessage = message.substring(message.indexOf(":") + 1);
                }
                
				// JOptionPane.showMessageDialog(window, finalMessage);
                

				JLabel gameOverLabel = new JLabel(finalMessage);
				//gameOverLabel.setFont(boldCustomFont.deriveFont(18f));
				gameOverLabel.setHorizontalAlignment(SwingConstants.CENTER);
				
				JOptionPane.showMessageDialog(window, gameOverLabel, "Game Over", JOptionPane.PLAIN_MESSAGE);
				
                cleanupResources();
                window.dispose();
            }
            else if (message.equals("ACK")) {
                // Handle buzz-in acknowledgment
                JOptionPane.showMessageDialog(window, "You buzzed in first! Select your answer.");
                for (JRadioButton option : options) {
                    option.setEnabled(true);
                }
                submit.setEnabled(true);
                poll.setEnabled(false);
                stopTimer(); // Stop the question timer
                startTimer(10); // 10 seconds to answer once buzzed in
            }
            else if (message.equals("NEGATIVE-ACK")) {
                // Handle negative acknowledgment
                JOptionPane.showMessageDialog(window, "Someone else buzzed in first!");
                poll.setEnabled(false);
            }
            else if (message.equals("NEXT")) {
                // Handle next question notification
                JOptionPane.showMessageDialog(window, "Moving to next question...");
                stopTimer();
            }
            else {
                // Handle unrecognized messages
                System.out.println("Unknown message from server: " + message);
            }
        });
    }

    private void startTimer(int seconds) {
        stopTimer(); // Stop any existing timer
        
        timerInstance = new Timer();
        clock = new TimerCode(seconds);
        timerInstance.schedule(clock, 0, 1000);
    }
    
    private void stopTimer() {
        if (timerInstance != null) {
            timerInstance.cancel();
            timerInstance = null;
        }
        if (clock != null) {
            clock.cancel();
            clock = null;
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == poll) {
            try {
                clientNetwork.sendBuzz(currentQuestionIndex);
                System.out.println("Sent buzz for question " + currentQuestionIndex + " with ID " + clientID);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(window, "Error sending buzz: " + ex.getMessage());
                ex.printStackTrace();
            }
        } 
        else if (e.getSource() == submit) {
            String selectedOption = getSelectedOption();
            if (selectedOption != null) {
                out.println("ANSWER:" + selectedOption);
                System.out.println("Submitted answer: " + selectedOption);
            }
        }
    }

    private String getSelectedOption() {
        for (JRadioButton option : options) {
            if (option.isSelected()) return option.getText();
        }
        JOptionPane.showMessageDialog(window, "Please select an option!");
        return null;
    }
    
    private void cleanupResources() {
        stopTimer();
        if (clientNetwork != null) {
            clientNetwork.close();
        }
    }

    // Timer inner class
    private class TimerCode extends TimerTask {
        private int duration;

        public TimerCode(int duration) {
            this.duration = duration;
        }

        @Override
        public void run() {
            SwingUtilities.invokeLater(() -> {
                if (duration <= 0) {
                    timer.setText("0");
                    timer.setForeground(Color.RED);
                    
                    // If time runs out and the client was allowed to answer
                    if (submit.isEnabled()) {
                        out.println("ANSWER:TIMEOUT");  // Inform server about timeout
                        JOptionPane.showMessageDialog(window, "Time's up! -20 points penalty.");
                    }
                    
                    poll.setEnabled(true); // Re-enable the poll button after timeout
                    submit.setEnabled(false);
                    for (JRadioButton option : options) {
                        option.setEnabled(false);
                    }
                    
                    cancel();
                    return;
                }
                
                timer.setText(String.valueOf(duration));
                timer.setForeground(duration <= 5 ? Color.RED : Color.BLACK);
                duration--;
            });
        }
    }
}