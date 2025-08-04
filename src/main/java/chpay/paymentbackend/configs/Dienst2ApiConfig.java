package chpay.paymentbackend.configs;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for setting up the Dienst2Api-related beans and components. Provides a
 * customized {@link RestTemplate} for handling RESTful communication.
 */
@Configuration
public class Dienst2ApiConfig {

  /**
   * Creates and configures a {@link RestTemplate} bean for handling RESTful communication. The
   * configured {@link RestTemplate} includes custom error handling and message converters.
   *
   * @return a configured instance of {@link RestTemplate}.
   */
  @Bean
  public RestTemplate restTemplate() {
    RestTemplate restTemplate = new RestTemplate();

    // Add error handling if needed
    restTemplate.setErrorHandler(
        new ResponseErrorHandler() {
          @Override
          public boolean hasError(ClientHttpResponse response) throws IOException {
            return response.getStatusCode().is4xxClientError()
                || response.getStatusCode().is5xxServerError();
          }

          @Override
          public void handleError(URI url, HttpMethod method, ClientHttpResponse response)
              throws IOException {
            throw new RestClientException(
                "Could not connect to CH user database: " + response.getStatusCode());
          }
        });

    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setSupportedMediaTypes(
        Arrays.asList(MediaType.APPLICATION_JSON, MediaType.APPLICATION_OCTET_STREAM));
    restTemplate.getMessageConverters().add(converter);

    return restTemplate;
  }
}
