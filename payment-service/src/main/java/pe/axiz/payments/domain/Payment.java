package pe.axiz.payments.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record Payment(UUID id, String owner, BigDecimal amount, String currency, String status) {}
