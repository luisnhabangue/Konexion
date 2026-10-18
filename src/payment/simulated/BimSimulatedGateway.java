package payment.simulated;

import model.enums.PaymentMethod;

public class BimSimulatedGateway extends AbstractSimulatedGateway {
    @Override public PaymentMethod method() { return PaymentMethod.BIM; }
    @Override protected String prefix() { return "BIM"; }
    @Override protected boolean isValidAccount(String a) { return a.matches("^\\d{10,14}$"); }
    @Override protected String invalidAccountMessage() { return "Número de conta BIM inválido."; }
}