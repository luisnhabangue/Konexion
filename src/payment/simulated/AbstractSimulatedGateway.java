package payment.simulated;

import payment.PaymentGateway;
import payment.PaymentRequest;
import payment.PaymentResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public abstract class AbstractSimulatedGateway implements PaymentGateway {

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000");

    protected abstract String prefix();
    protected abstract boolean isValidAccount(String account);
    protected abstract String invalidAccountMessage();

    @Override
    public PaymentResponse charge(PaymentRequest req) {
        simulateLatency();

        if (req.account() == null || !isValidAccount(req.account()))
            return fail("INS-2006", invalidAccountMessage());

        if (req.amount() == null || req.amount().signum() <= 0)
            return fail("INS-2001", "Valor inválido.");

        if (req.amount().compareTo(MAX_AMOUNT) > 0)
            return fail("INS-2002", "Valor excede o limite da transação.");

        char last = req.account().charAt(req.account().length() - 1);
        if (last == '0') return fail("INS-2005", "Saldo insuficiente.");
        if (last == '9') return fail("INS-9999", "Tempo de resposta excedido. Tente novamente.");

        String ref = prefix() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return new PaymentResponse(true, ref, "INS-0", "Transação concluída com sucesso.", LocalDateTime.now());
    }

    private PaymentResponse fail(String code, String msg) {
        return new PaymentResponse(false, null, code, msg, LocalDateTime.now());
    }

    private void simulateLatency() {
        try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}