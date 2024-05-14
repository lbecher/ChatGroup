package br.unioeste;

import java.io.*;
import java.net.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Server {
    private final int port; // Numero da porta do servidor.

    // Lista de acesso concorrente para threads ClientHandler.
    private static final CopyOnWriteArrayList<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public Server(String port) {
        this.port = Integer.parseInt(port);
    }

    private void serverMessage(String str) {
        System.out.println("[Server Message] : " + str);
    }

    // Metodo que verifica se o nome de usuario ja esta sendo utilizado.
    private boolean isUsernameTaken(String username) throws IOException {
        for (ClientHandler clientHandler : clients) {
            if (clientHandler.getUsername().equals(username)) {
                return true;
            }
        }
        return false;
    }

    // Método que inicia o servidor.
    public void run() {
        serverMessage("Inicialiazando o servidor...");

        try {
            // Servidor aguarda por conexoes.
            ServerSocket serverSocket = new ServerSocket(this.port);
            serverMessage("O servidor está rodando e aguardando por conexões.");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                serverMessage("Novo cliente conectado: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.setUsername();    // Configura nome de usuario.
                boolean usernameTaken = false;  // Flag para verificar se o nome ja foi utilizado.

                usernameTaken = isUsernameTaken(clientHandler.getUsername());

                // Se nome nao utilizado : add novo usuario na lista e cria nova thread.
                if (!usernameTaken) {
                    clients.add(clientHandler);
                    new Thread(clientHandler).start();
                } else {
                    clientSocket.close(); // Fecha a conexão com o cliente.
                }
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

    // ClientHandler para lidar com os clientes conectados.
    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.username = null;

            try {
                // Cria streams de entrada e saida atraves do socket para o cliente.
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            try {
                System.out.println("User " + getUsername() + " connected.");

                out.println("Welcome to the chat, " + getUsername() + "!");
                out.println("Type Your Message");
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    System.out.println("[" + getUsername() + "]: " + inputLine);

                    // Mensagem enviada a todos do servidor via broadcast.
                    broadcast(this, "[" + getUsername() + "]: " + inputLine);
                }

                // Remove o clienthandler da lista.
                clients.remove(this);

                // Fecha streams e sockets do cliente.
                in.close();
                out.close();
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public String setUsername() throws IOException {
            out.println("Enter your username:");
            return in.readLine();
        }

        public String getUsername() throws IOException {
            return this.username;
        }

        public void sendMessage(String message) {
            out.println(message);
            out.println("Type Your Message");
        }


    }
}
