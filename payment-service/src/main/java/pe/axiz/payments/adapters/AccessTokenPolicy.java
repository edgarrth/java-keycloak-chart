package pe.axiz.payments.adapters;

import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;

public final class AccessTokenPolicy implements OAuth2TokenValidator<Jwt> {
  public OAuth2TokenValidatorResult validate(Jwt token) {
    String expected =
        switch (String.valueOf(token.getClaimAsString("auth_flow"))) {
          case "code" -> "5";
          case "password" -> "4";
          case "phone" -> "3";
          default -> "";
        };
    if (token.getSubject() != null
        && !token.getSubject().isBlank()
        && token.getAudience() != null
        && token.getAudience().contains("payments-api")
        && !expected.isEmpty()
        && expected.equals(token.getClaimAsString("acr")))
      return OAuth2TokenValidatorResult.success();
    return OAuth2TokenValidatorResult.failure(
        new OAuth2Error(
            "invalid_token", "Audiencia o contexto de autenticación incorrectos", null));
  }
}
