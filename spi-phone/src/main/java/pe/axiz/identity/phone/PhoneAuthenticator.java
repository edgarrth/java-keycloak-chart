package pe.axiz.identity.phone;

import jakarta.ws.rs.core.Response;
import org.keycloak.authentication.*;
import org.keycloak.models.*;
import pe.axiz.identity.common.AcrPolicy;

public final class PhoneAuthenticator implements Authenticator {
  public void authenticate(AuthenticationFlowContext c) {
    UserModel user = c.getUser();
    boolean valid =
        user != null
            && user.getFederationLink() != null
            && user.getUsername().matches("\\+51[0-9]{9}");
    if (!valid) {
      c.failure(
          AuthenticationFlowError.INVALID_USER,
          Response.status(400)
              .entity("{\"error\":\"invalid_grant\"}")
              .type("application/json")
              .build());
      return;
    }
    c.getSession().getProvider(AcrPolicy.class).establish(c, "phone");
    c.success();
  }

  public void action(AuthenticationFlowContext c) {
    authenticate(c);
  }

  public boolean requiresUser() {
    return true;
  }

  public boolean configuredFor(KeycloakSession s, RealmModel r, UserModel u) {
    return true;
  }

  public void setRequiredActions(KeycloakSession s, RealmModel r, UserModel u) {}

  public void close() {}
}
