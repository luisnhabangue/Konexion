package payment.simulated;

import model.enums.PaymentMethod;

/**
 * Implementação simulada do gateway de pagamento Millennium BIM[cite: 6].
 *
 * @since 1.0[cite: 6]
 */
public class BimSimulatedGateway extends AbstractSimulatedGateway {

    /**
     * {@inheritDoc}
     *
     * @return O método {@link PaymentMethod#BIM}[cite: 6].
     */
    @Override public PaymentMethod method() { return PaymentMethod.BIM; }

    /**
     * {@inheritDoc}
     *
     * @return O prefixo {@code "BIM"}[cite: 6].
     */
    @Override protected String prefix() { return "BIM"; }

    /**
     * Valida se a conta informada possui entre 10 e 14 dígitos numéricos[cite: 6].
     *
     * @param a O número de conta a ser validado[cite: 6].
     * @return {@code true} se for válida; {@code false} caso contrário[cite: 6].
     */
    @Override protected boolean isValidAccount(String a) { return a.matches("^\\d{10,14}$"); }

    /**
     * {@inheritDoc}
     *
     * @return Mensagem indicando conta BIM inválida[cite: 6].
     */
    @Override protected String invalidAccountMessage() { return "Número de conta BIM inválido."; }
}