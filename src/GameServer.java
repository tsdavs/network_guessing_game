/*
 * A game server that accepts clients to play a guessing game
 *
 * Copyright (c) 2019 Tim Davis
 */
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Class that handles the game server instantiation.
 *
 * @author Timothy Davis
 * @version 1.0
 * @since 20 May 2019
 */
public class GameServer
{
    private final int MAXNUMPLAYERS = 99; //max players including queue
    private final int PORT = 61005; //server port

    private ServerSocket serverSocket; //server socket ref

    /**
     * Game Server constructor that create all game server required
     * methods to start
     */
    private GameServer()
    {
        try {
            serverSocket = new ServerSocket(PORT);

            System.out.println(new Date().toString() + ": Server running");
        } catch (IOException ioe){
            ioe.printStackTrace();
        }

        //executor pool to allow clients.
        ExecutorService pool = Executors.newFixedThreadPool(MAXNUMPLAYERS);

        try{
            //creates a new game class
            Game game = new Game();

            //adds new clients to the pool until its full.
            for (;;) {
                pool.execute(game.new Player(serverSocket.accept()));
            }

        } catch (IOException ioe){
            pool.shutdown();
        }

    }

    /**
     * Main method to instantiate GameServer
     * @param args main arguments
     */
    public static void main(String [] args)
    {
        new GameServer();
    }
}

/**
 * Game class that handles the guessing game and players inside
 */
class Game {

    /*
     * Separate Set for unique player name checking
     */
    private static Set<String> names = new HashSet<>();

    /*
     * Clients outputstreams for broadcast messages
     */
    private static Set<BufferedWriter> bws = new HashSet<>();

    /*
     * Player vector to store all players
     */
    private static Vector<Player> players = new Vector<>();

    private final int MAX = 9; //random number max
    private final int MIN = 0; //random number min
    //random number between MIN and MAX
    private final int RANDOMNUM = (int) (Math.random() * (MAX - MIN) + 1) + MIN;
    private final int MAXNUMPLAYERS = 2;
    private final int MAXATTEMPTS = 4;
    private final int MAXTIMEOUTCOUNTER = 10; //timeout for players to join

    private int globalAttempts = 0; //attempts allowed by all players combined
    private int numPlayers = 0; //amount of players

    /**
     * Game constructor that creates a random number
     */
    Game(){
        System.out.println(new Date().toString() + ": Random number is: " + RANDOMNUM);
    }

    /**
     * Clients submit guesses to the game to synchronise global attempts
     */
    private synchronized void guess(){
        ++globalAttempts;
    }

    /**
     * Broadcast a message to all players
     * @param message message to be broadcast to all players
     * @throws IOException Catches IO exceptions
     */
    private void broadcastMessage(String message) throws IOException{
        for(BufferedWriter bw : bws){
            bw.write(message);
            bw.flush();
        }
    }

    /**
     * Player object thread instantiated at new client connection
     */
    class Player implements Runnable {
        /*
         * players hold own player ranking vector for end of game
         */
        Vector<Player> rankedPlayers;

        private int timeOutCounter = 0; //time out for joining matches
        private int attempt = 0; //attempt counter

        private Socket clientSocket; //unique client socket
        private String playerName; //players name
        private InputStream is;
        private InputStreamReader isr;
        private BufferedReader br;
        private BufferedWriter bw;
        private OutputStreamWriter osw;
        private OutputStream os;

        /*
         * is the player correctly guesses, this is true
         */
        private boolean correctGuess = false;

        /*
         * used for when the player has used all attempts
         */
        private boolean isFinished = false;

        /*
         * used to notify if the player has disconnected
         */
        private boolean hasDisconnected = false;

        /**
         * Player constructor used to define unique client socket
         * @param s client socket
         */
        Player(Socket s) {
            clientSocket = s;
        }

        /**
         * Runnable loop for Player thread used as main action center
         */
        @Override
        public void run() {
            try{
                setup();

                processPlayerName();

                waitForPlayers();

                listen();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //player left
                try{
                    close(hasDisconnected);
                } catch (IOException ioe){
                    System.out.println(new Date().toString()
                            + ": error removing: " + playerName);
                }
            }
        }

        /**
         * Used to setup Players input and output streams.
         * @throws IOException Catches IO exceptions
         */
        private void setup() throws IOException{
            //Reading the message from the client
            is = clientSocket.getInputStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);

            //Sending the response back to the client.
            os = clientSocket.getOutputStream();
            osw = new OutputStreamWriter(os);
            bw = new BufferedWriter(osw);
        }

