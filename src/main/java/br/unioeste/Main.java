package br.unioeste;

public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            Main.help();
            return;
        }

        // Verificando os argumentos passados.
        String argumento = args[0];

        if (argumento.equals("--servidor") || argumento.equals("-s")) {
            String port = args[1];

            // Criar uma instância do servidor e executa.
            Server server = new Server(port);
            server.run();
        }
        else if (argumento.equals("--cliente") || argumento.equals("-c")) {
            String servidor_url = args[1];

            // Criar uma instância do cliente e executa.
            Cliente cliente = new Cliente(servidor_url);
            cliente.run();
        }
        else {
            Main.help();
            return;
        }
    }

    private static void help() {
        System.out.println("Iniciar servidor:  java Main [ --servidor | -s ] PORTA");
        System.out.println("Iniciar cliente:   java Main [ --cliente  | -c ] [ IP | DOMINIO ]:PORTA");
    }
}