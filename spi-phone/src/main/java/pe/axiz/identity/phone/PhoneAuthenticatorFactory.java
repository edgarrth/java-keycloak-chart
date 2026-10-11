package pe.axiz.identity.phone;

import java.util.List;
import org.keycloak.Config;
import org.keycloak.authentication.*;
import org.keycloak.models.*;
import org.keycloak.provider.ProviderConfigProperty;

public final class PhoneAuthenticatorFactory implements AuthenticatorFactory {
  public Authenticator create(KeycloakSession s) {
    return new PhoneAuthenticator();
  }

  public String getId() {
    return "axiz-phone";
  }

  public String getDisplayType() {
    return "Axiz phone ACR 3";
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
