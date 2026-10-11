package pe.axiz.identity.ropc;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import pe.axiz.identity.common.CredentialGrantAuthenticator;

public final class RopcDniAuthenticator extends CredentialGrantAuthenticator {
  protected String flow() {
    return "password";
  }

  protected boolean matchesIdentifier(String identifier) {
    return identifier.matches("[0-9]{8}");
  }

  protected boolean accepts(RealmModel realm, UserModel user) {
    return user.getFederationLink() == null;
  }
}
