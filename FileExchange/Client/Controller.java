package Client;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class Controller implements ActionListener, DocumentListener, MessageCallback{

    ChatGUI gui;
    private Client client;

    public Controller(ChatGUI gui, Client client) {
        this.gui = gui;
        this.client = client;
        
        this.client.setMessageCallback(this);
        gui.setActionListener(this);
        gui.setDocumentListener(this);
        updateView();
        
    }

    public void updateView() {
        SwingUtilities.invokeLater(() -> gui.getTerminalOut());
        
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        // TODO Auto-generated method stub

       
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // TODO Auto-generated method stub

         if (e.getActionCommand().equals("Enter")){
            // System.out.println(e.getActionCommand());
            // System.out.println(gui.getUserInput());

            client.setLastCmd(gui.getUserInput());
            Boolean validJoin = client.getJoined();
            Boolean validRegister = client.getRegistered();


            // * JOIN Command
            if (gui.getUserInput().trim().startsWith("/join")){

                lastCmdDisplay();

                if (!validJoin){
                    // get port
                    String portCharacters = extractPort(gui.getUserInput());
                    System.out.println("Last port characters: " + portCharacters);
                    int port = Integer.parseInt(portCharacters);

                    // get host
                    String hostCharacters =  extractHost(gui.getUserInput());
                    System.out.println("Last host characters: " + hostCharacters);

                    // client creds
                    client.setPort(port);
                    client.setHost(hostCharacters);

                    
                    try {
                        client.setSocket(client.getHost(), client.getPort());
                        // Listen to Server:
                        client.listenForMessage();
                        gui.setUserInput(""); // clear input box
                    } catch (IOException e1) {
                        // e1.printStackTrace();
                        gui.clientTerminalOut("Error: Unsuccessful connection. Check IP address or port");
                        gui.setUserInput("");
                        // gui.clientTerminalOut(client.getJoin().toString());  // checker
                    }
                } else {
                    gui.clientTerminalOut("Error: You are already joined to the server.");
                    gui.setUserInput("");
                }

            
            // * Download Command
            } else if (gui.getUserInput().trim().startsWith("/download")){

                lastCmdDisplay();

                if (validJoin && validRegister){

                    client.sendMessage(gui.getUserInput());
                    client.receiveFile(gui.getUserInput());

                    client.listenForMessage();
                    // Ensure that the listenForMessage thread completes before moving on
                        try {
                            client.getListenThread().join();
                        } catch (InterruptedException e2) {
                            e2.printStackTrace();
                        }
                } else {
                    gui.clientTerminalOut("Error: Invalid command. Make sure you are joined or registered.");
                }

                gui.setUserInput("");


            // * Register Command
            } else if (gui.getUserInput().trim().startsWith("/register")){

                if (validJoin) {
                    int registration = 0; 
                    String name = null;

                    // send message /register to server using client
                    client.sendMessage(gui.getUserInput());
                    registration = client.receiveRegistrationStatus();
                

                    // display last command (differs on other commands because name must not show yet)
                    if (registration == 1){
                        gui.clientTerminalOut(client.getLastCmd()); // display last command without registered name
                        gui.setUserInput("");

                        client.listenForMessage();

                        // Ensure that the listenForMessage thread completes before moving on
                        try {
                            client.getListenThread().join();
                        } catch (InterruptedException e2) {
                            e2.printStackTrace();
                        }

                        // get name from server
                        client.sendMessage("/getName");
                        name = extractName("SERVER:", client.receiveUserName()); // remove "SERVER" from received name
                        System.out.println("Name: " + name);
                
                        setRegistration(registration, name);

                    // already registered/ taken alias
                    } else {
                        lastCmdDisplay();
                        client.listenForMessage();
                        gui.setUserInput("");
                    }
                } else {
                    lastCmdDisplay();
                    gui.setUserInput("");
                    gui.clientTerminalOut("Error: Invalid command. Make sure you are joined or registered.");
                }
                
            // * /? or Help command
            } else if (gui.getUserInput().trim().startsWith("/?")) {
                lastCmdDisplay();
                gui.clientTerminalOut("Commands: /?, /join, /register, /leave, /get, /store, /dir");
                gui.setUserInput("");
                
            } else {
                lastCmdDisplay();
                gui.clientTerminalOut("Error: Command not found.");
                gui.setUserInput("");
            }
         } 
    }   

    //*  Displays last command of user in Terminal
    public void lastCmdDisplay(){
        if (client.getRegistered() == false){
            gui.setTerminalOut( client.getLastCmd(), "", client.getRegistered());
        } else {
            gui.setTerminalOut( client.getLastCmd(), client.getName(), client.getRegistered());
        }
    }   

    // * Register client registration in client
    public void setRegistration(int status, String name){
        System.out.println("Set Registration (Controller)");
        if (status == 1){
            client.setRegistered(true);
            client.setName(name);
        } else if (status == 0){
            client.setRegistered(false);
        }
    }

    //* Displays server response on gui.
    @Override
    public void onMessageReceived(String message) {
        // Update GUI with the received message
        SwingUtilities.invokeLater(() -> {
            gui.clientTerminalOut(message);
        });
    }


    //* Extract Port from user input
    private static String extractPort(String input) {
        // Regular expression to match numbers after the last space in the input string
        String regex = "\\s(\\d+)$"; // Matches one or more digits after the last space

        // Create a Pattern object
        Pattern pattern = Pattern.compile(regex);

        // Create a Matcher object
        Matcher matcher = pattern.matcher(input);

        // Find the last match (numbers after the last space)
        String portExtracted = null;
        if (matcher.find()) {
            portExtracted = matcher.group(1); // Group 1 contains the matched digits
        }

        return portExtracted;
    }

    // * Extract Host from user input
    private static String extractHost(String input) {
        // Regular expression to match the string after "/join" and before the second space
        String regex = "/join\\s(\\S+)"; // Matches "/join", a space, and captures the string before the second space

        // Create a Pattern object
        Pattern pattern = Pattern.compile(regex);

        // Create a Matcher object
        Matcher matcher = pattern.matcher(input);

        // Find the match
        String hostExtracted = null;
        if (matcher.find()) {
           hostExtracted = matcher.group(1); // Group 1 contains the captured string
        }

        return hostExtracted;
    }

     // * Extract Name from user input
     private static String extractName(String key, String input) {
        // Define a regex pattern to match "/register" followed by a space and capture the word
       // The word is captured in a capturing group (indicated by parentheses)
       String regex = key + "\\s+(\\w+)";
       
       // Create a Pattern object from the regex
       Pattern pattern = Pattern.compile(regex);
       
       // Create a Matcher object for the input sentence
       Matcher matcher = pattern.matcher(input);
       
       // Check if the pattern is found in the input sentence
       if (matcher.find()) {
           // Group 1 of the matcher contains the captured word
           return matcher.group(1);
       } else {
           // Return an empty string or handle the case when "/register" is not found
           return "";
       }
   }
    
}
