import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer
{
    private ServerSocket serverSocket;
    final private int NUMPLAYERS = 4;

    private GameServer()
    {
        try {
            serverSocket = new ServerSocket(61005);

            System.out.println("Server running");
        } catch (IOException ioe){
            ioe.printStackTrace();
        }

        ExecutorService pool = Executors.newFixedThreadPool(NUMPLAYERS);

        while(true){
            try{
                //maybe use a for loop here
                Game game = new Game();

                for (;;) {
                    pool.execute(game.new Player(serverSocket.accept()));
                }

            } catch (IOException ioe){
                pool.shutdown();
            }
        }
    }

    public static void main(String [] args)
    {
        new GameServer();
    }
}

class Game {

    private static Set<String> names = new HashSet<>();

    private static Set<BufferedWriter> bws = new HashSet<>();

    private static Vector<Player> players = new Vector<>();

    private static Queue<Player> playerQueue = new LinkedList<>();


    private final int MAX = 9; //number limit
    private final int MIN = 0;
    private final int RANDOMNUM = (int) (Math.random() * (MAX - MIN) + 1) + MIN; //random number between MIN and MAX
    private final int MAXNUMPLAYERS = 2;
    private final int MAXATTEMPTS = 2;
    private final int MAXTIMEOUTCOUNTER = 5;

    private int globalAttempts = 0;
    private int numPlayers = 0;

    Game(){
        System.out.println("Random number is: " + RANDOMNUM);
    }

    public synchronized void guess(int guess, Player player, int attempt){
        ++globalAttempts;
    }

    //broadcast new player joining
    private void broadcastMessage(String message) throws IOException{
        for(BufferedWriter bw : bws){
            bw.write(message);
            bw.flush();
        }
    }

    class Player implements Runnable {

        private Socket clientSocket;
        private String playerName;
        private int attempt = 0;
        private InputStream is;
        private InputStreamReader isr;
        private BufferedReader br;
        private BufferedWriter bw;
        private OutputStreamWriter osw;
        private OutputStream os;
        private boolean correctGuess = false;
        private boolean isFinished = false;
        private int timeOutCounter = 0;

        Player(Socket s) {
            clientSocket = s;
        }

        @Override
        public void run() {
            try{
                setup();

                processPlayerName();

                waitForPlayers();

                close();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //player left

            }
        }

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

        private void processPlayerName() throws IOException{
            while (true) {
                bw.write("Enter your name: \n");
                bw.flush();

                playerName = br.readLine();

                if (playerName == null) {
                    return;
                }

                synchronized (names) {
                    if (!playerName.isBlank() && !names.contains(playerName)) {
                        ++numPlayers;

                        if(numPlayers > MAXNUMPLAYERS){
                            addPlayerToQueue();
                            bw.write("MESSAGE You have joined the game \n");
                            bw.flush();
                        }

                        System.out.println(this.playerName + " added to game");
                        names.add(playerName);
                        players.add(this);

                        bw.write("MESSAGE \n");

                        bw.flush();

                        break;
                    }
                }
            }

            broadcastMessage("MESSAGE " + playerName + " has joined \n");

            //adds current bw to the broadcast group
            bws.add(bw);
        }

        private void waitForPlayers() throws IOException{
            while(numPlayers <= MAXNUMPLAYERS) {
                if(numPlayers == MAXNUMPLAYERS) {
                    bw.write("MESSAGE Game starting. Send your guesses between " + MIN +  " and " + MAX + "\n");
                    bw.flush();

                    listen();
                } else {
                    bw.write("MESSAGE Waiting for " + (MAXNUMPLAYERS - numPlayers) + " more player(s) \n");
                    bw.flush();
                }

                try {
                    Thread.sleep(2000);

                    ++timeOutCounter;

                } catch (Exception e){
                    e.printStackTrace();
                }

                if(timeOutCounter == MAXTIMEOUTCOUNTER){
                    bw.write("MESSAGE Timeout. Not enough players\n");
                    bw.flush();

                    bw.write("QUIT\n");
                    bw.flush();

                    return;
                }
            }
        }

        private void addPlayerToQueue() throws IOException {
            playerQueue.add(this);

            System.out.println(this.playerName + " added to queue");

            bw.write("MESSAGE \n");
            bw.flush();

            bw.write("MESSAGE You have been added to a queue. \n");
            bw.flush();

            while (numPlayers > MAXNUMPLAYERS){
                bw.write("MESSAGE Please wait until the current game is over. \n");
                bw.flush();

                try {
                    Thread.sleep(2000);

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void listen() throws IOException{
            while(true){
                if(!isFinished) {
                    if (!correctGuess || attempt != MAXATTEMPTS) {
                        bw.write("1\n");
                    } else {
                        bw.write("0\n");
                    }
                    bw.flush();

                    String message = br.readLine();

                    if (message != null) {
                        try {
                            int guess = Integer.parseInt(message);

                            processGuess(guess);
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e){
                        e.printStackTrace();
                    }
                    if(globalAttempts == (MAXNUMPLAYERS * MAXATTEMPTS)){
                        gameOver();
                        break;
                    }
                }
            }
        }

        private void processGuess(int guess) throws IOException{
            String message;

            String MAR = "Max attempts reached";

            if(++attempt <= MAXATTEMPTS) {
                guess(guess, this, attempt);

                if (guess == RANDOMNUM) {
                    message = " Correct! Congratulations!";
                    //adds remaining attempts
                    globalAttempts += MAXATTEMPTS - attempt;
                    correctGuess = true;
                    isFinished = true;
                }
                else if (guess > RANDOMNUM && guess <= MAX)
                    message = " The number is lower.";

                else if (guess < RANDOMNUM && guess >= MIN)
                    message = " The number is higher.";

                else
                    message = " Number is out of bounds.";

                message = "Attempt " + attempt + "/" + MAXNUMPLAYERS + ": " + guess + message;

                if(attempt == MAXATTEMPTS){
                    message = message + " " + MAR;
                    isFinished = true;
                }

                bw.flush();
            } else {
                message = MAR;
            }

            bw.write(message + "\n");

            bw.flush();
        }

        private void gameOver() throws IOException{
            String result;

            for(Player p : players){

                if(p.correctGuess){
                    result = " correctly ";
                } else {
                    result = " wrong ";
                }

                bw.write("END " + p.playerName + " guessed" + result
                        + "with " + p.attempt + "/" + MAXATTEMPTS + " guesses.\n");
                bw.flush();
            }

            bw.write("QUIT\n");
            bw.flush();

            //remove all existence
            bws.remove(bw);
            players.remove(this);
            names.remove(this.playerName);
            --numPlayers;
        }

        private void close() throws IOException{
            is.close();
            isr.close();
            br.close();

            os.close();
            osw.close();
            bw.close();
        }
    }
}


