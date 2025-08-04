package chpay.paymentbackend.Auth;

import chpay.shared.service.NotificationService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

/**
 * AuthSecConfig is a Spring configuration class that sets up the security configuration for the
 * application. It customizes behaviors such as request authorization, OAuth2 login, and logout
 * functionality.
 *
 * <p>The class integrates a custom OIDC (OpenID Connect) user service to handle user details during
 * the authentication process.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class AuthSecConfig {

  @Autowired private CustomOIDCUserService customOidcUserService;

  @Autowired private CustomAccessDeniedHandler customAccessDeniedHandler;

  @Autowired private OAuth2FailureHandler OAuth2FailureHandler;

  @Autowired private NotificationService notificationService;

  @Autowired private ApiKeyFilter apiKeyAuthFilter;

  @Autowired private LoadUserFromJwtFilter loadUserFromJwtFilter;

  @Bean
  public SessionRegistry sessionRegistry() {
    return new SessionRegistryImpl();
  }

  @Value("${spring.profiles.active:}")
  private String activeProfiles;

  /**
   * Configures the security filter chain for API requests. This method customizes the HTTP security
   * settings for endpoint authorization and authentication behaviors specific to API routes. It
   * applies various filters, such as JWT authentication and API key validation, to secure the API
   * endpoints.
   *
   * <p>Authorization is granted to specific endpoints (e.g., allowing all requests to
   * "/api/events/status"), while all other API requests are authenticated using OAuth2 resource
   * server with JWT.
   *
   * @param http an instance of {@link HttpSecurity} used to configure HTTP security for the
   *     application
   * @return the configured {@link SecurityFilterChain} for API endpoints
   * @throws Exception if an error occurs while configuring the security filter chain
   */
  @Bean
  @Order(1) // Higher priority
  public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/**")
        .authorizeHttpRequests(
            authz ->
                authz
                    .requestMatchers("/api/events/status")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            oauth2 ->
                oauth2
                    .authenticationEntryPoint(
                        (request, response, authException) -> {
                          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                          response
                              .getWriter()
                              .write("JWT auth failed: " + authException.getMessage());
                        })
                    .jwt(Customizer.withDefaults()))
        .addFilterAfter(loadUserFromJwtFilter, BearerTokenAuthenticationFilter.class)
        .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public JwtDecoder jwtDecoder() {
    return JwtDecoders.fromIssuerLocation("https://connect.ch.tudelft.nl");
  }

  /**
   * Configures and builds the security filter chain used to secure HTTP requests. This method sets
   * up request authorization, authentication, and logout behavior.
   *
   * @param http the {@code HttpSecurity} object used to configure HTTP security
   * @return the configured {@code SecurityFilterChain} instance
   * @throws Exception if there is an error during configuration
   */
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    configureRequestAuthorization(http);

    // Create and configure the success handler
    SavedRequestAwareAuthenticationSuccessHandler successHandler =
        new SavedRequestAwareAuthenticationSuccessHandler();
    successHandler.setDefaultTargetUrl("/index");
    http.oauth2Login(
            oauth2 ->
                oauth2
                    .loginPage("/login")
                    .successHandler(successHandler)
                    .failureHandler(OAuth2FailureHandler)
                    .userInfoEndpoint(
                        userInfo -> {
                          userInfo.oidcUserService(customOidcUserService);
                        }))
        .sessionManagement(
            s -> s.maximumSessions(1).sessionRegistry(sessionRegistry()).expiredUrl("/expired"))
        .headers(
            headers ->
                headers.httpStrictTransportSecurity(
                    hsts -> hsts.includeSubDomains(true).preload(true).maxAgeInSeconds(31536000)));

    configureLogout(http);
    return http.build();
  }

  /**
   * Configures authorization rules for incoming HTTP requests.
   *
   * <p>This method specifies which requests should be permitted or require authentication. while
   * all other requests must be authenticated.
   *
   * @param http an instance of {@link HttpSecurity} used to customize authorization rules
   * @throws Exception if an error occurs while configuring authorization
   */
  private void configureRequestAuthorization(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            authz -> {
              // Base permitted paths
              authz
                  .requestMatchers(
                      "/",
                      "/expired",
                      "/login",
                      "/error",
                      "/logout-success",
                      "/css/**",
                      "/js/**",
                      "/images/**",
                      "/balance/status")
                  .permitAll();

              // Only permit test endpoints when test profile is active
              if (activeProfiles.contains("test")) {
                authz.requestMatchers("/test/**").permitAll();
              }
              authz
                  .requestMatchers("/transactions/**", "/index")
                  .hasAnyRole("ADMIN", "USER", "BANNED", "COMMITTEE");
              authz.requestMatchers("/admin/**").hasRole("ADMIN");
              authz
                  .requestMatchers("/**")
                  .access(
                      new WebExpressionAuthorizationManager(
                          "hasAnyRole('USER', 'ADMIN', 'COMMITTEE') and !hasRole('BANNED')"));
              authz.anyRequest().authenticated();
            })
        .csrf(
            csrf ->
                csrf.ignoringRequestMatchers("/balance/status", "/transactions/email-receipt/**"))
        .exceptionHandling(
            exceptions -> exceptions.accessDeniedHandler(customAccessDeniedHandler)
            /*
             * This code has tested my resolve like nothing has since that time I was stabbed. When
             * Spring Security's filter chain throws an exception, even if it does so because you
             * coded it that way, you are NOT able to intercept this exception with the exception
             * handler (with the annotations SPRING provides) because the exception is thrown BEFORE
             * the request reaches Spring MVC's DispatcherServlet. As a result,
             * the @RestControllerAdvice exception handler never sees this exception. So we had to
             * implement it in a custom handler.
             */
            );
  }

  /**
   * Configures the logout functionality for the application.
   *
   * @param http the {@link HttpSecurity} to modify the logout behavior
   * @throws Exception if an error occurs while configuring the logout settings
   */
  private void configureLogout(HttpSecurity http) throws Exception {
    http.logout(
        logout ->
            logout
                .logoutUrl("/logout")
                .clearAuthentication(true)
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "POST"))
                .logoutSuccessHandler(
                    (request, response, authentication) -> {
                      // Generate timestamp token for validation
                      System.out.println("Logout handler triggered");
                      long timestamp = System.currentTimeMillis();
                      System.out.println("Redirecting to: /logout-success?ts=" + timestamp);
                      response.sendRedirect("/logout-success?ts=" + timestamp);
                    }));
  }
}
