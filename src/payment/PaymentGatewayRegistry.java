package payment;

import model.enums.PaymentMethod;
import payment.simulated.BimSimulatedGateway;
import payment.simulated.EmolaSimulatedGateway;
import payment.simulated.MpesaSimulatedGateway;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registo central responsável por gerir e fornecer as instâncias de {@link PaymentGateway}[cite: 2].
 *
 * @since 1.0[cite: 2]
 */
public class PaymentGatewayRegistry {

    /** Mapa que associa cada método de pagamento à sua respetiva implementação de gateway[cite: 2]. */
    private final Map<PaymentMethod, PaymentGateway> gateways = new EnumMap<>(PaymentMethod.class);

    /**
     * Cria o registo e inicializa por omissão os gateways simulados de M-Pesa, e-Mola e BIM[cite: 2].
     */
    public PaymentGatewayRegistry() {
        register(new MpesaSimulatedGateway());
        register(new EmolaSimulatedGateway());
        register(new BimSimulatedGateway());
    }

    /**
     * Regista uma nova implementação de gateway de pagamento[cite: 2].
     *
     * @param gateway O gateway a ser registado[cite: 2].
     */
    public void register(PaymentGateway gateway) {
        gateways.put(gateway.method(), gateway);
    }

    /**
     * Obtém o gateway associado ao método de pagamento especificado[cite: 2].
     *
     * @param method O método de pagamento pretendido[cite: 2].
     * @return A instância de {@link PaymentGateway} correspondente[cite: 2].
     * @throws IllegalArgumentException Se não existir nenhum gateway registado para o método[cite: 2].
     */
    public PaymentGateway get(PaymentMethod method) {
        PaymentGateway gateway = gateways.get(method);
        if (gateway == null) {
            throw new IllegalArgumentException("Método sem gateway: " + method);
        }
        return gateway;
    }
}