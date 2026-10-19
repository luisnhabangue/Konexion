package payment.simulated;

import model.enums.PaymentMethod;

/**
 * Implementação simulada do gateway de pagamento M-Pesa[cite: 8].
 *
 * @since 1.0[cite: 8]
 */
public class MpesaSimulatedGateway extends AbstractSimulatedGateway {

    /**
     * {@inheritDoc}
     *
     * @return O método {@link PaymentMethod#MPESA}[cite: 8].
     */
    @Override public PaymentMethod method() { return PaymentMethod.MPESA; }

    /**
     * {@inheritDoc}
     *
     * @return O prefixo {@code "MP"}[cite: 8].
     */
    @Override protected String prefix() { return "MP"; }

    /**
     * Valida se o número M-Pesa possui 9 dígitos e começa pelos prefixos 84 ou 85[cite: 8].
     *
     * @param a O número de conta a ser validado[cite: 8].
     * @return {@code true} se for válido; {@code false} caso contrário[cite: 8].
     */
    @Override protected boolean isValidAccount(String a) { return a.matches("^(84|85)\\d{7}$"); }

    /**
     * {@inheritDoc}
     *
     * @return Mensagem indicando número M-Pesa inválido[cite: 8].
     */
    @Override protected String invalidAccountMessage() { return "Número M-Pesa inválido (deve começar por 84 ou 85)."; }
}