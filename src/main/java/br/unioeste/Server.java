package br.unioeste;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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
    private static class Room {
        // Nomes de usuários dos membros.
        private HashSet<String> members;

        private String room_name;
        private String admin;
        private String hashedPassword;

        public Room(String room_name, String admin, String hashedPassword) {
            this.members = new HashSet<String>();

            this.room_name = room_name;
            this.admin = admin;
            this.hashedPassword = hashedPassword;
        }

        public void addMember(String username) {
            this.members.add(username);
        }

        public void removeMember(String username, ClientHandler client) {
            if (this.admin.equals(username)) {
                client.sendMessage("ERRO O administrador não pode ser removido!");
                return;
            }
            this.members.remove(username);
        }

        public boolean validateHashedPassword(String hashedPassword) {
            if (this.hashedPassword != null && !this.hashedPassword.equals(hashedPassword)) {
                return false;
            }
            return true;
        }

        public void sendMessageForMembers(String sender, String sender_message) {
            String message = "MENSAGEM " + this.room_name + " " + sender + " " + sender_message;

            ClientHandler admin = clients.get(this.admin);
            admin.sendMessage(message);

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

                while (true) {
                    String message = recieveMessage();
                    String[] splited_message = message.split(" ");

                    switch (splited_message[0]) {
                        case "CRIAR_SALA":
                            // Lida com o restante do comando em outro método.
                            handleRoomCreation(splited_message);
                            break;
                        
                        case "LISTAR_SALAS":
                            // Comando simples, não precisa de um handle.
                            listRooms();
                            break;
                        
                        case "ENTRAR_SALA":
                            // ENTRAR_SALA <nome_da_sala> [ hash(senha) ]
                            handleJoinRoom(splited_message);
                            break;
                        
                        case "SAIR_SALA":
                            
                            break;
                        
                        case "FECHAR_SALA":
                            
                            break;
                        
                        case "ENVIAR_MENSAGEM":
                            // Lida com o restante do comando em outro método.
                            handleSendMessage(splited_message);
                            break;
                    
                        default:
                            sendMessage("ERRO Comando inválido ou não esparado!");
                            break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void registerClient() throws IOException {
            while (true) {
                // Deve ter o formato REGISTRO <username>.
                String message = recieveMessage();
                String[] splited_message = message.split(" ");

                if (splited_message.length == 2) {
                    String action = splited_message[0];

                    if (action.equals("REGISTRO")) {
                        String username = splited_message[1];

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

        private void handleJoinRoom(String[] splited_message) { 
            String room_name = splited_message[1];

            if (!rooms.containsKey(room_name)) {
                sendMessage("ERRO A sala " + room_name + " não existe!");
                return;
            }

            Room room = rooms.get(room_name);
            room.addMember(this.username);

            //sendMessage("ENTRAR_SALA_OK " + room.getMembers());
        }

        private void handleSendMessage(String[] splited_message) {
            if (splited_message.length < 3) {
                sendMessage("ERRO Argumentos faltando em ENVIAR_MENSAGEM!");
                return;
            }

            String room_name = splited_message[1];

            if (!rooms.containsKey(room_name)) {
                sendMessage("ERRO A sala " + room_name + " não existe!");
                return;
            }

            String message = "";

            for (int i = 2; i < splited_message.length; i++) {
                message = message.concat(" " + splited_message[i]);
            }

            Room room = rooms.get(room_name);
            room.sendMessageForMembers(this.username, message);
        }

        private void handleRoomCreation(String[] splited_message) {
            if (splited_message.length != 3) {
                sendMessage("ERRO Argumentos faltando em CRIAR_SALA!");
                return;
            }

            switch (splited_message[1]) {
                case "PUBLICA":
                    createRoom(splited_message[2], this.username, null);
                    break;
                
                case "PRIVADA":
                    if (splited_message.length != 4) {
                        sendMessage("ERRO Quantidade errada de argumentos em CRIAR_SALA!");
                        return;
                    }
                    createRoom(splited_message[2], this.username, splited_message[3]);
                    break;
            
                default:
                    sendMessage("ERRO Argumento inválido em CRIAR_SALA!");
                    break;
            }
        }

        private void createRoom(String room_name, String admin, String hashedPassword) {
            if (!rooms.containsKey(room_name)) {
                Room room = new Room(room_name, admin, hashedPassword);
                rooms.put(room_name, room);
                sendMessage("CRIAR_SALA_OK");
            } else {
                sendMessage("ERRO Uma sala já existe com esse nome!");
            }
        }

        private void listRooms() {
            String concatenatedRoomsNames = rooms.keySet().stream()
                .filter(name -> name instanceof String)
                .map(name -> (String) name)
                .collect(Collectors.joining(" "));
            
            sendMessage("SALAS " + concatenatedRoomsNames);
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
