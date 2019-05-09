import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GameServer
{
    private ServerSocket serverSocket;
    final private int NUMPLAYERS = 4;

    private GameServer()
    {
        try {
            serverSocket = new ServerSocket(61004);

            System.out.println("Server running");
        } catch (IOException ioe){
            ioe.printStackTrace();
        }

        ExecutorService pool = Executors.newFixedThreadPool(NUMPLAYERS);

        while(true){
            try{
                Game game = new Game();

                //maybe use a for loop here
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

    private final int MAX = 9; //number limit
    private final int MIN = 0;
    private final int RANDOMNUM = (int) (Math.random() * (MAX - MIN) + 1) + MIN; //random number between MIN and MAX

    public synchronized void guess(int guess){
        //System.out.println("");
    }


    class Player implements Runnable {

        private Socket clientSocket;
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
                processCommands();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                //player left
            }
        }

        private void setup() throws IOException{
            System.out.println(RANDOMNUM); //for testing purposes remove later

            //Reading the message from the client
            is = clientSocket.getInputStream();
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);

            //Sending the response back to the client.
            os = clientSocket.getOutputStream();
            osw = new OutputStreamWriter(os);
            bw = new BufferedWriter(osw);
        }

        private void processCommands(){
            while (true) {
                try {
                    int guess = Integer.parseInt(br.readLine());

                    guess(guess);

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
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}


