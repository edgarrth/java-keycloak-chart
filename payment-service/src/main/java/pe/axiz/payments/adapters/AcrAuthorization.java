package pe.axiz.payments.adapters;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

@Component("acr")
public class AcrAuthorization {
  public boolean allowed(Authentication authentication, int minimum) {
    if (!(authentication instanceof JwtAuthenticationToken token)) return false;
    String value = token.getToken().getClaimAsString("acr");
    boolean merchant =
        token.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_merchant"));
    try {
      return merchant && Integer.parseInt(value) >= minimum && Integer.parseInt(value) <= 5;
    } catch (RuntimeException e) {
      return false;
    }
  }
}
