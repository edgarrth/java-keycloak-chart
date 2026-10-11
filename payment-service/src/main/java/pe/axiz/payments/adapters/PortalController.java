package pe.axiz.payments.adapters;

import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
public class PortalController {
  @GetMapping(value = "/", produces = "text/html")
  public String home() {
    return "<html lang='es'><title>Axiz Payments</title><h1>Identidad para pagos</h1><p>Se valida el DNI mediante Authorization Code y PKCE.</p><a href='/oauth2/authorization/axiz'>Iniciar sesión</a></html>";
  }

  @GetMapping("/portal")
  public Map<String, Object> portal(@AuthenticationPrincipal OidcUser user) {
    return Map.of(
        "username",
        user.getPreferredUsername(),
        "acr",
        user.getClaimAsString("acr"),
        "flow",
        user.getClaimAsString("auth_flow"));
  }

  @GetMapping("/ready")
  public Map<String, String> ready() {
    return Map.of("status", "UP");
  }
}
