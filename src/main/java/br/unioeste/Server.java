package br.unioeste;

import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final int port;
    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public Server(String port) {
        this.port = Integer.parseInt(port);
    }

    public void run() {
        System.out.println("Iniciando o servidor...");

        try {
            ServerSocket serverSocket = new ServerSocket(this.port);
            System.out.println("O servidor está rodando e aguardando por conexões.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Novo cliente conectado: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void broadcast(ClientHandler sender, String message) {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler != sender) {
                clientHandler.sendMessage(message);
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;

            try {
                // Create input and output streams for communication 
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                String username = getUsername();
                System.out.println("User " + username + " connected.");

                out.println("Welcome to the chat, " + username + "!");
                out.println("Type Your Message");
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    System.out.println("[" + username + "]: " + inputLine);

                    // Broadcast the message to all clients 
                    broadcast(this, "[" + username + "]: " + inputLine);
                }

                // Remove the client handler from the list 
                clients.remove(this);

                // Close the input and output streams and the client socket 
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String getUsername() throws IOException {
            out.println("Enter your username:");
            return in.readLine();
        }

        public void sendMessage(String message) {
            out.println(message);
            out.println("Type Your Message");
        }
    }
}
