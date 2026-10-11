package pe.axiz.payments.adapters;

import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import pe.axiz.payments.domain.*;

@Repository
public class JdbcPaymentRepository implements PaymentRepository {
  private final JdbcTemplate jdbc;

  public JdbcPaymentRepository(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Transactional
  public Payment create(String owner, String key, BigDecimal amount, String currency) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "insert into payment(id,owner,idempotency_key,amount,currency,status) values(?,?,?,?,?,'PENDING') on conflict(owner,idempotency_key) do nothing",
        id,
        owner,
        key,
        amount,
        currency);
    Payment result =
        jdbc.queryForObject(
            "select * from payment where owner=? and idempotency_key=?",
            (r, n) ->
                new Payment(
                    r.getObject("id", UUID.class),
                    r.getString("owner"),
                    r.getBigDecimal("amount"),
                    r.getString("currency"),
                    r.getString("status")),
            owner,
            key);
    if (result.amount().compareTo(amount) != 0 || !result.currency().equals(currency))
      throw new IllegalArgumentException("La clave de idempotencia ya contiene otro pago");
    return result;
  }

  public List<Payment> find(String owner) {
    return jdbc.query(
        "select * from payment where owner=? order by created_at",
        (r, n) ->
            new Payment(
                r.getObject("id", UUID.class),
                r.getString("owner"),
                r.getBigDecimal("amount"),
                r.getString("currency"),
                r.getString("status")),
        owner);
  }

  public boolean approve(UUID id, String owner) {
    return jdbc.update(
            "update payment set status='APPROVED' where id=? and owner=? and status='PENDING'",
            id,
            owner)
        == 1;
  }
}
