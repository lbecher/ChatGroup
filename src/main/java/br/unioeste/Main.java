package br.unioeste;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Uso: java Main [--servidor | -s] [--cliente | -c]");
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
            // Criar uma instância do cliente
            Cliente cliente = new Cliente();
            cliente.run();
        }
        else {
            System.out.println("Argumento inválido. Use --servidor | -s para o servidor, ou --cliente | -c para o cliente.");
            return;
        }
    }
}