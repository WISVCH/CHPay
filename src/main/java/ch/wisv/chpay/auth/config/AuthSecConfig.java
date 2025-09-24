package ch.wisv.chpay.auth.config;

import ch.wisv.chpay.auth.component.ApiKeyFilter;
import ch.wisv.chpay.auth.component.CustomAccessDeniedHandler;
import ch.wisv.chpay.auth.component.OAuth2FailureHandler;
import ch.wisv.chpay.auth.service.CustomOIDCUserService;
import ch.wisv.chpay.core.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.web.SecurityFilterChain;
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

  @Bean
  public SessionRegistry sessionRegistry() {
    return new SessionRegistryImpl();
  }

  @Value("${spring.profiles.active:}")
  private String activeProfiles;

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

    // Add API key filter before OAuth2 login
    http.addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);

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
   * <p>This method specifies which requests should be permitted or require authentication. Most
   * authorization is now handled at the controller level using @PreAuthorize annotations.
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
                      "/topup/status",
                      "/actuator/health/liveness",
                      "/actuator/health/readiness",
                      "/actuator/health")
                  .permitAll();

              // Only permit test endpoints when test profile is active
              if (activeProfiles.contains("test")) {
                authz.requestMatchers("/test/**").permitAll();
              }

              // All other requests require authentication - authorization is handled at controller
              // level
              authz.anyRequest().authenticated();
            })
        .csrf(
            csrf ->
                csrf.ignoringRequestMatchers(
                    "/topup/status", "/transactions/email-receipt/**", "/api/**"))
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