        /**
         * Processes the players name input from the client
         * @throws IOException Catches IO exceptions
         */
        private void processPlayerName() throws IOException{
            //loops until the name has accepted
            while (true) {
                sendMessage("Enter your name: \n");

                playerName = br.readLine();

                if (playerName == null) {
                    return;
                }

                //checks to see if the name has been used by another player
                synchronized (names) {
                    if (!playerName.isBlank() && !names.contains(playerName)) {
                        ++numPlayers;

                        //adds player to queue if already enough players
                        if(numPlayers > MAXNUMPLAYERS){
                            addPlayerToQueue();
                            sendMessage("MESSAGE You have joined the game \n");
                        }

                        System.out.println(new Date().toString() + ": " +
                                playerName + " at " + clientSocket.getPort()
                                + " added to game");

                        //adds the players name to the names list
                        names.add(playerName);
                        //adds this to the player vector
                        players.add(this);
                        //lets the client start receiving messages
                        sendMessage("MESSAGE \n");

                        break;
                    }
                }
            }

            //tell other players someone has joined
            broadcastMessage("MESSAGE " + playerName + " has joined \n");

            //adds current bw to the broadcast group
            bws.add(bw);
        }

        /**
         * For players that have to wait for minimum players to play game
         * @throws IOException Catches IO exceptions
         */
        private void waitForPlayers() throws IOException {
            //loops until enough players
            while(numPlayers <= MAXNUMPLAYERS) {
                if(numPlayers == MAXNUMPLAYERS) {
                    //sends first message to start
                    sendMessage("MESSAGE Game starting. Send your guesses between "
                            + MIN +  " and " + MAX + " type '/quit' to forfeit" + "\n");
                    break;
                } else {
                    sendMessage("MESSAGE Waiting for " +
                            (MAXNUMPLAYERS - numPlayers) + " more player(s) \n");
                }

                //sleep to stop spamming user with waiting messages
                try {
                    Thread.sleep(2000);

                    //timeout if not enough players
                    ++timeOutCounter;

                } catch (Exception e){
                    e.printStackTrace();
                }

                // :(
                if(timeOutCounter == MAXTIMEOUTCOUNTER){
                    sendMessage("MESSAGE Timeout. Not enough players\n");

                    sendMessage("QUIT\n");

                    return;
                }
            }
        }

