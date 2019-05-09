import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server
{
    private ServerSocket serverSocket;

    private Server()
    {
        try {
            serverSocket = new ServerSocket(61004);

            System.out.println("Server running");
        } catch (IOException ioe){
            ioe.printStackTrace();
        }

        while(true){
            try{
                Socket clientSocket = serverSocket.accept();

                ServerThread serviceThread = new ServerThread(clientSocket);

                serviceThread.start();
            } catch (IOException ioe){
                ioe.printStackTrace();
            }
        }
    }

    public static void main(String [] args)
    {
        new Server();
    }
}

class ServerThread extends Thread{
    private Socket clientSocket;

    private int attempt = 0;

    private int max = 9; //number limit

    private int min = 0;

    private int randomNum = (int)(Math.random() * (max - min) + 1) + min; //random number between min and max

    ServerThread(Socket s){
        clientSocket = s;
    }

    @Override
    public void run() {

        System.out.println(randomNum);

        while(true){
            try {
                //Reading the message from the client
                InputStream is = clientSocket.getInputStream();

                InputStreamReader isr = new InputStreamReader(is);

                BufferedReader br = new BufferedReader(isr);

                int guess = Integer.parseInt(br.readLine());

                System.out.println("Attempt " + ++attempt + " from " + clientSocket.getPort() + " is " + guess);

                //Sending the response back to the client.
                OutputStream os = clientSocket.getOutputStream();

                OutputStreamWriter osw = new OutputStreamWriter(os);

                BufferedWriter bw = new BufferedWriter(osw);

                String message;

                if(guess == randomNum)
                    message = " Correct! Congratulations!";

                else if(guess > randomNum && guess <= max)
                    message = " The number is lower. Try again.";

                else if(guess < randomNum && guess >= min)
                    message = " The number is higher. Try again.";

                else
                    message = " Number is out of bounds. Try again.";

                bw.write("Attempt " + attempt + ": " + guess + message +  "\n");

                bw.flush();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}


