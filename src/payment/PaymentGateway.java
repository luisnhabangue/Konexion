package payment;

import model.enums.PaymentMethod;

public interface PaymentGateway {
    PaymentMethod method();
    PaymentResponse charge(PaymentRequest request);
}