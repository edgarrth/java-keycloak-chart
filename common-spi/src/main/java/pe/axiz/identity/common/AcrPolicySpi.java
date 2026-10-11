package pe.axiz.identity.common;

import org.keycloak.provider.*;

public final class AcrPolicySpi implements Spi {
  public boolean isInternal() {
    return false;
  }

  public String getName() {
    return "axiz-acr-policy";
  }

  public Class<? extends Provider> getProviderClass() {
    return AcrPolicy.class;
  }

  public Class<? extends ProviderFactory> getProviderFactoryClass() {
    return AcrPolicyProviderFactory.class;
  }
}
