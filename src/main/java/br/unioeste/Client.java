package br.unioeste;

import com.google.common.hash.Hashing;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import javax.crypto.SecretKey;

public class Client extends Crypt {
    private String server;
    private int port;

    private String username;     // Nome do usuário.
    private Socket socket;       // Socket do usuário.
    private PrintWriter out;     // Buffer de saída do usuário.
    private BufferedReader in;   // Buffer de saída do usuário.
    private SecretKey aesKey;    // Chave do AES.

    private BufferedReader userInput;

    public Client(String server, int port) {
        this.server = server;
        this.port = port;
        this.userInput = new BufferedReader(new InputStreamReader(System.in));
    }

    public void run() {
        clientLog("Iniciando o cliente...");

        try {
            startSocket();
            
            registerClient();

            if (!authenticateClient()) {
                return;
            }

            /*
             * O código a partir daqui é provisório, feito para testes!
             */

            // Thread para receber mensagens do servidor
            Thread serverReaderThread = new Thread(() -> {
                try {
                    while (true) {
                        String commmand = receiveCommand();
                        String[] splittedCommmand = commmand.split(" ");

                        switch (splittedCommmand[0]) {
                            case "ERRO":
                                handleError("[SERVIDOR] " + commmand.split(" ", 2)[1]);
                                break;
                            default:
                                clientLog(commmand);
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            serverReaderThread.start();

            // Thread para ler mensagens do usuário
            Thread userReaderThread = new Thread(() -> {
                try {
                    String userInput;
                    while ((userInput = this.userInput.readLine()) != null) {
                        String[] splittedUserInput = userInput.split(" ");

                        switch (splittedUserInput[0]) {
                            case "CRIAR_SALA":
                                if (splittedUserInput.length == 4 && splittedUserInput[1].equals("PRIVADA")) {
                                    String hashedPassword = Hashing.sha256()
                                        .hashString(splittedUserInput[3], StandardCharsets.UTF_8)
                                        .toString();
                                    sendCommand("CRIAR_SALA PRIVADA " + splittedUserInput[2] + " " + hashedPassword);
                                }
                                else if (splittedUserInput.length == 3 && splittedUserInput[1].equals("PUBLICA")) {
                                    sendCommand("CRIAR_SALA PUBLICA " + splittedUserInput[2]);
                                }
                                else {
                                    handleError("O comando CRIAR_SALA está errado!");
                                }
                                break;
                            
                            case "ENTRAR_SALA":
                                if (splittedUserInput.length == 3) {
                                    String hashedPassword = Hashing.sha256()
                                        .hashString(splittedUserInput[2], StandardCharsets.UTF_8)
                                        .toString();
                                    sendCommand("ENTRAR_SALA " + splittedUserInput[1] + " " + hashedPassword);
                                }
                                else if (splittedUserInput.length == 2) {
                                    sendCommand(userInput);
                                }
                                else {
                                    handleError("O comando ENTRAR_SALA está errado!");
                                }
                                break;

                            default:
                                sendCommand(userInput);
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            userReaderThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void startSocket() throws Exception {
        socket = new Socket(server, port);

        clientLog("Conectado ao servidor!");

        out = new PrintWriter(socket.getOutputStream(), true); 
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    private void registerClient() throws Exception {

        /*
         * O código desse método é provisório, feito para testes!
         */

        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        
        while (true) {
            System.out.println("Nome de usuário:");
            String username = reader.readLine();

            sendCommand("REGISTRO " + username);

            String command = receiveCommand();

            if (command.equals("REGISTRO_OK")) {
                clientLog("REGISTRO_OK");
                break;
            }
            else if (command.split(" ", 2)[0].equals("ERRO")) {
                String error = command.split(" ", 2)[1];
                handleError("[SERVIDOR] " + error);
            }
            else {
                handleError("Comando inesperado durante o registro!");
            }
        }
    }

    private boolean authenticateClient() throws Exception {
        sendCommand("AUTENTICACAO " + username);

        String command = receiveCommand();
        String[] splittedCommand = command.split(" ");

        if (splittedCommand.length != 2) {
            handleError("Comando inválido ou com número errado de argumentos!");
            return false;
        }

        if (!splittedCommand[0].equals("CHAVE_PUBLICA")) {
            handleError("Comando inesperado!");
            return false;
        }

        String publicKeyBase64 = splittedCommand[1];
        byte[] publicKeyBytes = decodeBase64(publicKeyBase64);
        PublicKey publicKey = publicKeyFromBytes(publicKeyBytes);

        clientLog("Chave pública recebida!");

        SecretKey secretKey = generateAesKey();

        clientLog("Chave simétrica gerada!");

        byte[] encryptedAesKey = encryptRsa(secretKey.getEncoded(), publicKey);
        String encodedAesKey = encodeBase64(encryptedAesKey);
        sendCommand("CHAVE_SIMETRICA " + encodedAesKey);

        clientLog("Chave simétrica enviada!");

        aesKey = secretKey;

        return true;
    }


    // Método que recebe as streams de comandos do servidor.
    private String receiveCommand() throws Exception {
        String command = in.readLine();
        if (aesKey != null) {
            try {
                byte[] bytes = decodeBase64(command);
                bytes = decryptAes(bytes, aesKey);
                command = new String(bytes);
            } catch (Exception e) {
                handleError("Erro ao descriptografar o comando recebido!");
                e.printStackTrace();
            }
        }
        return command;
    }

    // Método que envia as streams dos comandos para o servidor.
    private void sendCommand(String command) {
        if (aesKey != null) {
            try {
                byte[] bytes = command.getBytes();
                bytes = encryptAes(bytes, aesKey);
                command = encodeBase64(bytes);
            } catch (Exception e) {
                handleError("Erro ao criptografar o comando enviado!");
                e.printStackTrace();
            }
        }
        out.println(command);
    }



    private void handleError(String error) {

        /*
         * O código desse método é provisório, feito para testes!
         */

        clientLog("[ERRO REPORTADO] " + error);
    }



    private void clientLog(String log) {
        System.out.println(log);
    }
}
