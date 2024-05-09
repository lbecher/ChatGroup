package br.unioeste;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            Main.help();
            return;
        }

        // Verificando os argumentos passados
        String argumento = args[0];

        if (argumento.equals("--servidor") || argumento.equals("-s")) {
            // Criar uma instância do servidor
            Servidor servidor = new Servidor();
            servidor.run();
        }
        else if (argumento.equals("--cliente") || argumento.equals("-c")) {
            if (args.length == 1) {
                Main.help();
                return;
            }

            String servidor_url = args[1];

            // Criar uma instância do cliente
            Cliente cliente = new Cliente(servidor_url);
            cliente.run();
        }
        else {
            Main.help();
            return;
        }
    }

    private static void help() {
        System.out.println("Uso: java Main [--servidor | -s] [--cliente | -c SERVIDOR_URL ]");
        System.out.println("Onde SERVIDOR_URL define-se como IP:PORTA.");
    }
}