package pe.axiz.identity.external;

import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import pe.axiz.identity.common.CredentialGrantAuthenticator;

public final class ExternalAuthPhoneAuthenticator extends CredentialGrantAuthenticator {
  protected String flow() {
    return "phone";
  }

  protected boolean matchesIdentifier(String identifier) {
    return identifier.matches("\\+51[0-9]{9}");
  }

  protected boolean accepts(RealmModel realm, UserModel user) {
    var component =
        user.getFederationLink() == null ? null : realm.getComponent(user.getFederationLink());
    return component != null && "external-auth-phone-storage".equals(component.getProviderId());
  }
}
