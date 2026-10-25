package payment;

import model.enums.PaymentMethod;

/**
 * Interface que define o contrato principal para os gateways de pagamento do sistema.
 *
 * @since 1.0
 */
public interface PaymentGateway {

    /**
     * Retorna o método de pagamento associado a este gateway.
     *
     * @return O {@link PaymentMethod} correspondente.
     */
    PaymentMethod method();

    /**
     * Processa a cobrança de uma transação de pagamento.
     *
     * @param request Dados e parâmetros da solicitação de pagamento.
     * @return Um {@link PaymentResponse} contendo o resultado da transação.
     */
    PaymentResponse charge(PaymentRequest request);
}