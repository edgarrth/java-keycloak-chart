package pe.axiz.identity.common;

import java.util.List;
import org.keycloak.models.*;
import org.keycloak.protocol.oidc.mappers.*;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.representations.IDToken;

public final class AcrMapper extends AbstractOIDCProtocolMapper
    implements OIDCAccessTokenMapper, OIDCIDTokenMapper {
  public String getId() {
    return "axiz-verified-acr";
  }

  public String getDisplayType() {
    return "ACR verificado Axiz";
  }

  public String getDisplayCategory() {
    return "Token";
  }

  public String getHelpText() {
    return "Se publica el ACR establecido por el flujo de autenticación.";
  }

  public List<ProviderConfigProperty> getConfigProperties() {
    return List.of();
  }

  protected void setClaim(
      IDToken token,
      ProtocolMapperModel model,
      UserSessionModel userSession,
      KeycloakSession session,
      ClientSessionContext clientSession) {
    String acr = userSession.getNote("axiz_acr");
    if (acr == null) throw new IllegalStateException("Sesión sin ACR verificado");
    token.setAcr(acr);
    token.getOtherClaims().put("auth_flow", userSession.getNote("axiz_flow"));
  }
}
