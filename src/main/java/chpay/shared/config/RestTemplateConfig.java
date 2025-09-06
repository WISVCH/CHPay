package chpay.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for providing RestTemplate beans to the application.
 * This configuration ensures that RestTemplate instances are available for
 * dependency injection throughout the application.
 */
@Configuration
public class RestTemplateConfig {

  /**
   * Creates and configures a RestTemplate bean for HTTP client operations.
   * This bean can be injected into services that need to make HTTP requests.
   *
   * @return a configured RestTemplate instance
   */
  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
  }
}
