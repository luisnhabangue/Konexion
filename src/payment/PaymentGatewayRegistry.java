package payment;

import model.enums.PaymentMethod;
import payment.simulated.BimSimulatedGateway;
import payment.simulated.EmolaSimulatedGateway;
import payment.simulated.MpesaSimulatedGateway;

import java.util.EnumMap;
import java.util.Map;

/**
 * Registo central responsável por gerir e fornecer as instâncias de {@link PaymentGateway}.
 *
 * @since 1.0
 */
public class PaymentGatewayRegistry {

    /** Mapa que associa cada método de pagamento à sua respetiva implementação de gateway */
    private final Map<PaymentMethod, PaymentGateway> gateways = new EnumMap<>(PaymentMethod.class);

    /**
     * Cria o registo e inicializa por omissão os gateways simulados de M-Pesa, e-Mola e BIM.
     */
    public PaymentGatewayRegistry() {
        register(new MpesaSimulatedGateway());
        register(new EmolaSimulatedGateway());
        register(new BimSimulatedGateway());
    }

    /**
     * Regista uma nova implementação de gateway de pagamento.
     *
     * @param gateway O gateway a ser registado.
     */
    public void register(PaymentGateway gateway) {
        gateways.put(gateway.method(), gateway);
    }

    /**
     * Obtém o gateway associado ao método de pagamento especificado.
     *
     * @param method O método de pagamento pretendido.
     * @return A instância de {@link PaymentGateway} correspondente.
     * @throws IllegalArgumentException Se não existir nenhum gateway registado para o método.
     */
    public PaymentGateway get(PaymentMethod method) {
        PaymentGateway gateway = gateways.get(method);
        if (gateway == null) {
            throw new IllegalArgumentException("Método sem gateway: " + method);
        }
        return gateway;
    }
}