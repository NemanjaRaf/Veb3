import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;

public class PomocniServis {

    public static void main(String[] args) throws IOException {
        int port = 8081;
        ServerSocket serverSocket = new ServerSocket(port);

        System.out.println("Pomoćni Servis running on port " + port);

        while (true) {
            try (Socket clientSocket = serverSocket.accept();
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

                String input = in.readLine();
                if (input != null) {
                    int quotesSize = Integer.parseInt(input);
                    if (quotesSize == 0) {
                        out.println(-1);
                        continue;
                    }

                    int randomIndex = new Random().nextInt(quotesSize);

                    out.println(randomIndex);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
