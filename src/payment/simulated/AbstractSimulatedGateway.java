package payment.simulated;

import payment.PaymentGateway;
import payment.PaymentRequest;
import payment.PaymentResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Classe base abstrata para a simulação de gateways de pagamento[cite: 5].
 * <p>
 * Implementa validações de conta e de montante, simulação de latência de rede
 * e cenários de teste baseados no último dígito da conta informada[cite: 5].
 * </p>
 *
 * @since 1.0[cite: 5]
 */
public abstract class AbstractSimulatedGateway implements PaymentGateway {

    /** Limite máximo permitido por transação (100 000)[cite: 5]. */
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("100000");

    /**
     * Retorna o prefixo do código de referência gerado para a transação[cite: 5].
     *
     * @return O prefixo textual do provedor[cite: 5].
     */
    protected abstract String prefix();

    /**
     * Valida o formato da conta de acordo com as regras específicas do gateway[cite: 5].
     *
     * @param account O número de conta a validar[cite: 5].
     * @return {@code true} se a conta for válida; {@code false} caso contrário[cite: 5].
     */
    protected abstract boolean isValidAccount(String account);

    /**
     * Retorna a mensagem de erro a ser exibida em caso de conta inválida[cite: 5].
     *
     * @return A mensagem de erro da conta[cite: 5].
     */
    protected abstract String invalidAccountMessage();

    /**
     * Executa a cobrança simulada, aplicando validações de conta, saldo e limites[cite: 5].
     *
     * @param req O pedido de pagamento a ser processado[cite: 5].
     * @return O {@link PaymentResponse} contendo o resultado da simulação[cite: 5].
     */
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

    /**
     * Constrói uma resposta de falha com o código e a mensagem especificados[cite: 5].
     *
     * @param code O código de erro do provedor[cite: 5].
     * @param msg A mensagem descritiva da falha[cite: 5].
     * @return Um {@link PaymentResponse} com estado de insucesso[cite: 5].
     */
    private PaymentResponse fail(String code, String msg) {
        return new PaymentResponse(false, null, code, msg, LocalDateTime.now());
    }

    /**
     * Simula o atraso de resposta da rede durante o processamento (800ms)[cite: 5].
     */
    private void simulateLatency() {
        try { Thread.sleep(800); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}