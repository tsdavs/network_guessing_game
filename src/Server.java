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

    ServerThread(Socket s){
        clientSocket = s;
    }

    @Override
    public void run() {

        while(true){
            try {
                //Reading the message from the client
                InputStream is = clientSocket.getInputStream();

                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line = br.readLine();
                System.out.println("Message received from " + clientSocket.getPort() + " is " + line);


                //Sending the response back to the client.
                OutputStream os = clientSocket.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os);
                BufferedWriter bw = new BufferedWriter(osw);
                bw.write(line + "\n");
                System.out.println("Message sent back to the " + clientSocket.getPort() + " is " + line);
                bw.flush();
            } catch (IOException e){

            }
        }
    }
}


