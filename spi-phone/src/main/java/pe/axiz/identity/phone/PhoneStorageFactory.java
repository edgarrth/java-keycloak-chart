package pe.axiz.identity.phone;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.*;
import org.keycloak.storage.UserStorageProviderFactory;

public final class PhoneStorageFactory implements UserStorageProviderFactory<PhoneStorage> {
  public PhoneStorage create(KeycloakSession s, ComponentModel m) {
    return new PhoneStorage(s, m);
  }

  public String getId() {
    return "axiz-phone-storage";
  }

  public String getHelpText() {
    return "Se consultan identidades telefónicas de PostgreSQL sin importar credenciales.";
  }
}
