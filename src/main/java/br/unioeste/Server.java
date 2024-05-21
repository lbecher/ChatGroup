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

    // Exibir log do servidor no console.
    private static void serverLog(String log) {
        System.out.println(log);
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
        private String roomName;
        private String admin;
        private String hashedPassword;
        private boolean is_private;

        // Construtor.
        public Room(String roomName, String admin, String hashedPassword) {
            this.roomName = roomName;
            this.admin = admin;
            this.hashedPassword = hashedPassword;

            this.members = new HashSet<String>();

            is_private = hashedPassword != null;
        }

        // Método para verificar se uma sala é privada.
        public boolean isRoomPrivate() {
            return is_private;
        }

        // Método para verificar se um usuário já é membro da sala.
        public boolean isMember(String username) {
            return this.members.contains(username) || this.admin.equals(username);
        }

        // Método que adiciona novo membro à sala.
        public void addMember(String username) {
            this.members.add(username);
        }

        // Método que remove um memebro da sala.
        public void removeMember(String member) {
            if (this.admin.equals(member)) {
                ClientHandler client = clients.get(member);
                client.sendCommand("ERRO O administrador não pode ser removido!");
                return;
            }
            this.members.remove(member);
        }

        // Método que valida a senha hash.
        public boolean validateHashedPassword(String hashedPassword) {
            // DISCUTIR ISSO AAAAAAAAAAAAAAAAAH
            return (this.hashedPassword != null && !this.hashedPassword.equals(hashedPassword)) ? false : true;
        }

        // Método que encaminha as mensagens para os membros da sala.
        public void sendCommandForMembers(String sender, String message) {
            String command = "MENSAGEM " + this.roomName + " " + sender + " " + message;

            ClientHandler admin = clients.get(this.admin);
            admin.sendCommand(command);

            for (String member : this.members) {
                ClientHandler client = clients.get(member);
                client.sendCommand(command);
            }
        }

        public void exit(String member) {
            this.removeMember(member);
            this.notifyMembersAboutMemberExit(member);
        }

        public void notifyMembersAboutNewMember(String username) {
            String command = "ENTROU " + this.roomName + " " + username;

            ClientHandler admin = clients.get(this.admin);
            admin.sendCommand(command);

            for (String member : this.members) {
                ClientHandler client = clients.get(member);
                client.sendCommand(command);
            }
        }

        public void notifyMembersAboutMemberExit(String username) {
            String command = "SAIU " + this.roomName + " " + username;

            ClientHandler admin = clients.get(this.admin);
            admin.sendCommand(command);

            for (String member : this.members) {
                ClientHandler client = clients.get(member);
                client.sendCommand(command);
            }
        }

        // Método que lista todos os membros da sala.
        public String listAllRoomMembers() {
            return this.admin + " " + members.stream().collect(Collectors.joining(" "));
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
                    String command = recieveCommand();
                    String[] splitedCommand = command.split(" ");

                    switch (splitedCommand[0]) {
                        case "CRIAR_SALA":
                            // Lida com o restante do comando em outro método.
                            handleRoomCreation(splitedCommand);
                            break;
                        
                        case "LISTAR_SALAS":
                            // Comando simples, não precisa de um handle.
                            listRooms();
                            break;
                        
                        case "ENTRAR_SALA":
                            // Lida com o restante do comando em outro método.
                            handleJoinRoom(splitedCommand);
                            break;
                        
                        case "SAIR_SALA":
                            // Lida com o restante do comando em outro método.
                            handleExitRoom(splitedCommand);
                            break;
                        
                        case "FECHAR_SALA":
                            
                            break;
                        
                        case "ENVIAR_MENSAGEM":
                            // Lida com o restante do comando em outro método.
                            handleSendCommand(splitedCommand);
                            break;
                    
                        default:
                            sendCommand("ERRO Comando inválido ou não esparado!");
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
                String message = recieveCommand();
                String[] splitedCommand = message.split(" ");

                if (splitedCommand.length == 2) {
                    String action = splitedCommand[0];

                    if (action.equals("REGISTRO")) {
                        String username = splitedCommand[1];

                        if (!isUsernameTaken(username)) {
                            this.username = username;
                            clients.put(username, this);
                            sendCommand("REGISTRO_OK");
                            return;
                        }
                        else {
                            sendCommand("ERRO Nome de usuário em uso! Tente novamente.");
                        }
                    }
                    else {
                        sendCommand("ERRO Comando inválido ou não esparado!");
                    }
                }
                else {
                    sendCommand("ERRO Comando ou argumentos inválidos! Tente REGISTRO <nome_de_usuário>.");
                }
            }
        }

        // Método que adiciona um membro na sala.
        private void handleJoinRoom(String[] splitedCommand) { 
            if (splitedCommand.length < 2) {
                sendCommand("ERRO Argumentos faltando em ENTRAR_SALA!");
                return;
            }

            String roomName = splitedCommand[1];

            if (!rooms.containsKey(roomName)) {
                sendCommand("ERRO A sala " + roomName + " não existe!");
                return;
            }

            Room room = rooms.get(roomName);

            if (room.isRoomPrivate()) {
                if (splitedCommand.length < 3) {
                    sendCommand("ERRO Não foi informado uma senha para entrar na sala privada " + roomName + "!");
                    return;
                }

                String hashedPassword = splitedCommand[1];

                if (!room.validateHashedPassword(hashedPassword)) {
                    sendCommand("ERRO Senha incorreta!");
                    return;
                }
            }

            sendCommand("ENTRAR_SALA_OK " + room.listAllRoomMembers());

            room.notifyMembersAboutNewMember(this.username);
            room.addMember(this.username);
        }

        // Método que encaminha as mensagens para os membros da sala.
        private void handleSendCommand(String[] splitedCommand) {
            if (splitedCommand.length < 3) {
                sendCommand("ERRO Argumentos faltando em ENVIAR_MENSAGEM!");
                return;
            }

            String roomName = splitedCommand[1];

            if (!rooms.containsKey(roomName)) {
                sendCommand("ERRO A sala " + roomName + " não existe!");
                return;
            }

            Room room = rooms.get(roomName);

            if (!room.isMember(this.username)) {
                sendCommand("ERRO Você não é membro da sala " + roomName + "!");
                return;
            }

            String message = "";

            for (int i = 2; i < splitedCommand.length; i++) {
                message = message.concat(" " + splitedCommand[i]);
            }

            room.sendCommandForMembers(this.username, message);
        }

        // Método para criar uma sala.
        private void handleRoomCreation(String[] splitedCommand) {
            if (splitedCommand.length <= 2 || splitedCommand.length >= 5) {
                sendCommand("ERRO Argumentos faltando em CRIAR_SALA!");
                return;
            }

            switch (splitedCommand[1]) {
                case "PUBLICA":
                    createRoom(splitedCommand[2], this.username, null);
                    break;
                
                case "PRIVADA":
                    if (splitedCommand.length != 4) {
                        sendCommand("ERRO Quantidade errada de argumentos em CRIAR_SALA!");
                        return;
                    }
                    createRoom(splitedCommand[2], this.username, splitedCommand[3]);
                    break;
            
                default:
                    sendCommand("ERRO Argumento inválido em CRIAR_SALA!");
                    break;
            }
        }

        // Método que cria as salas publicas ou privadas.
        private void createRoom(String roomName, String admin, String hashedPassword) {
            if (!rooms.containsKey(roomName)) {
                Room room = new Room(roomName, admin, hashedPassword);
                rooms.put(roomName, room);
                sendCommand("CRIAR_SALA_OK");
            } else {
                sendCommand("ERRO Uma sala já existe com esse nome!");
            }
        }

        // Método para sair de uma sala.
        private void handleExitRoom(String[] splitedCommand) {
            if (splitedCommand.length != 2) {
                sendCommand("ERRO Argumentos inválidos para o comando SAIR_SALA!");
                return;
            }

            String roomName = splitedCommand[1];

            if (!rooms.containsKey(roomName)) {
                sendCommand("ERRO A sala informada não existe!");
                return;
            }

            Room room = rooms.get(roomName);

            if (!room.isMember(this.username)) {
                sendCommand("ERRO Usuário não é membro da sala.");
                return;
            }

            sendCommand("SAIR_SALA_OK");

            room.exit(this.username);
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
            
            sendCommand("SALAS " + concatenatedRoomsNames);
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

        // Método que envia os comandos para cliente.
        private void sendCommand(String command) {
            out.println(command);
        }

        // Método que recebe os comandos do servidor.
        private String recieveCommand() throws IOException {
            return in.readLine();
        }
    }
}
