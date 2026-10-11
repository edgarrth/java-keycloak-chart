package pe.axiz.identity.common;

public final class SpiVersion {
  private SpiVersion() {}

  public static String of(Class<?> providerType) {
    String version = providerType.getPackage().getImplementationVersion();
    return version == null ? "desarrollo" : version;
  }
}
