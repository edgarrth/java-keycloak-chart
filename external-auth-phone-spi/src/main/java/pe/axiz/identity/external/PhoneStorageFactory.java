package pe.axiz.identity.external;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.provider.ServerInfoAwareProviderFactory;
import org.keycloak.storage.UserStorageProviderFactory;
import pe.axiz.identity.common.SpiVersion;

public final class PhoneStorageFactory
    implements UserStorageProviderFactory<PhoneStorage>, ServerInfoAwareProviderFactory {
  public PhoneStorage create(KeycloakSession s, ComponentModel m) {
    return new PhoneStorage(s, m);
  }

  public String getId() {
    return "external-auth-phone-storage";
  }

  public String getHelpText() {
    return "Se consultan identidades telefónicas de PostgreSQL sin importar credenciales.";
  }

  public java.util.Map<String, String> getOperationalInfo() {
    return java.util.Map.of(
        "version", SpiVersion.of(PhoneStorageFactory.class), "keycloak", "26.8.0");
  }
}
