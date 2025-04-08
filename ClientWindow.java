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
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

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
    private Font customFont;
	private Font boldCustomFont;
	private Font italicCustomFont;
	private Color backgroundColor;
	private Color titleColor;
	private Color correctAnswerColor;
	private Color wrongAnswerColor;
    
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
        

		customFont = CustomFont.loadCustomFont("/font/x12y12pxMaruMinyaM.ttf").deriveFont(21f);
		boldCustomFont = customFont.deriveFont(Font.BOLD);
		italicCustomFont = customFont.deriveFont(Font.ITALIC);
		backgroundColor = new Color(221, 251, 248);
		titleColor = new Color(13, 203, 202);
		correctAnswerColor = new Color(7, 139, 31);
		wrongAnswerColor = new Color(225, 0, 0);
        
        JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

		JLabel messageLabel = new JLabel(" Welcome to CSC340 Trivia!");
		messageLabel.setFont(boldCustomFont.deriveFont(50f));

		JLabel testKnowledgeLabel = new JLabel("Are you ready to test your knowledge of CSC340?");
		testKnowledgeLabel.setFont(boldCustomFont.deriveFont(30f));

		//Names are in alphabetical order btw :)
		JLabel namesLabel = new JLabel("Created by: Dawit Kasy, Tuana Turhan, Natalie Spiska");
		namesLabel.setFont(italicCustomFont.deriveFont(25f));

		messageLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		messageLabel.setForeground(titleColor);
		testKnowledgeLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		namesLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);

		panel.add(messageLabel);
		panel.add(Box.createVerticalStrut(10));
		panel.add(testKnowledgeLabel);
		panel.add(Box.createVerticalStrut(10));
		panel.add(namesLabel);

		//All of this is to delete the stupid button
		////////////////////////////////////////////////////////////////////////////////////////
		//JOptionPane.showMessageDialog(null, panel, "Welcome", JOptionPane.PLAIN_MESSAGE);

		JOptionPane optionPane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION);
		JDialog dialog = optionPane.createDialog("Welcome");
		//removes the OK button from the screen
		optionPane.setOptions(new Object[] {});

		JButton customButton = new JButton("I'm ready!");
		customButton.setBackground(titleColor);
		customButton.setFont(customFont.deriveFont(20f));
		customButton.addActionListener(e -> dialog.dispose());

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		buttonPanel.add(customButton);

		dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		dialog.setSize(800, 230);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);

		////////////////////////////////////////////////////////////////////////////////////////


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
        window.getContentPane().setBackground(backgroundColor);
		window.setFont(boldCustomFont.deriveFont(12f));
        window.setLayout(null);
        window.setSize(400, 400);	
		window.setLocationRelativeTo(null);
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
        timer.setFont(customFont.deriveFont(16f));
        timer.setBounds(250, 250, 100, 20);
        window.add(timer);

        // Score
        score = new JLabel("SCORE: 0");
        score.setFont(customFont.deriveFont(16f));
        score.setBounds(50, 250, 100, 20);
        window.add(score);

        // Buttons
        poll = new JButton("Poll (Buzz)");
        poll.setFont(customFont.deriveFont(12f));
        poll.setBounds(10, 300, 120, 40);
        poll.addActionListener(this);
        poll.setEnabled(false);  // Disabled until first question arrives
        window.add(poll);

        submit = new JButton("Submit");
        submit.setFont(customFont.deriveFont(12f));
        submit.setBounds(200, 300, 120, 40);
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

                     GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
                     ge.registerFont(customFont);

                    //question.setText("<html>" + parts[0] + ". " + parts[1] + "</html>");
                    question.setText("<html><span style=\"font-family: 'x12y12pxMaruMinyaM'; font-size: 16px;\">" + parts[0] + ". " + parts[1] + "</span></html>");
                    for (int i = 0; i < options.length && i < 4; i++) {
                        options[i].setFont(customFont.deriveFont(12f));
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
                JLabel correctLabel = new JLabel("Correct! Your score is now " + currentScore);
				correctLabel.setFont(boldCustomFont.deriveFont(18f));
				correctLabel.setForeground(correctAnswerColor);	

				JPanel correctPanel = new JPanel();
				correctPanel.add(correctLabel);

				JOptionPane.showMessageDialog(window, correctPanel, "Buzzed In", JOptionPane.PLAIN_MESSAGE);

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
                JLabel wrongLabel = new JLabel("Wrong answer! Your score is now " + currentScore);
				wrongLabel.setFont(boldCustomFont.deriveFont(18f));
				wrongLabel.setForeground(wrongAnswerColor);	

				JPanel wrongPanel = new JPanel();
				wrongPanel.add(wrongLabel);
				
                JOptionPane.showMessageDialog(window, wrongPanel, "Buzzed In", JOptionPane.PLAIN_MESSAGE);

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
                }
                
				// JOptionPane.showMessageDialog(window, finalMessage);

				JLabel gameOverLabel = new JLabel(finalMessage);
				gameOverLabel.setFont(boldCustomFont.deriveFont(18f));
				gameOverLabel.setHorizontalAlignment(SwingConstants.CENTER);
				
				JOptionPane.showMessageDialog(window, gameOverLabel, "Game Over", JOptionPane.PLAIN_MESSAGE);
				
                cleanupResources();
                window.dispose();
            }
            else if (message.equals("ACK")) {
                // Handle buzz-in acknowledgment
                JLabel buzzLabel = new JLabel("You buzzed in first! Select your answer.");
				buzzLabel.setFont(boldCustomFont.deriveFont(18f)); 
				buzzLabel.setForeground(Color.BLACK);

				JPanel buzzPanel = new JPanel();
				buzzPanel.add(buzzLabel);

				JOptionPane.showMessageDialog(window, buzzPanel, "Buzzed In", JOptionPane.PLAIN_MESSAGE);
			
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
               ////////// 
				JLabel otherBuzzLabel = new JLabel("Someone else buzzed in first!");
				otherBuzzLabel.setFont(boldCustomFont.deriveFont(18f)); 
				otherBuzzLabel.setForeground(Color.BLACK);

				JPanel otherBuzzPanel = new JPanel();
				otherBuzzPanel.add(otherBuzzLabel);

				JOptionPane.showMessageDialog(window, otherBuzzPanel, "Buzzed In", JOptionPane.PLAIN_MESSAGE);
				/////////// 
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