        /**
         * Handles players in the queue
         * @throws IOException Catches IO exceptions
         */
        private void addPlayerToQueue() throws IOException {
            System.out.println(new Date().toString() + ": " + playerName +
                    " at " + clientSocket.getPort() + " added to queue");

            //lets the client start receiving messages
            sendMessage("MESSAGE \n");

            //lets the player know
            sendMessage("MESSAGE You have been added to a queue. \n");

            //loops here until game is over. This acts better than queue object imo
            while (numPlayers > MAXNUMPLAYERS){
                sendMessage("MESSAGE Please wait until the current game is over. \n");

                //sleep to stop spamming client
                try {
                    Thread.sleep(5000);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Main action loop to listen and action client input
         * @throws IOException Catches IO exceptions
         */
        private void listen() throws IOException{
            //loops for user input and action
            while(true){
                if(!isFinished) {
                    //lets client know to keep guessing or not
                    if (!correctGuess || attempt != MAXATTEMPTS) {
                        sendMessage("1\n");
                    } else {
                        sendMessage("0\n");
                    }

                    String message = "";

                    //checks for message from client or disconnection
                    try {
                        message = br.readLine();
                        System.out.println(new Date().toString() +
                                ": Message from " + playerName + " at "
                                + clientSocket.getPort() + " -> " + message);

                    } catch (IOException io) {
                        hasDisconnected = true;
                    }

                    //handles message
                    if(message != null){
                        //quits gracefully
                        if(message.equals("/quit")) {
                            return;
                        }

                        //turns input into int for processing
                        try {
                            int guess = Integer.parseInt(message);

                            processGuess(guess);
                        } catch (NumberFormatException e) {
                            hasDisconnected = true;
                            return;
                        }
                    } else {
                        hasDisconnected = true;
                        return;
                    }
                }
                // Players loop here until all other players are done
                else if (globalAttempts >= (numPlayers * MAXATTEMPTS)
                            || globalAttempts == 0){
                    gameOver();
                    return;
                }
            }
        }

        /**
         * Processes the guess based on higher, lower or equal to random number
         * @param guess Clients guess to process
         * @throws IOException Catches IO exceptions
         */
        private void processGuess(int guess) throws IOException{
            String message;

            String MAR = "Max attempts reached";

            //if can still guess
            if(++attempt <= MAXATTEMPTS) {
                guess(); //synchronises globalattempts

                System.out.println(new Date().toString()
                        + ": " + playerName + " guessed " + guess +
                        ", attempt: " + attempt + "/" + MAXATTEMPTS);

                //checks for higher, lower, equal to or oob
                if (guess == RANDOMNUM) {
                    message = " Correct! Congratulations!";
                    //removes remaining attempts from GA
                    globalAttempts += (MAXATTEMPTS - attempt);
                    correctGuess = true; //yay!
                    isFinished = true;
                }
                else if (guess > RANDOMNUM && guess <= MAX)
                    message = " The number is lower.";

                else if (guess < RANDOMNUM && guess >= MIN)
                    message = " The number is higher.";

                else
                    message = " Number is out of bounds.";

                message = "Attempt " + attempt + "/" + MAXATTEMPTS
                        + ": " + guess + message;

                //max attempts done
                if(attempt == MAXATTEMPTS){
                    message = message + " " + MAR;
                    isFinished = true;
                }
            } else {
                message = MAR;
            }

            sendMessage(message + "\n");
        }

        /**
         * Handles game over for users when the game finishes
         * @throws IOException Catches IO exceptions
         */
        private void gameOver() throws IOException{
            String result;

            rankPlayers();

            //sends out rankings for all playser
            for(Player p : rankedPlayers){

                if(p.correctGuess){
                    result = " correctly ";
                } else {
                    result = " wrong ";
                }

                if(p.hasDisconnected){
                    sendMessage("END " + p.playerName + " forfeited.\n");
                }else {
                    sendMessage("END " + p.playerName + " guessed" + result
                            + "with " + p.attempt + "/" + MAXATTEMPTS + " guesses.\n");
                }
            }

            sendMessage("QUIT\n");

            //removes the player object from vector
            players.remove(this);
            //removes the name from the list
            names.remove(this.playerName);
        }

        /**
         * Ranks the player based on correct guesses > wrong
         * and lower guesses the better
         */
        private void rankPlayers(){
            //copies the player vector to mess with
            rankedPlayers = new Vector<>(players);

            for(int i = 0; i < rankedPlayers.size(); i++){
                for(int j = 0; j < rankedPlayers.size(); j++) {
                    if (i < j) {
                        //if p1 is wrong and p2 is right, p2 > p1
                        if (!rankedPlayers.get(i).correctGuess
                                && rankedPlayers.get(j).correctGuess) {
                            Collections.swap(rankedPlayers, i, j);
                        }
                        //if p1 attempts > p2 attempts and both correct, swap
                        else if (rankedPlayers.get(i).attempt > rankedPlayers.get(j).attempt
                                && rankedPlayers.get(i).correctGuess
                                && rankedPlayers.get(j).correctGuess) {
                            Collections.swap(rankedPlayers, i, j);
                        }
                        //if p1 disconnected and p2 hasn't, swap
                        else if (rankedPlayers.get(i).hasDisconnected
                                && !rankedPlayers.get(j).hasDisconnected) {
                            Collections.swap(rankedPlayers, i, j);
                        }
                    }
                }
            }
        }

        /**
         * Method used to close up everything opened for the player
         * @param hasDisconnected if the client disconnected from the server
         * @throws IOException Catches IO exceptions
         */
        private void close(boolean hasDisconnected) throws IOException{
            is.close();
            isr.close();
            br.close();

            os.close();
            osw.close();
            bw.close();

            if(hasDisconnected){
                attempt = MAXATTEMPTS; //helps GA

                System.out.println(new Date().toString() + ": " + playerName
                        + " at " + clientSocket.getPort() + " disconnected");
            } else {
                System.out.println(new Date().toString() + ": " + playerName
                        + " at " + clientSocket.getPort()
                        + " has left/been removed.");
            }

            //remove all existence
            bws.remove(bw);
            --numPlayers;

            if(!correctGuess) {
                globalAttempts += (MAXATTEMPTS - attempt);
            }

            clientSocket.close();
        }

        /**
         * Method to send message and check if user has disconnected
         * @param message message to send
         * @throws IOException Catches IO exceptions
         */
        private void sendMessage(String message) throws IOException{
            try {
                bw.write(message);
                bw.flush();
            } catch (SocketException se) {
                hasDisconnected = true;
            }
        }
    }
}


