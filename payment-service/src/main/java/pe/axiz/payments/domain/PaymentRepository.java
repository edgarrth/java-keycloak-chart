package pe.axiz.payments.domain;

import java.math.BigDecimal;
import java.util.*;

public interface PaymentRepository {
  Payment create(String owner, String key, BigDecimal amount, String currency);

  List<Payment> find(String owner);

  boolean approve(UUID id, String owner);
}
