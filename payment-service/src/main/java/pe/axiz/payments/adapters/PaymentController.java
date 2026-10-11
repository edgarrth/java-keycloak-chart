package pe.axiz.payments.adapters;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pe.axiz.payments.application.PaymentService;
import pe.axiz.payments.domain.Payment;

@RestController
@RequestMapping("/api")
public class PaymentController {
  private final PaymentService service;

  public PaymentController(PaymentService service) {
    this.service = service;
  }

  public record Request(
      @NotNull @DecimalMin("0.01") @DecimalMax("1000000.00") @Digits(integer = 7, fraction = 2)
          BigDecimal amount,
      @Pattern(regexp = "PEN|USD") @NotNull String currency) {}

  @GetMapping("/identity")
  public Map<String, Object> identity(@AuthenticationPrincipal Jwt jwt) {
    return Map.of(
        "subject",
        jwt.getSubject(),
        "acr",
        jwt.getClaimAsString("acr"),
        "flow",
        jwt.getClaimAsString("auth_flow"));
  }

  @GetMapping("/payments")
  @PreAuthorize("@acr.allowed(authentication, 3)")
  public List<Payment> list(@AuthenticationPrincipal Jwt jwt) {
    return service.find(jwt.getSubject());
  }

  @PostMapping("/payments")
  @PreAuthorize("@acr.allowed(authentication, 4)")
  public Payment create(
      @AuthenticationPrincipal Jwt jwt,
      @RequestHeader("Idempotency-Key") @Pattern(regexp = "[A-Za-z0-9-]{8,80}") String key,
      @Valid @RequestBody Request request) {
    return service.create(jwt.getSubject(), key, request.amount(), request.currency());
  }

  @PostMapping("/payments/{id}/approve")
  @PreAuthorize("@acr.allowed(authentication, 5)")
  public ResponseEntity<?> approve(@AuthenticationPrincipal Jwt jwt, @PathVariable("id") UUID id) {
    return service.approve(id, jwt.getSubject())
        ? ResponseEntity.ok(Map.of("status", "APPROVED"))
        : ResponseEntity.notFound().build();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<?> conflict(IllegalArgumentException e) {
    return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
  }
}
