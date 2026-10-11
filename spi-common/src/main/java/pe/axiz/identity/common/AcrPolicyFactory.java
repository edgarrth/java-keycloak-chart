package pe.axiz.identity.common;

import org.keycloak.Config;
import org.keycloak.models.*;

public final class AcrPolicyFactory implements AcrPolicyProviderFactory {
  public AcrPolicy create(KeycloakSession session) {
    return new AcrPolicy() {
      public void establish(org.keycloak.authentication.AuthenticationFlowContext c, String flow) {
        String acr =
            switch (flow) {
              case "code" -> "5";
              case "password" -> "4";
              case "phone" -> "3";
              default -> throw new IllegalArgumentException("Flujo no reconocido");
            };
        c.getAuthenticationSession().setUserSessionNote("axiz_acr", acr);
        c.getAuthenticationSession().setUserSessionNote("axiz_flow", flow);
      }

      public void close() {}
    };
  }

  public String getId() {
    return "default";
  }

  public void init(Config.Scope c) {}

  public void postInit(KeycloakSessionFactory f) {}

  public void close() {}
}
