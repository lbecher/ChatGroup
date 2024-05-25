package br.unioeste;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

// CLASSE DO SERVIDOR.
public class Server {

    // Numero da porta do servidor.
    private final int port;

    // HashMaps para realizar buscas, verificações e acessos concorrentes.
    private static final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<String, ClientHandler>();
    private static final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<String, Room>();


    // Construtor da classe servidor.
    public Server(String port) {
        this.port = Integer.parseInt(port);
    }

    // Método que inicializa o servidor.
    public void run() {
        serverLog("Inicialiazando o servidor...");

        try {
            // Inicializa o soquete do servidor.
            ServerSocket serverSocket = new ServerSocket(this.port);
            serverLog("O servidor está rodando e aguardando por conexões.");

            // Enquanto o servidor estiver rodando, aguarda novas conexões serem identificadas.
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

    // Método que Exibe logs do servidor no console.
    private static void serverLog(String log) {
        System.out.println(log);
    }

    // Método que verifica se o nome de usuário já esta sendo utilizado.
    private static boolean isUsernameTaken(String username) throws IOException {
        return clients.containsKey(username);
    }
    

    // --------------------------------------------------------------------------------------------------------------


    // CLASSE PARA AS SALAS.
    private static class Room {
        // Nomes de usuários dos membros.
        private HashSet<String> members;
        private String roomName;
        private String admin;
        private String hashedPassword;
        private boolean isPrivate;



        // Construtor.
        public Room(String roomName, String admin, String hashedPassword) {
            this.roomName = roomName;
            this.admin = admin;
            this.hashedPassword = hashedPassword;

            this.members = new HashSet<String>();

            isPrivate = hashedPassword != null;
        }



        // Método para verificar se uma sala é privada.
        public boolean getIsPrivate() {
            return isPrivate;
        }

        // Método para verificar se um usuário já é membro da sala.
        public boolean isMember(String username) {
            return this.members.contains(username) || this.admin.equals(username);
        }

        // Método para verificar se um usuário é admin da sala.
        public boolean isAdmin(String username) {
            return this.admin.equals(username);
        }



        // Método que cria as salas publicas ou privadas.
        public void createRoom(String roomName, String admin, String hashedPassword) {
            if (!rooms.containsKey(roomName)) {
                Room room = new Room(roomName, admin, hashedPassword);
                rooms.put(roomName, room);

                ClientHandler roomAdmin = clients.get(this.admin);
                roomAdmin.sendCommand("CRIAR_SALA_OK");
            } else {
                ClientHandler roomAdmin = clients.get(this.admin);
                roomAdmin.sendCommand("ERRO Uma sala já existe com esse nome!");
            }
        }

        // Método para o cliente entrar na sala.
        public void joinRoom(String member) {
            notifyMembersAboutNewMember(member);
            addMember(member);
        }

        // Método que adiciona novo membro à sala.
        public void addMember(String username) {
            this.members.add(username);
        }

        // Método que lista todos os membros da sala.
        public String listAllRoomMembers() {
            return this.admin + " " + members.stream().collect(Collectors.joining(" "));
        }

        // Método que valida a senha hash.
        public boolean validateHashedPassword(String hashedPassword) {
            // DISCUTIR ISSO AAAAAAAAAAAAAAAAAH
            return (this.hashedPassword != null && !this.hashedPassword.equals(hashedPassword)) ? false : true;
        }

        // Método para o usuário sair de uma sala.
        public void exitRoom(String member) {
            this.removeMember(member);
            this.notifyMembersAboutMemberExit(member);
        }

        // Método que remove um membro da sala.
        public void removeMember(String member) {
            if (this.admin.equals(member)) {
                ClientHandler client = clients.get(member);
                client.sendCommand("ERRO : O administrador não pode ser removido!");
                return;
            }
            this.members.remove(member);
        }

        // Método para banir um usuário.
        public void banUser(String member) {
            if (member != null) {
                this.members.remove(member);
                notifyAboutUserBan(member);
            }
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



        // Método que notifica todos os membros de uma sala sobre o novo membro.
        public void notifyMembersAboutNewMember(String username) {
            String command = "ENTROU " + this.roomName + " " + username;

            ClientHandler admin = clients.get(this.admin);
            admin.sendCommand(command);

            for (String member : this.members) {
                ClientHandler client = clients.get(member);
                client.sendCommand(command);
            }
        }

        // Método que informa os membros de uma sala sobre a saída de um membro.
        public void notifyMembersAboutMemberExit(String username) {
            String command = "SAIU " + this.roomName + " " + username;

            ClientHandler admin = clients.get(this.admin);
            admin.sendCommand(command);

            for (String member : this.members) {
                ClientHandler client = clients.get(member);
                client.sendCommand(command);
            }
        }

        // Método que notifica os membros de uma sala sobre o fechamento da sala.
        public void notifyMembersAboutRoomClose() {
            String command = "SALA_FECHADA " + this.roomName;

            ClientHandler admin = clients.get(this.admin);
            admin.sendCommand(command);

            for (String member : this.members) {
                ClientHandler client = clients.get(member);
                client.sendCommand(command);
            }
        }

        //Método que notifica os membros de uma sala sobre o banimento de um usuário da sala.
        public void notifyAboutUserBan(String member) {
            // Mensagem para o admin da sala.
            String command = "BANIMENTO_OK " + member;
            ClientHandler admin = clients.get(this.admin);
            admin.sendCommand(command);

            // Mensagem para o usuário banido.
            command = "BANIDO_DA_SALA " + this.roomName;
            ClientHandler member_banned = clients.get(member);
            member_banned.sendCommand(command);

            // Mensagem para todos os outros membros da sala.
            command = "SAIU " + this.roomName + " " + member;
            for (String roomMamber : this.members) {
                ClientHandler client = clients.get(roomMamber);
                client.sendCommand(command);
            }
        }
    }


    // ---------------------------------------------------


    // CLASSE PARA OS CLIENTES CONECTADOS.
    private static class ClientHandler implements Runnable {
        private String username;   // Nome do usuário.
        private Socket socket;     // Socket do usuário.
        private PrintWriter out;   // Buffer de saída do usuário.
        private BufferedReader in; // Buffer de saída do usuário.
        private KeyPair keyPair;   // Senha do usuário.



        // Construtor da classe ClientHandler.
        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.username = null;

            try {
                // Cria streams de entrada e saída para o cliente através do seu socket.
                out = new PrintWriter(socket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }



        // Método que inicializa a execucao do cliente.
        @Override
        public void run() {
            try {
                // Registra o socket e nome de usuário no hashmap clients.
                this.registerClient();

                // Criptografa a conexão entre o cliente e o servidor.
                //this.authenticateClient();

                while (true) {
                    String command = recieveCommand();
                    String[] splitedCommand = command.split(" ");

                    switch (splitedCommand[0]) {
                        case "CRIAR_SALA":
                            // Lida com o restante do comando em outro método.
                            handleRoomCreation(splitedCommand);
                            break;
                        
                        case "LISTAR_SALAS":
                            handleListRooms(splitedCommand);
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
                            handleCloseRoom(splitedCommand);
                            break;
                        
                        case "BANIR_USUARIO":
                            handleBanUser(splitedCommand);
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

            // Permanece nesse método até obter um nome de usuário válid ou a conexão ser fechada.
            while (true) {
                // Deve ter o formato REGISTRO <username>.
                String command = recieveCommand();
                String[] splitedCommand = command.split(" ");

                // Valida quantidade de argumentos.
                if (splitedCommand.length == 2) {
                    String action = splitedCommand[0];

                    // Valida o comando para registrar-se.
                    if (action.equals("REGISTRO")) {
                        String username = splitedCommand[1];

                        // Valida disponibilidade do nome de usuário.
                        if (!isUsernameTaken(username)) {
                            this.username = username;
                            clients.put(username, this);
                            sendCommand("REGISTRO_OK");
                            return;
                        }
                        else {
                            sendCommand("ERRO : Nome de usuário em uso! Tente novamente.");
                        }
                    }
                    else {
                        sendCommand("ERRO : Comando inválido ou não esparado!");
                    }
                }
                else {
                    sendCommand("ERRO : Comando ou argumentos inválidos! Tente REGISTRO <nome_de_usuário>.");
                }
            }
        }

        // Método que criar uma conexão criptografada entre o cliente e o servidor.
        public boolean authenticateClient() throws IOException {
            String command = recieveCommand();
            String[] splitedCommand = command.split(" ");

            if (splitedCommand.length != 2) {
                sendCommand("ERRO Comando inválido ou com número errado de argumentos!");
                return false;
            }

            if (!splitedCommand[0].equals("AUTENTICACAO")) {
                sendCommand("ERRO Comando inesperado!");
                return false;
            }

            try {
                this.generateKeyPair();
            } catch (NoSuchAlgorithmException e) {
                sendCommand("ERRO Erro no servidor!");
                serverLog(e.toString());
                return false;
            }

            String publicKey = this.getPublicKey();
            sendCommand("CHAVE_PUBLICA " + this.encodeBase64(publicKey));

            // Falta coisas aqui

            return true;
        }

        // Método que recebe as streams de comandos do servidor.
        private String recieveCommand() throws IOException {
            return in.readLine();
        }

        // Método que envia as streams dos comandos para cliente.
        private void sendCommand(String command) {
            out.println(command);
        }



        // Método para criar uma sala.
        private void handleRoomCreation(String[] splitedCommand) {
            if (splitedCommand.length <= 2 || splitedCommand.length >= 5) {
                sendCommand("ERRO : Argumentos faltando em CRIAR_SALA!");
                return;
            }

            switch (splitedCommand[1]) {
                case "PUBLICA":
                    if (splitedCommand.length != 3) {
                        sendCommand("ERRO : Quantidade errada de argumentos em CRIAR_SALA!");
                    }
                    rooms.get(splitedCommand[2]).createRoom(splitedCommand[2], this.username, null);
                    break;

                case "PRIVADA":
                    if (splitedCommand.length != 4) {
                        sendCommand("ERRO : Quantidade errada de argumentos em CRIAR_SALA!");
                        return;
                    }
                    rooms.get(splitedCommand[2]).createRoom(splitedCommand[2], this.username, splitedCommand[3]);
                    break;

                default:
                    sendCommand("ERRO : Argumento inválido em CRIAR_SALA!");
                    break;
            }
        }

        // Método para listar todas as salas.
        private void handleListRooms(String[] splitedCommand) {
            if (splitedCommand.length != 1) {
                sendCommand("ERRO : Argumentos inválidos para o comando LISTAR_SALAS");
            }

            String concatenatedRoomsNames = rooms.entrySet().stream()
                    .filter(entry -> entry.getKey() instanceof String && entry.getValue() instanceof Room)
                    .map(entry -> {
                        String name = entry.getKey();
                        Room room = entry.getValue();
                        String availabilityString = room.getIsPrivate() ? "Privada" : "Publica";
                        return name + "(" + availabilityString + ")";
                    })
                    .collect(Collectors.joining(" "));

            sendCommand("SALAS " + concatenatedRoomsNames);
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

            if (room.getIsPrivate()) {
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

            rooms.get(roomName).joinRoom(this.username);
            sendCommand("ENTRAR_SALA_OK " + room.listAllRoomMembers());
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

            room.exitRoom(this.username);
            sendCommand("SAIR_SALA_OK");
        }

        // Método que fecha uma sala.
        private void handleCloseRoom(String[] splitedCommand) { 
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

            if (!room.isAdmin(this.username)) {
                sendCommand("ERRO Apenas o admin pode fechar a sala!");
                return;
            }

            room.notifyMembersAboutRoomClose();
            rooms.remove(roomName);

            sendCommand("FECHAR_SALA_OK");
        }

        // Método para banir um usuário de uma sala.
        private void handleBanUser(String[] splitedCommand) {
            // Verifica a quantidade de argumentos lidos.
            if (splitedCommand.length != 3) {
                sendCommand("ERRO : Argumentos inválidos ou incompletos para o comando BANIR_USUARIO.");
                return;
            }

            // Verifica se a sala existe.
            if (!rooms.containsKey(splitedCommand[1])) {
                sendCommand("ERRO : A sala informada nao existe.");
                return;
            }

            // Verifica se o banidor é o admin da sala.
            if (!Objects.equals(rooms.get(splitedCommand[1]).admin, this.username)) {
                sendCommand("ERRO : Voce não é o admin da sala.");
                return;
            }

            // Verifica que o usuário a ser banido é membro da sala.
            if (!rooms.get(splitedCommand[1]).isMember(splitedCommand[2])) {
                sendCommand("ERRO : Usuario informado não é membro da sala.");
                return;
            }

            // Verifica se o usuário a ser banido é o próprio usuário banidor.
            if (Objects.equals(this.username, splitedCommand[2])) {
                sendCommand("ERRO : Você não pode banir-se da sala.");
                return;
            }

            rooms.get(splitedCommand[1]).banUser(splitedCommand[2]);
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

            String message = splitedCommand[2];

            for (int i = 3; i < splitedCommand.length; i++) {
                message = message.concat(" " + splitedCommand[i]);
            }

            room.sendCommandForMembers(this.username, message);
        }




        private void generateKeyPair() throws NoSuchAlgorithmException {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);
            this.keyPair = keyPairGenerator.generateKeyPair();
        }
        private String getPublicKey() {
            Key publicKey = keyPair.getPublic();
            return publicKey.toString();
        }
        private String getPrivateKey() {
            Key privateKey = keyPair.getPrivate();
            return privateKey.toString();
        }
        private String decodeBase64(String string) {
            byte[] decodedBytes = Base64.getDecoder().decode(string);
            return new String(decodedBytes);
        }
        private String encodeBase64(String string) {
            byte[] stringBytes = string.getBytes();
            return Base64.getEncoder().encodeToString(stringBytes);
        }
    }
}
