package pe.axiz.payments.application;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.stereotype.Service;
import pe.axiz.payments.domain.*;

@Service
public class PaymentService {
  private final PaymentRepository repository;

  public PaymentService(PaymentRepository repository) {
    this.repository = repository;
  }

  public Payment create(String owner, String key, BigDecimal amount, String currency) {
    return repository.create(owner, key, amount, currency);
  }

  public List<Payment> find(String owner) {
    return repository.find(owner);
  }

  public boolean approve(UUID id, String owner) {
    return repository.approve(id, owner);
  }
}
