package test;

import model.enums.PaymentMethod;
import service.PaymentService;

public class TesteServicoPagamento {
    public static void main(String[] args) {
        // Troquem pelos IDs reais: um grupo com ciclo em andamento
        // e um utilizador que seja membro desse grupo e ainda não pagou.
        Long groupId = 1L;
        Long userId = 1L;

        var service = new PaymentService();
        var resp = service.payContribution(groupId, userId, PaymentMethod.MPESA, "841234567");
        System.out.println(resp.success() + " | " + resp.transactionRef() + " | " + resp.message());
    }
}