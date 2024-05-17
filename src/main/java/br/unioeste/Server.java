package br.unioeste;

import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    // Numero da porta do servidor.
    private final int port;

    // Usa HashMap para realizar buscas e verificações de maneira mais eficiente.
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<String, ClientHandler>();
    private static final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<String, Room>();

    // Construtor.
    public Server(String port) {
        this.port = Integer.parseInt(port);
    }

    // Método que inicia o servidor.
    public void run() {
        serverLog("Inicialiazando o servidor...");

        try {
            // Inicializa soquete do servidor.
            ServerSocket serverSocket = new ServerSocket(this.port);
            serverLog("O servidor está rodando e aguardando por conexões.");

            // Quando uma nova conexão é identificada, 
            while (true) {
                // Aceita uma conexão via soquete (novo cliente).
                Socket clientSocket = serverSocket.accept();
                serverLog("Novo cliente conectado: " + clientSocket);

                // Instancia cliente e o executa em uma nova thread.
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void serverLog(String str) {
        System.out.println("[Server log]: " + str);
    }

    // Método que verifica se o nome de usuário já esta sendo utilizado.
    private static boolean isUsernameTaken(String username) throws IOException {
        if (clients.containsKey(username)) {
            return true;
        }
        return false;
    }
    


    // ---------------------------------------------------
    // Classe para as gerenciar as salas.
    private class Room {
        private HashSet<String> members;
        private String hashedPassword;

        public Room(String hashedPassword) {
            this.members = new HashSet<String>();
            this.hashedPassword = hashedPassword;
        }

        public void addMember(String username) {
            this.members.add(username);
        }

        public void removeMember(String username) {
            this.members.remove(username);
        }

        public boolean validateHashedPassword(String hashedPassword) {
            if (this.hashedPassword != null && !this.hashedPassword.equals(hashedPassword)) {
                return false;
            }
            return true;
        }

        public void sendMessageForMembers(String message) {
            for (String member : this.members) {
                ClientHandler client = clients.get(member);
                client.sendMessage(message);
            }
        }
    }



    // ---------------------------------------------------
    // Classe para lidar com os clientes conectados.
    private static class ClientHandler implements Runnable {
        private String username;
        
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

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
                // Registra soquete e nome de usuário em clients. Permanece 
                // nesse método até obter um nome de usuário válido ou a
                // conexão ser fechada.
                this.registerClient();

                /*
                 *
                 *   Lógicas de salas e mensagens vem aqui.....
                 *
                 */

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void registerClient() throws IOException {
            while (true) {
                // Deve ter o formato REGISTRO <username>.
                String command = recieveMessage();
                String[] splited_command = command.split(" ");

                if (splited_command.length == 2) {
                    String action = splited_command[0];

                    if (action.equals("REGISTRO")) {
                        String username = splited_command[1];

                        if (!isUsernameTaken(username)) {
                            this.username = username;
                            clients.put(username, this);
                            sendMessage("REGISTRO_OK");
                            return;
                        }
                        else {
                            sendMessage("ERRO Nome de usuário em uso! Tente novamente.");
                        }
                    }
                    else {
                        sendMessage("ERRO Comando inválido ou não esparado!");
                    }
                }
                else {
                    sendMessage("ERRO Comando ou argumentos inválidos! Tente REGISTRO <nome_de_usuário>.");
                }
            }
        }

        public void removeUser() throws IOException {
            // Remove o clienthandler da lista.
            clients.remove(this);

            // Fecha streams e sockets do cliente.
            in.close();
            out.close();
            socket.close();
        }

        public String getUsername() throws IOException {
            return this.username;
        }

        private void sendMessage(String message) {
            out.println(message);
        }

        private String recieveMessage() throws IOException {
            return in.readLine();
        }
    }
}
