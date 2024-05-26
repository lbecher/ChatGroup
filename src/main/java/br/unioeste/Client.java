package br.unioeste;

import java.io.BufferedReader;
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

    public Client(String server, int port) {
        this.server = server;
        this.port = port;
    }

    public void run() {
        clientLog("Iniciando o cliente...");

        try {
            startSocket();
            
            registerClient();

            /*if (!authenticateClient()) {
                return;
            }*/

            while (true) {
                String command = recieveCommand();
                String[] splitedCommand = command.split(" ", 1);

                switch (splitedCommand[0]) {
                    case "ERRO":
                        handleError(splitedCommand[1]);
                        break;

                    default:
                        handleError("Comando inválido ou não esparado!");
                        break;
                }
            }
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

            String command = recieveCommand();

            if (command.equals("REGISTRO_OK")) {
                break;
            }
        }
    }

    private boolean authenticateClient() throws Exception {
        sendCommand("AUTENTICACAO " + username);

        String command = recieveCommand();
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

        aesKey = generateAesKey();

        byte[] encryptedAesKey = encryptRsa(aesKey.getEncoded(), publicKey);
        String encodedAesKey = encodeBase64(encryptedAesKey);
        sendCommand("CHAVE_SIMETRICA " + encodedAesKey);

        return true;
    }


    // Método que recebe as streams de comandos do servidor.
    private String recieveCommand() throws Exception {
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
                command = encodeBase64(command.getBytes());
                byte[] bytes = encryptAes(command.getBytes(), aesKey);
                command = new String(bytes);
            } catch (Exception e) {
                handleError("Erro ao criptografar o comando enviado!");
                e.printStackTrace();
            }
        }
        out.println(command);
    }



    private void handleError(String error) {
        // provisório, o ideal é isso aparecer na interface
        clientLog(error);
    }



    private void clientLog(String log) {
        System.out.println(log);
    }
}
