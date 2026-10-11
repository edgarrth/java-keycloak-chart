package pe.axiz.identity.common;

import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.provider.Provider;

public interface AcrPolicy extends Provider {
  void establish(AuthenticationFlowContext context, String flow);
}
