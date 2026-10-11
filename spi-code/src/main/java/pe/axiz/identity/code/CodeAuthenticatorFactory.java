package pe.axiz.identity.code;

import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.*;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

public final class CodeAuthenticatorFactory implements AuthenticatorFactory {
  public Authenticator create(KeycloakSession s) {
    return new CodeAuthenticator();
  }

  public String getId() {
    return "axiz-code";
  }

  public String getDisplayType() {
    return "Axiz code ACR 5";
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
    return "Se valida el origen del usuario y se establece el ACR.";
  }

  public List<ProviderConfigProperty> getConfigProperties() {
    return List.of();
  }

  public void init(Config.Scope c) {}

  public void postInit(KeycloakSessionFactory f) {}

  public void close() {}
}
