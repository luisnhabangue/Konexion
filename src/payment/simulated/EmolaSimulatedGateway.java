package payment.simulated;

import model.enums.PaymentMethod;

public class EmolaSimulatedGateway extends AbstractSimulatedGateway {
    @Override public PaymentMethod method() { return PaymentMethod.EMOLA; }
    @Override protected String prefix() { return "EM"; }
    @Override protected boolean isValidAccount(String a) { return a.matches("^(86|87)\\d{7}$"); }
    @Override protected String invalidAccountMessage() { return "Número e-Mola inválido (deve começar por 86 ou 87)."; }
}