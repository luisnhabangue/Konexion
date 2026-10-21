package payment;

import model.enums.PaymentMethod;

/**
 * Interface que define o contrato principal para os gateways de pagamento do sistema[cite: 1].
 *
 * @since 1.0[cite: 1]
 */
public interface PaymentGateway {

    /**
     * Retorna o método de pagamento associado a este gateway[cite: 1].
     *
     * @return O {@link PaymentMethod} correspondente[cite: 1].
     */
    PaymentMethod method();

    /**
     * Processa a cobrança de uma transação de pagamento[cite: 1].
     *
     * @param request Dados e parâmetros da solicitação de pagamento[cite: 1].
     * @return Um {@link PaymentResponse} contendo o resultado da transação[cite: 1].
     */
    PaymentResponse charge(PaymentRequest request);
}