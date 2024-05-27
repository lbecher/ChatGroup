public class Main {
    public static void main(String[] args) {
        if (args.length < 2) {
            Main.help();
            return;
        }

        // Verificando os argumentos passados.
        String argumento = args[0];

        if (argumento.equals("--servidor") || argumento.equals("-s")) {
            int port = Integer.parseInt(args[1]);

            // Criar uma instância do servidor e executa.
            Server server = new Server(port);
            server.run();
        }
        else if (argumento.equals("--cliente") || argumento.equals("-c")) {
            String servidor_url = args[1];
            String[] splitted_servidor_url = servidor_url.split(":", 2);

            String server = splitted_servidor_url[0];
            int port = Integer.parseInt(splitted_servidor_url[1]);

            // Criar uma instância do cliente e executa.
            Client client = new Client(server, port);
            client.run();
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