package br.unioeste;

public class Cliente {
    private String servidor_url;

    public Cliente(String servidor_url) {
        this.servidor_url = servidor_url;
    }

    public void run() {
        System.out.println("Iniciando o cliente...");
    }
}
