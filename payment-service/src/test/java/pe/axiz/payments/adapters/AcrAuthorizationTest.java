package pe.axiz.payments.adapters;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class AcrAuthorizationTest {
  private JwtAuthenticationToken token(String acr, boolean role) {
    return new JwtAuthenticationToken(
        Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .claim("sub", "merchant")
            .claim("acr", acr)
            .build(),
        role ? List.of(new SimpleGrantedAuthority("ROLE_merchant")) : List.of());
  }

  @Test
  void appliesAllThresholds() {
    AcrAuthorization policy = new AcrAuthorization();
    for (int level = 3; level <= 5; level++)
      for (int minimum = 3; minimum <= 5; minimum++)
        assertEquals(level >= minimum, policy.allowed(token("" + level, true), minimum));
  }

  @Test
  void rejectsUntrustedClaims() {
    AcrAuthorization policy = new AcrAuthorization();
    for (String invalid : List.of("0", "6", "500", "invalid", ""))
      assertFalse(policy.allowed(token(invalid, true), 3));
    assertFalse(policy.allowed(token("5", false), 3));
  }
}
