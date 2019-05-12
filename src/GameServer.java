import javax.swing.text.html.HTMLDocument;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
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

                pool.execute(game.new Player(serverSocket.accept()));
                pool.execute(game.new Player(serverSocket.accept()));
                pool.execute(game.new Player(serverSocket.accept()));
                pool.execute(game.new Player(serverSocket.accept()));


            } catch (IOException ioe){
                ioe.printStackTrace();
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


    private final int MAX = 9; //number limit
    private final int MIN = 0;
    private final int RANDOMNUM = (int) (Math.random() * (MAX - MIN) + 1) + MIN; //random number between MIN and MAX

    Game(){
        System.out.println(RANDOMNUM);
    }

    public synchronized void guess(int guess, Player player){
        //System.out.println("");
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

        Player(Socket s) {
            clientSocket = s;
        }

        @Override
        public void run() {
            try{
                setup();

                processClientInput();
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

            while (true) {
                bw.write("Enter your name: \n");

                bw.flush();

                playerName = br.readLine();

                if (playerName == null) {
                    return;
                }

                synchronized (names) {
                    if (!playerName.isBlank() && !names.contains(playerName)) {
                        names.add(playerName);

                        bw.write("1\n");

                        bw.flush();

                        break;
                    }
                }
            }

            //broadcast new player joining
            for(BufferedWriter bw : bws){
                System.out.println(bw);
                bw.write("MESSAGE " + playerName + " has joined \n");
                bw.flush();
            }
            //adds current bw to the broadcast group
            bws.add(bw);
        }

        private void processClientInput() throws IOException{
            while(true){
                processGuess();

            }

        }

        private void processGuess() throws IOException{
            int guess = Integer.parseInt(br.readLine());

            guess(guess, this);

            System.out.println("Attempt " + ++attempt + " from " + clientSocket.getPort() + " is " + guess);

            String message;

            if (guess == RANDOMNUM)
                message = " Correct! Congratulations!";

            else if (guess > RANDOMNUM && guess <= MAX)
                message = " The number is lower. Try again.";

            else if (guess < RANDOMNUM && guess >= MIN)
                message = " The number is higher. Try again.";

            else
                message = " Number is out of bounds. Try again.";

            bw.write("Attempt " + attempt + ": " + guess + message + "\n");

            bw.flush();
        }

    }
}


