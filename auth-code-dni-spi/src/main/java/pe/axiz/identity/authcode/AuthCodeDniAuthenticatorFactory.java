package pe.axiz.identity.authcode;

import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.*;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import pe.axiz.identity.common.SpiVersion;

public final class AuthCodeDniAuthenticatorFactory
    implements AuthenticatorFactory, ServerInfoAwareProviderFactory {
  public Authenticator create(KeycloakSession s) {
    return new AuthCodeDniAuthenticator();
  }

  public String getId() {
    return "auth-code-dni";
  }

  public String getDisplayType() {
    return "Authorization Code DNI / v"
        + SpiVersion.of(AuthCodeDniAuthenticatorFactory.class)
        + " / ACR 5";
  }

  public String getReferenceCategory() {
    return "axiz";
  }

  public boolean isConfigurable() {
    return false;
  }

  public AuthenticationExecutionModel.Requirement[] getRequirementChoices() {
    return new AuthenticationExecutionModel.Requirement[] {
      AuthenticationExecutionModel.Requirement.REQUIRED
    };
  }

  public boolean isUserSetupAllowed() {
    return false;
  }

  public String getHelpText() {
    return "Se autentica con usuario y contraseña y se establece el ACR únicamente tras comprobar las credenciales.";
  }

  public List<ProviderConfigProperty> getConfigProperties() {
    return List.of();
  }

  public void init(Config.Scope c) {}

  public void postInit(KeycloakSessionFactory f) {}

  public void close() {}

  public java.util.Map<String, String> getOperationalInfo() {
    return java.util.Map.of(
        "version", SpiVersion.of(AuthCodeDniAuthenticatorFactory.class), "keycloak", "26.8.0");
  }
}
