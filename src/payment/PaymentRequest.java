package payment;

import java.math.BigDecimal;

/**
 * Registo que transporta os dados de uma solicitação de pagamento[cite: 3].
 *
 * @param account O número da conta ou número de telefone associado ao pagamento[cite: 3].
 * @param amount O valor monetário a ser cobrado[cite: 3].
 * @param reference A referência única do pedido ou transação[cite: 3].
 * @param description A descrição detalhada do pagamento[cite: 3].
 *
 * @since 1.0[cite: 3]
 */
public record PaymentRequest(String account, BigDecimal amount, String reference, String description) {}