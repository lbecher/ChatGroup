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
    private PublicKey publicKey; // Par de chaves.
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

            if (!authenticateClient()) {
                return;
            }

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
        sendCommand("REGISTRO asta");

        String command = recieveCommand();
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
        publicKey = decodePublicKeyBase64(publicKeyBase64);

        aesKey = generateAesKey();
        String encryptedAesKeyBase64 = encryptRsaBase64(aesKey, publicKey);
        sendCommand("CHAVE_SIMETRICA " + encryptedAesKeyBase64);

        return true;
    }


    // Método que recebe as streams de comandos do servidor.
    private String recieveCommand() throws Exception {
        String command = in.readLine();
        if (aesKey != null) {
            try {
                command = decryptAes(command, aesKey);
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
                command = encryptAes(command, aesKey);
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
