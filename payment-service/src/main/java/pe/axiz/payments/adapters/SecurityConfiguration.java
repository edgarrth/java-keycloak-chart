package pe.axiz.payments.adapters;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {
  @Bean
  JwtDecoder decoder(@Value("${axiz.issuer}") String issuer, @Value("${axiz.jwks}") String jwks) {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(jwks).build();
    OAuth2TokenValidator<Jwt> audience = new AccessTokenPolicy();
    decoder.setJwtValidator(
        new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(issuer), audience));
    return decoder;
  }

  @Bean
  org.springframework.security.oauth2.client.registration.ClientRegistrationRepository clients(
      @Value("${axiz.issuer}") String issuer,
      @Value("${axiz.jwks}") String jwks,
      @Value("${KEYCLOAK_INTERNAL_URL:http://localhost:8080}") String internal) {
    var client =
        org.springframework.security.oauth2.client.registration.ClientRegistration
            .withRegistrationId("axiz")
            .clientId("payments-web")
            .clientAuthenticationMethod(
                org.springframework.security.oauth2.core.ClientAuthenticationMethod.NONE)
            .authorizationGrantType(
                org.springframework.security.oauth2.core.AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile")
            .issuerUri(issuer)
            .authorizationUri(issuer + "/protocol/openid-connect/auth")
            .tokenUri(internal + "/realms/axiz/protocol/openid-connect/token")
            .jwkSetUri(jwks)
            .userInfoUri(internal + "/realms/axiz/protocol/openid-connect/userinfo")
            .userNameAttributeName("preferred_username")
            .clientName("Axiz Payments")
            .build();
    return new org.springframework.security.oauth2.client.registration
        .InMemoryClientRegistrationRepository(client);
  }

  @Bean
  @Order(1)
  SecurityFilterChain api(HttpSecurity http) throws Exception {
    JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(
        jwt -> {
          Map<String, Object> realm = jwt.getClaim("realm_access");
          if (realm == null) return List.of();
          Object roles = realm.get("roles");
          if (!(roles instanceof Collection<?> values)) return List.of();
          return values.stream()
              .<org.springframework.security.core.GrantedAuthority>map(
                  r -> new SimpleGrantedAuthority("ROLE_" + r))
              .toList();
        });
    return http.securityMatcher("/api/**")
        .csrf(c -> c.disable())
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(a -> a.anyRequest().authenticated())
        .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter)))
        .build();
  }

  @Bean
  SecurityFilterChain portal(HttpSecurity http) throws Exception {
    return http.authorizeHttpRequests(
            a ->
                a.requestMatchers("/", "/error", "/ready").permitAll().anyRequest().authenticated())
        .oauth2Login(o -> o.defaultSuccessUrl("/portal", true))
        .build();
  }
}
