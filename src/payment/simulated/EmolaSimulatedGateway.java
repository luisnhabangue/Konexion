package payment.simulated;

import model.enums.PaymentMethod;

/**
 * Implementação simulada do gateway de pagamento e-Mola[cite: 7].
 *
 * @since 1.0[cite: 7]
 */
public class EmolaSimulatedGateway extends AbstractSimulatedGateway {

    /**
     * {@inheritDoc}
     *
     * @return O método {@link PaymentMethod#EMOLA}[cite: 7].
     */
    @Override public PaymentMethod method() { return PaymentMethod.EMOLA; }

    /**
     * {@inheritDoc}
     *
     * @return O prefixo {@code "EM"}[cite: 7].
     */
    @Override protected String prefix() { return "EM"; }

    /**
     * Valida se o número e-Mola possui 9 dígitos e começa pelos prefixos 86 ou 87[cite: 7].
     *
     * @param a O número de conta a ser validado[cite: 7].
     * @return {@code true} se for válido; {@code false} caso contrário[cite: 7].
     */
    @Override protected boolean isValidAccount(String a) { return a.matches("^(86|87)\\d{7}$"); }

    /**
     * {@inheritDoc}
     *
     * @return Mensagem indicando número e-Mola inválido[cite: 7].
     */
    @Override protected String invalidAccountMessage() { return "Número e-Mola inválido (deve começar por 86 ou 87)."; }
}