package br.unioeste;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Objects;
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
        return clients.containsKey(username);
    }
    

    // --------------------------------------------------------------------------------------------------------------


    // Classe para gerenciar as salas.
    private static class Room {
        // Nomes de usuários dos membros.
        private HashSet<String> members;
        private String room_name;
        private String admin;
        private String hashedPassword;
        private boolean is_private;

        // Construtor.
        public Room(String room_name, String admin, String hashedPassword) {
            this.members = new HashSet<String>();
            this.room_name = room_name;
            this.admin = admin;
            this.hashedPassword = hashedPassword;
            is_private = hashedPassword != null;
        }

        // Método para verificar se uma sala é privada.
        public boolean isRoomPrivate() {
            return is_private;
        }

        // Método para verificar se um usuário ja é membro da sala.
        public boolean isMember(String username) {
            return this.members.contains(username);
        }

        // Método que adiciona novo membro na sala.
        public void addMember(String username) {
            this.members.add(username);
        }

        // Método que remove um memebro da sala.
        public void removeMember(String username, ClientHandler client) {
            if (this.admin.equals(username)) {
                client.sendMessage("ERRO O administrador não pode ser removido!");
                return;
            }
            this.members.remove(username);
        }

        // Método que valida a senha hash.
        public boolean validateHashedPassword(String hashedPassword) {
            return (this.hashedPassword != null && !this.hashedPassword.equals(hashedPassword)) ? false : true;
        }

        // Método que encaminha as mensagens para os membros da sala.
        public void sendMessageForMembers(String sender, String sender_message) {
            String message = "MENSAGEM " + this.room_name + " " + sender + " " + sender_message;

            ClientHandler admin = clients.get(this.admin);
            admin.sendMessage(message);

            for (String member : this.members) {
                ClientHandler client = clients.get(member);
                client.sendMessage(message);
            }
        }

        // Método que lista todos os membros da sala.
        public String listAllRoomMembers () {
            String allMembers = members.stream().collect(Collectors.joining(" "));
            return "ENTRAR_SALA_OK " + allMembers;
        }
    }


    // ---------------------------------------------------


    // Classe para lidar com os clientes conectados.
    private static class ClientHandler implements Runnable {
        private String username;
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        // Construtor.
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
                // Registra soquete e nome de usuário em clients.
                // Permanece nesse método até obter um nome de usuário válido
                // ou a conexão ser fechada.
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
                            handleExitRoom(splited_message);
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

        // Método que registra o usuário no servidor.
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

        // Método que adiciona um membro na sala.
        private void handleJoinRoom(String[] splited_message) { 
            String room_name = splited_message[1];

            if (!rooms.containsKey(room_name)) {
                sendMessage("ERRO A sala " + room_name + " não existe!");
                return;
            }

            Room room = rooms.get(room_name);
            room.addMember(this.username);

            sendMessage(room.listAllRoomMembers());

            ClientHandler admin = clients.get(room.admin);
            for (String member : room.members) {
                if (!Objects.equals(member, this.username))
                    admin.sendMessage("ENTROU " + room.room_name + " " + this.username);
            }
        }

        // Método que encaminha as mensagens para os membros da sala.
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

            Room room = rooms.get(room_name);

            if (!room.isMember(this.username)) {
                sendMessage("ERRO Você não é membro da sala " + room_name + "!");
                return;
            }

            String message = "";

            for (int i = 2; i < splited_message.length; i++) {
                message = message.concat(" " + splited_message[i]);
            }

            room.sendMessageForMembers(this.username, message);
        }

        // Método para criar uma sala.
        private void handleRoomCreation(String[] splited_message) {
            if (splited_message.length <= 2 || splited_message.length >= 5) {
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

        // Método que cria as salas publicas ou privadas.
        private void createRoom(String room_name, String admin, String hashedPassword) {
            if (!rooms.containsKey(room_name)) {
                Room room = new Room(room_name, admin, hashedPassword);
                rooms.put(room_name, room);
                sendMessage("CRIAR_SALA_OK");
            } else {
                sendMessage("ERRO Uma sala já existe com esse nome!");
            }
        }

        // Método para sair de uma sala.
        private void handleExitRoom(String[] splited_message) {
            if (splited_message.length != 2) {
                sendMessage("ERRO Argumentos inválidos para o comando SAIR_SALA!");
                return;
            }

            if (!rooms.containsKey(splited_message[1])) {
                sendMessage("ERRO A sala informada nao existe!");
                return;
            }

            if (!rooms.get(splited_message[1]).isMember(this.username)) {
                sendMessage("ERRO Usuário não é membro da sala.");
                return;
            }

            sendMessage("SAIR_SALA_OK");

            ClientHandler admin = clients.get(rooms.get(splited_message[1]).admin);
            for (String member : rooms.get(splited_message[1]).members) {
                if (!Objects.equals(member, this.username)) {
                    admin.sendMessage("SAIU " + rooms.get(splited_message[1]).room_name + " " + this.username);
                }
            }

            rooms.get(splited_message[1]).members.remove(this.username);

            if (Objects.equals(rooms.get(splited_message[1]).admin, this.username)) {
                rooms.remove(splited_message[1]);
            }

        }

        // Método que lista todas as salas.
        private void listRooms() {
            String concatenatedRoomsNames = rooms.entrySet().stream()
                    .filter(entry -> entry.getKey() instanceof String && entry.getValue() instanceof Room)
                    .map(entry -> {
                        String name = entry.getKey();
                        Room room = entry.getValue();
                        String availabilityString = room.isRoomPrivate() ? "Privada" : "Publica";
                        return name + "(" + availabilityString + ")";
                    })
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

        // Método que retorna o nome do usuário.
        public String getUsername() throws IOException {
            return this.username;
        }

        // Método que envia as mensagens digitadas.
        private void sendMessage(String message) {
            out.println(message);
        }

        // Método que capta as mensagens digitadas.
        private String recieveMessage() throws IOException {
            return in.readLine();
        }
    }
}
