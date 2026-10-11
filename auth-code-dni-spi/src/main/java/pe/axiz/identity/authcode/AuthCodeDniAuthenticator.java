package pe.axiz.identity.authcode;

import jakarta.ws.rs.core.MultivaluedMap;
import org.keycloak.authentication.AuthenticationFlowContext;
import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordForm;
import org.keycloak.authentication.authenticators.util.AuthenticatorUtils;
import org.keycloak.events.Errors;
import pe.axiz.identity.common.AcrPolicy;

public final class AuthCodeDniAuthenticator extends UsernamePasswordForm {
  @Override
  protected boolean validateForm(
      AuthenticationFlowContext context, MultivaluedMap<String, String> data) {
    String dni = data.getFirst("username");
    var user =
        dni == null
            ? null
            : context.getSession().users().getUserByUsername(context.getRealm(), dni);
    if (dni == null
        || !dni.matches("[0-9]{8}")
        || user == null
        || user.getFederationLink() != null) {
      AuthenticatorUtils.dummyHash(context);
      context.getEvent().error(Errors.INVALID_USER_CREDENTIALS);
      context.failureChallenge(
          AuthenticationFlowError.INVALID_USER,
          challenge(context, "invalidUserMessage", "username"));
      return false;
    }
    if (!super.validateForm(context, data)) return false;
    context.getSession().getProvider(AcrPolicy.class).establish(context, "code");
    return true;
  }
}
