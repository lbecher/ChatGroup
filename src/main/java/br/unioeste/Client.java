package br.unioeste;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
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
             * O código a partir é provisório, feito para testes!
             */

            // Thread para receber mensagens do servidor
            Thread serverReaderThread = new Thread(() -> {
                try {
                    while (true) {
                        System.out.println(receiveCommand());
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
                        sendCommand(userInput);
                    }
                } catch (IOException e) {
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
            else if (command.equals("ERRO")) {
                String error = command.split(" ", 1)[1];
                handleError(error);
            }
            else {
                handleError("Comando inesperado durante o registro!");
            }
        }
    }

    private boolean authenticateClient() throws Exception {
        sendCommand("AUTENTICACAO " + username);

        String command = receiveCommand();
        String[] splitedCommand = command.split(" ");

        if (splitedCommand.length != 2) {
            handleError("Comando inválido ou com número errado de argumentos!");
            return false;
        }

        if (!splitedCommand[0].equals("CHAVE_PUBLICA")) {
            handleError("Comando inesperado!");
            return false;
        }

        String publicKeyBase64 = splitedCommand[1];
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
                byte[] bytes = encryptAes(command.getBytes(), aesKey);
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
