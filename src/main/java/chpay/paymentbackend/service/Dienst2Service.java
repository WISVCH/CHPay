package chpay.paymentbackend.service;

import chpay.paymentbackend.entities.Dienst2ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * The Dienst2Service class provides functionality to interact with the Dienst2 API. It allows
 * fetching user data based on specific attributes such as netID or Google username. The API
 * requests include proper authorization using a token-based method.
 */
@Service
public class Dienst2Service {

  private final RestTemplate restTemplate;
  private final String apiBaseUrl;
  private final String apiKey;

  /**
   * Constructs a new Dienst2Service instance.
   *
   * @param restTemplate the RestTemplate used for making HTTP requests
   * @param apiBaseUrl the base URL of the Dienst2 API (inserted from application.yml)
   * @param apiKey the API token used for authentication (inserted from application.yml)
   */
  public Dienst2Service(
      RestTemplate restTemplate,
      @Value("${api.dienst2.base-url}") String apiBaseUrl,
      @Value("${api.dienst2.token}") String apiKey) {
    this.restTemplate = restTemplate;
    this.apiBaseUrl = apiBaseUrl;
    this.apiKey = apiKey;
  }

  /**
   * Fetches user data from the Dienst2 API using the provided netID. The method constructs an HTTP
   * GET request with appropriate headers for authorization and retrieves user information in the
   * form of a Dienst2ApiResponse object.
   *
   * @param netID the unique network ID of the user whose data is to be fetched
   * @return a Dienst2ApiResponse containing user data, including attributes such as first name,
   *     surname, email, and netID
   */
  public Dienst2ApiResponse fetchUserDataNetID(String netID) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token " + apiKey);

    HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

    String url = apiBaseUrl + "/ldb/api/v3/people/?netid=" + netID;

    return restTemplate
        .exchange(url, HttpMethod.GET, requestEntity, Dienst2ApiResponse.class)
        .getBody();
  }

  /**
   * Fetches user data from the Dienst2 API using the provided Google username. The method
   * constructs an HTTP GET request with appropriate headers for authorization and retrieves user
   * information in the form of a Dienst2ApiResponse object.
   *
   * @param googleUsername the Google username of the user whose data is to be fetched
   * @return a Dienst2ApiResponse containing user data, including attributes such as first name,
   *     surname, email, and netID
   */
  public Dienst2ApiResponse fetchUserDataGoogleUsername(String googleUsername) {
    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Token " + apiKey);

    HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
    String url = apiBaseUrl + "/ldb/api/v3/people/?google_username=" + googleUsername;

    return restTemplate
        .exchange(url, HttpMethod.GET, requestEntity, Dienst2ApiResponse.class)
        .getBody();
  }
}
