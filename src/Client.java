import java.io.*;
import java.net.Socket;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Client extends Thread{

    private Socket socket;
    private InputStream is;
    private InputStreamReader isr;
    private BufferedReader br;
    private BufferedWriter bw;
    private OutputStreamWriter osw;
    private OutputStream os;
    private Scanner scanner;

    private Client() {
        Thread t = new Thread(() -> {
            try {
                socket = new Socket("localhost", 61005);

                setup();

                while(true) {
                    //sendGuess();

                    listen();

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        t.start();
    }

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

    private void sendName() throws IOException {
        while(true) {
            String message = br.readLine();

            if(message.equals("1")){
                System.out.println("Welcome...");
                break;
            }

            System.out.println(message);

            bw.write(scanner.next() + "\n");

            bw.flush();
        }
    }

    private void listen() throws IOException {
        String message = br.readLine();

        //if(message.startsWith("MESSAGE ")){
            System.out.println(message);
        //}

    }

    private void sendGuess() throws IOException {
        int guess = -1;

        boolean error;

        do {
            try {
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