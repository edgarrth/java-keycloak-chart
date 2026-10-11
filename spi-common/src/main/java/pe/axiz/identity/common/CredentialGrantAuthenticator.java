package pe.axiz.identity.common;

import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.authenticators.browser.AbstractUsernameFormAuthenticator;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.services.managers.AuthenticationManager;

public abstract class CredentialGrantAuthenticator implements Authenticator {
  protected abstract String flow();

  protected abstract boolean accepts(RealmModel realm, UserModel user);

  protected abstract boolean matchesIdentifier(String identifier);

  public final void authenticate(AuthenticationFlowContext context) {
    var form = context.getHttpRequest().getDecodedFormParameters();
    String username = form.getFirst("username");
    String password = form.getFirst("password");
    if (username == null
        || password == null
        || username.length() > 32
        || !matchesIdentifier(username)) {
      AuthenticatorUtils.dummyHash(context);
      fail(context, Errors.INVALID_USER_CREDENTIALS);
      return;
    }
    context.getEvent().detail(Details.USERNAME, username);
    context
        .getAuthenticationSession()
        .setAuthNote(AbstractUsernameFormAuthenticator.ATTEMPTED_USERNAME, username);
    UserModel user = context.getSession().users().getUserByUsername(context.getRealm(), username);
    if (user == null
        || user.getServiceAccountClientLink() != null
        || !accepts(context.getRealm(), user)) {
      AuthenticatorUtils.dummyHash(context);
      fail(context, Errors.INVALID_USER_CREDENTIALS);
      return;
    }
    context.getEvent().user(user);
    String blocked = AuthenticatorUtils.getDisabledByBruteForceEventError(context, user);
    if (!user.isEnabled() || blocked != null) {
      AuthenticatorUtils.dummyHash(context);
      context.getEvent().error(blocked == null ? Errors.USER_DISABLED : blocked);
      context.forceChallenge(error());
      return;
    }
    context.setUser(user);
    if (!user.credentialManager().isValid(UserCredentialModel.password(password))) {
      fail(context, Errors.INVALID_USER_CREDENTIALS);
      return;
    }
    context
        .getAuthenticationSession()
        .setAuthNote(AuthenticationManager.PASSWORD_VALIDATED, "true");
    context.getSession().getProvider(AcrPolicy.class).establish(context, flow());
    context.success(PasswordCredentialModel.TYPE);
  }

  private static Response error() {
    return Response.status(400)
        .type("application/json")
        .entity(Map.of("error", "invalid_grant", "error_description", "Credenciales inválidas"))
        .build();
  }

  private static void fail(AuthenticationFlowContext context, String eventError) {
    context.getEvent().error(eventError);
    context.failure(AuthenticationFlowError.INVALID_USER, error());
  }

  public final void action(AuthenticationFlowContext context) {
    authenticate(context);
  }

  public final boolean requiresUser() {
    return false;
  }

  public final boolean configuredFor(KeycloakSession session, RealmModel realm, UserModel user) {
    return true;
  }

  public final void setRequiredActions(KeycloakSession session, RealmModel realm, UserModel user) {}

  public final void close() {}
}
