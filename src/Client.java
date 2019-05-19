/*
 * A client that connects to a game server that allows the user to play a
 * guessing game.
 *
 * Copyright (c) 2019 Tim Davis
 */
import java.io.*;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Class that handles the user playing the guessing game.
 *
 * @author Timothy Davis
 * @version 1.0
 * @since 20 May 2019
 */
public class Client extends Thread{

    private final int PORT = 61005; //server port
    private final String ADDRESS = "localhost"; //server address

    private Socket socket; //server socket
    private InputStream is;
    private InputStreamReader isr;
    private BufferedReader br;
    private BufferedWriter bw;
    private OutputStreamWriter osw;
    private OutputStream os;
    private Scanner scanner;

    /*
     *used to connect to server when available
     */
    private boolean hasConnected = false;

    /**
     * Client constructor that create all user required methods to play
     */
    private Client() {
        //loops until the client can connect to the server
        while(!hasConnected) {
            try {
                socket = new Socket(ADDRESS, PORT);
                System.out.println("You have connected to the server");
                hasConnected = true; //used to break the loop
            } catch (Exception e) {
                System.out.println("You cannot connect to the server. Trying again...");

                //sleep so the client isn't abusing the network
                try {
                    Thread.sleep(2000);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
        //create our new thread
        Thread t = new Thread(() -> {
            try {
                setup();

                listen();

            } catch (Exception e) {
                //any catch will disconnect the client from the server
                System.out.println("You have been disconnected from the server.");
            } finally {
                try {
                    //close it all up
                    is.close();
                    isr.close();
                    br.close();
                    os.close();
                    osw.close();
                    bw.close();
                    scanner.close();
                } catch (IOException ioe){
                    System.out.println("Error closing");
                }
            }
        });
        //start er up!
        t.start();
    }

    /**
     * Setup method to assign IO variables
* @throws IOException Catches IO exceptions
     */
    private void setup() throws IOException {
        //Reading the message from the server
        is = socket.getInputStream();
        isr = new InputStreamReader(is);
        br = new BufferedReader(isr);

        //Sending the response back to the client.
        os = socket.getOutputStream();
        osw = new OutputStreamWriter(os);
        bw = new BufferedWriter(osw);

        scanner = new Scanner(System.in);

        sendName();
    }

    /**
     * Sends user's name to the server and receives accepted name
     * @throws IOException Catches IO exceptions
     */
    private void sendName() throws IOException {
        //loops until a valid name is accepted
        while(true) {
            String message = br.readLine();

            //if name is accepted, move along
            if(message.equals("MESSAGE ")){
                break;
            }

            System.out.println(message);

            bw.write(scanner.next() + "\n");

            bw.flush();
        }
    }

    /**
     * Listens for input from the server and processes the message
     * @throws IOException Catches IO exceptions
     */
    private void listen() throws IOException {
        //loops until end of game or quit
        while(true) {
            String message = br.readLine();

            //handles messages from the server
            if (message.startsWith("MESSAGE")) {
                System.out.println(message.replace("MESSAGE ", ""));

            } else if (message.equals("1")) {
                sendGuess();

            } else if (message.startsWith("END")) {
                System.out.println(message.replace("END ", ""));

            } else if (message.equals("QUIT")) {
                return;
            }
        }
    }

    /**
     * Sends the user input to the Server for action
     * @throws IOException Catches IO exceptions
     */
    private void sendGuess() throws IOException {
        //guess is -1 for oob error from server. Acts as server side error check
        int guess = -1;

        //error in user input
        boolean error;

        do {
            try {
                String input = scanner.next();

                //checks for quit message and tells server for graceful exit.
                if(input.equals("/quit"))
                {
                    bw.write(input + "\n");

                    bw.flush();

                    return;
                }

                //turns guess into int for client side checking
                guess = Integer.parseInt(input);

                error = false;

            } catch (InputMismatchException e) {
                System.out.println("Input was NaN. Try again.");

                error = true;
            }
        } while (error);

        String sendMessage = guess + "\n";

        //sends out message
        bw.write(sendMessage);

        bw.flush();

        //reads in server response
        String message = br.readLine();

        System.out.println(message);
    }

    /**
     * Main method to instantiate client thread(s)
     * @param args main arguments
     */
    public static void main(String[] args) {
        Thread[] threads = {
                new Client()
        };

        for(Thread t : threads){
            t.start();
        }
    }

}