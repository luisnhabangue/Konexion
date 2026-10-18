package test;

import model.enums.PaymentMethod;
import payment.PaymentGatewayRegistry;
import payment.PaymentRequest;
import java.math.BigDecimal;

public class TestePagamento {
    public static void main(String[] args) {
        var registry = new PaymentGatewayRegistry();
        String[][] casos = {
                {"MPESA", "841234567"},   // sucesso
                {"MPESA", "841234560"},   // saldo insuficiente
                {"MPESA", "841234569"}, // timeout
                {"MPESA", "831234567"},   // numero invalido
                {"EMOLA", "861234567"},   // sucesso
        };
        for (String[] c : casos) {
            var resp = registry.get(PaymentMethod.valueOf(c[0]))
                    .charge(new PaymentRequest(c[1], new BigDecimal("500"), "TESTE", "teste"));
            System.out.println(c[0] + " " + c[1] + " -> " + resp.success() + " | " + resp.message());
        }
    }
}