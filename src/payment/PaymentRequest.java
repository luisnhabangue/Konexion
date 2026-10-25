package payment;

import java.math.BigDecimal;

/**
 * Registo que transporta os dados de uma solicitação de pagamento .
 *
 * @param account O número da conta ou número de telefone associado ao pagamento.
 * @param amount O valor monetário a ser cobrado.
 * @param reference A referência única do pedido ou transação.
 * @param description A descrição detalhada do pagamento.
 *
 * @since 1.0
 */
public record PaymentRequest(String account, BigDecimal amount, String reference, String description) {}