package payment;

import model.enums.PaymentMethod;
import payment.simulated.BimSimulatedGateway;
import payment.simulated.EmolaSimulatedGateway;
import payment.simulated.MpesaSimulatedGateway;

import java.util.EnumMap;
import java.util.Map;

public class PaymentGatewayRegistry {

    private final Map<PaymentMethod, PaymentGateway> gateways = new EnumMap<>(PaymentMethod.class);

    public PaymentGatewayRegistry() {
        register(new MpesaSimulatedGateway());
        register(new EmolaSimulatedGateway());
        register(new BimSimulatedGateway());
    }

    public void register(PaymentGateway gateway) {
        gateways.put(gateway.method(), gateway);
    }

    public PaymentGateway get(PaymentMethod method) {
        PaymentGateway gateway = gateways.get(method);
        if (gateway == null) {
            throw new IllegalArgumentException("Método sem gateway: " + method);
        }
        return gateway;
    }
}