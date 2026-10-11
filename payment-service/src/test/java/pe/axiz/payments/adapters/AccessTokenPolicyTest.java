package pe.axiz.payments.adapters;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AccessTokenPolicyTest {
  private Jwt jwt(String audience, String flow, String acr) {
    var builder = Jwt.withTokenValue("test").header("alg", "RS256").claim("sub", "merchant");
    if (audience != null) builder.audience(List.of(audience));
    if (flow != null) builder.claim("auth_flow", flow);
    if (acr != null) builder.claim("acr", acr);
    return builder.build();
  }

  @Test
  void rejectsMissingSubject() {
    Jwt token =
        Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .audience(List.of("payments-api"))
            .claim("auth_flow", "password")
            .claim("acr", "4")
            .build();
    assertTrue(new AccessTokenPolicy().validate(token).hasErrors());
  }

  @Test
  void rejectsMissingAudienceWithoutException() {
    assertTrue(new AccessTokenPolicy().validate(jwt(null, "password", "4")).hasErrors());
  }

  @Test
  void rejectsMismatchedContextAndAudience() {
    var policy = new AccessTokenPolicy();
    assertTrue(policy.validate(jwt("other-api", "code", "5")).hasErrors());
    assertTrue(policy.validate(jwt("payments-api", "phone", "5")).hasErrors());
    assertTrue(policy.validate(jwt("payments-api", null, null)).hasErrors());
  }

  @Test
  void acceptsVerifiedContexts() {
    var policy = new AccessTokenPolicy();
    for (String[] pair :
        List.of(
            new String[] {"code", "5"},
            new String[] {"password", "4"},
            new String[] {"phone", "3"}))
      assertFalse(policy.validate(jwt("payments-api", pair[0], pair[1])).hasErrors());
  }
}
