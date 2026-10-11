package pe.axiz.identity.common;

import org.keycloak.Config;
import org.keycloak.models.*;
import org.keycloak.provider.ServerInfoAwareProviderFactory;

public final class AcrPolicyFactory
    implements AcrPolicyProviderFactory, ServerInfoAwareProviderFactory {
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

  public java.util.Map<String, String> getOperationalInfo() {
    return java.util.Map.of("version", SpiVersion.of(AcrPolicyFactory.class), "keycloak", "26.8.0");
  }
}
