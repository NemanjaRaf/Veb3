import com.google.gson.Gson;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

class Quote {
    String author;
    String quote;

    public Quote(String author, String quote) {
        this.author = author;
        this.quote = quote;
    }
}

class QuoteService {

    private static final Gson gson = new Gson();
    private static final List quotes = Collections.synchronizedList(new ArrayList());

    public static void main(String[] args) throws IOException {
        int port = 8080;
        ServerSocket serverSocket = new ServerSocket(port);

        System.out.println("Glavni Servis running on port " + port);

        while (true) {
            try (Socket clientSocket = serverSocket.accept();

                 PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

                String inputLine;
                StringBuilder requestBody = new StringBuilder();
                String method = "";
                String path = "";

                // Read the request
                while (!(inputLine = in.readLine()).isEmpty()) {
                    if (requestBody.length() == 0) {
                        String[] requestLine = inputLine.split(" ");
                        method = requestLine[0];
                        path = requestLine[1];
                    }
                    requestBody.append(inputLine + "\n");
                }

                System.out.println("Client connected");

                if (method.equals("GET") && path.equals("/quotes")) {
                    String quoteOfDay = getQuoteOfDay();
                    String response = buildQuotesPage(quoteOfDay);
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/html");
                    out.println("Content-Length: " + response.length());
                    out.println();
                    out.println(response);
                } else if (path.equals("/error")) {
                    String response = buildErrorPage();
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/html");
                    out.println("Content-Length: " + response.length());
                    out.println();
                    out.println(response);
                } else if (method.equals("POST") && path.equals("/save-quote")) {
                    StringBuilder payload = new StringBuilder();
                    while(in.ready()){
                        payload.append((char) in.read());
                    }
                    String[] postData = payload.toString().split("&");
                    String author = "";
                    String quoteText = "";
                    if (postData.length != 2) {
                        // redirect to /error
                        out.println("HTTP/1.1 303 See Other");
                        out.println("Location: /error");
                        out.println();
                        continue;

                    }
                    for (String keyValue : postData) {
                        String[] pair = keyValue.split("=");
                        if (pair.length != 2) {
                            out.println("HTTP/1.1 303 See Other");
                            out.println("Location: /error");
                            out.println();
                            continue;
                        }
                        if (pair[0].equals("author")) {
                            author = java.net.URLDecoder.decode(pair[1], "UTF-8");
                        } else if (pair[0].equals("quote")) {
                            quoteText = java.net.URLDecoder.decode(pair[1], "UTF-8");
                        } else {
                            out.println("HTTP/1.1 303 See Other");
                            out.println("Location: /error");
                            out.println();
                            continue;
                        }
                    }

                    if (author.isEmpty() || quoteText.isEmpty()) {
                        out.println("HTTP/1.1 303 See Other");
                        out.println("Location: /error");
                        out.println();
                        continue;
                    }

                    Quote newQuote = new Quote(author, quoteText);
                    quotes.add(newQuote);

                    out.println("HTTP/1.1 303 See Other");
                    out.println("Location: /quotes");
                    out.println();
                }

                out.close();
                in.close();
                clientSocket.close();
            }
        }
    }

    private static String getQuoteOfDay() {
        String quoteOfDay = "";
        try (Socket pomocniSocket = new Socket("localhost", 8081);
             PrintWriter out = new PrintWriter(pomocniSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(pomocniSocket.getInputStream()))) {

            out.println(quotes.size());

            String line = in.readLine();
            if (line != null) {
                int randomIndex = Integer.parseInt(line);
                if (randomIndex == -1) {
                    quoteOfDay = "No quotes saved yet.";
                }

                if (randomIndex >= 0 && randomIndex < quotes.size()) {
                    Quote randomQuote = (Quote) quotes.get(randomIndex);
                    quoteOfDay = randomQuote.author + ": \"" + randomQuote.quote + "\"";
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            quoteOfDay = "Error fetching quote of the day.";
        }
        return quoteOfDay;
    }


    private static String buildErrorPage() {
        return "<html><head><title>Error</title></head><body><h1>400 Bad Request</h1></body></html>";
    }

    private static String buildQuotesPage(String quoteOfDay) {
        StringBuilder htmlBuilder = new StringBuilder();

        htmlBuilder.append("<html><head><title>Quotes Management</title></head><body>");

        htmlBuilder.append("<form action='/save-quote' method='POST'>");
        htmlBuilder.append("<input type='text' name='author' placeholder='Author'/>");
        htmlBuilder.append("<input type='text' name='quote' placeholder='Quote'/>");
        htmlBuilder.append("<input type='submit' value='Save Quote'/>");
        htmlBuilder.append("</form>");

        htmlBuilder.append("<div>");
        htmlBuilder.append("<h2>Quote of the day:</h2>");
        htmlBuilder.append("<p>").append(quoteOfDay).append("</p>");
        htmlBuilder.append("</div>");

        htmlBuilder.append("<div>");
        htmlBuilder.append("<h2>Saved Quotes</h2>");
        for (Object qu : quotes) {
            Quote q = (Quote) qu;
            htmlBuilder.append("<p>").append(q.author).append(": \"").append(q.quote).append("\"</p>");
        }
        htmlBuilder.append("</div>");

        htmlBuilder.append("</body></html>");

        return htmlBuilder.toString();
    }
}
