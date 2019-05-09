import java.io.*;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Client extends Thread{

    private Socket socket;

    private Client() {
        Thread t = new Thread(() -> {
            try {
                socket = new Socket("localhost", 61004);

                while(true) {
                    sendGuess();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();
    }

    private void sendGuess() throws IOException {
        //Send the message to the server
        OutputStream os = socket.getOutputStream();

        OutputStreamWriter osw = new OutputStreamWriter(os);

        BufferedWriter bw = new BufferedWriter(osw);

        int guess = 0;

        boolean error;

        do {
            try {
                Scanner scanner = new Scanner(System.in);

                guess = scanner.nextInt();

                error = false;

            } catch (InputMismatchException e) {
                System.out.println("Input was NaN. Try again.");

                error = true;
            }
        } while (error);

        String sendMessage = guess + "\n";

        bw.write(sendMessage);

        bw.flush();

        //Get the return message from the server
        InputStream is = socket.getInputStream();

        InputStreamReader isr = new InputStreamReader(is);

        BufferedReader br = new BufferedReader(isr);

        String message = br.readLine();

        System.out.println(message);

    }

    public static void main(String[] args) {
        Thread[] threads = {
                new Client()
        };

        for(Thread t : threads){
            t.start();
        }
    }

}