package payment.simulated;

import model.enums.PaymentMethod;

public class MpesaSimulatedGateway extends AbstractSimulatedGateway {
    @Override public PaymentMethod method() { return PaymentMethod.MPESA; }
    @Override protected String prefix() { return "MP"; }
    @Override protected boolean isValidAccount(String a) { return a.matches("^(84|85)\\d{7}$"); }
    @Override protected String invalidAccountMessage() { return "Número M-Pesa inválido (deve começar por 84 ou 85)."; }
}