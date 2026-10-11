package pe.axiz.identity.phone;

import java.security.MessageDigest;
import java.sql.*;
import java.util.*;
import java.util.stream.Stream;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.*;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.*;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;

public final class PhoneStorage
    implements UserStorageProvider,
        UserLookupProvider,
        CredentialInputValidator,
        CredentialInputUpdater {
  private final KeycloakSession session;
  private final ComponentModel model;

  public PhoneStorage(KeycloakSession session, ComponentModel model) {
    this.session = session;
    this.model = model;
  }

  private Connection connection() throws SQLException {
    Properties properties = new Properties();
    properties.setProperty("user", System.getenv("PHONE_DB_USER"));
    properties.setProperty("password", System.getenv("PHONE_DB_PASSWORD"));
    properties.setProperty("connectTimeout", "5");
    properties.setProperty("socketTimeout", "10");
    Connection connection =
        new org.postgresql.Driver().connect(System.getenv("PHONE_DB_URL"), properties);
    if (connection == null) throw new SQLException("La URL de identidad debe utilizar PostgreSQL");
    connection.setReadOnly(true);
    return connection;
  }

  public UserModel getUserByUsername(RealmModel realm, String name) {
    if (name == null || !name.matches("\\+51[0-9]{9}")) return null;
    try (Connection c = connection();
        PreparedStatement p =
            c.prepareStatement(
                "select enabled,first_name,last_name,email from phone_identity where phone=?")) {
      p.setString(1, name);
      try (ResultSet r = p.executeQuery()) {
        if (!r.next()) return null;
        boolean enabled = r.getBoolean(1);
        String firstName = r.getString(2);
        String lastName = r.getString(3);
        String email = r.getString(4);
        return new AbstractUserAdapterFederatedStorage(session, realm, model) {
          public String getUsername() {
            return name;
          }

          public void setUsername(String value) {
            throw new org.keycloak.storage.ReadOnlyException(
                "La identidad externa es de solo lectura");
          }

          public String getFirstName() {
            return firstName;
          }

          public String getLastName() {
            return lastName;
          }

          public String getEmail() {
            return email;
          }

          public boolean isEmailVerified() {
            return false;
          }

          public Map<String, List<String>> getAttributes() {
            Map<String, List<String>> attributes = new HashMap<>(super.getAttributes());
            attributes.put(UserModel.FIRST_NAME, List.of(firstName));
            attributes.put(UserModel.LAST_NAME, List.of(lastName));
            attributes.put(UserModel.EMAIL, List.of(email));
            attributes.put(UserModel.USERNAME, List.of(name));
            return attributes;
          }

          public Stream<String> getAttributeStream(String attribute) {
            if (UserModel.FIRST_NAME.equals(attribute)) return Stream.of(firstName);
            if (UserModel.LAST_NAME.equals(attribute)) return Stream.of(lastName);
            if (UserModel.EMAIL.equals(attribute)) return Stream.of(email);
            return super.getAttributeStream(attribute);
          }

          public boolean isEnabled() {
            return enabled;
          }

          public Stream<RoleModel> getRoleMappingsStream() {
            return Stream.of(realm.getRole("merchant"));
          }
        };
      }
    } catch (SQLException e) {
      throw new IllegalStateException("No se pudo consultar la identidad externa", e);
    }
  }

  public UserModel getUserById(RealmModel realm, String id) {
    return getUserByUsername(realm, StorageId.externalId(id));
  }

  public UserModel getUserByEmail(RealmModel realm, String email) {
    return null;
  }

  public boolean supportsCredentialType(String type) {
    return PasswordCredentialModel.TYPE.equals(type);
  }

  public boolean isConfiguredFor(RealmModel r, UserModel u, String type) {
    return supportsCredentialType(type);
  }

  public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
    if (!supportsCredentialType(input.getType()) || input.getChallengeResponse() == null)
      return false;
    try (Connection c = connection();
        PreparedStatement p =
            c.prepareStatement(
                "select salt,password_hash,iterations from phone_identity where phone=? and enabled=true")) {
      p.setString(1, user.getUsername());
      try (ResultSet r = p.executeQuery()) {
        if (!r.next()) return false;
        PBEKeySpec spec =
            new PBEKeySpec(
                input.getChallengeResponse().toCharArray(), r.getBytes(1), r.getInt(3), 256);
        try {
          return MessageDigest.isEqual(
              r.getBytes(2),
              SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                  .generateSecret(spec)
                  .getEncoded());
        } finally {
          spec.clearPassword();
        }
      }
    } catch (Exception e) {
      throw new IllegalStateException("No se pudo validar la credencial externa", e);
    }
  }

  public boolean updateCredential(RealmModel realm, UserModel user, CredentialInput input) {
    if (supportsCredentialType(input.getType()))
      throw new ReadOnlyException("La clave se administra en la base externa");
    return false;
  }

  public void disableCredentialType(RealmModel realm, UserModel user, String type) {
    if (supportsCredentialType(type))
      throw new ReadOnlyException("La clave se administra en la base externa");
  }

  public Stream<String> getDisableableCredentialTypesStream(RealmModel realm, UserModel user) {
    return Stream.empty();
  }

  public void close() {}
}
