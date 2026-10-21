package test;

import service.PasswordRecoveryService;

public class TestRecoveryPassword {

    public static void main(String[] args) {

        PasswordRecoveryService service =
                new PasswordRecoveryService();

        try {
            service.sendCode("nhabangueluis@gmail.com");

            System.out.println("Código enviado com sucesso!");

        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
