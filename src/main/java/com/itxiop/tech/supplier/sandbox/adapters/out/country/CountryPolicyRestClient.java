package com.itxiop.tech.supplier.sandbox.adapters.out.country;

import com.itxiop.tech.supplier.sandbox.domain.policy.CountryPolicy;
import com.itxiop.tech.supplier.sandbox.domain.value.CountryCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@Lazy
public class CountryPolicyRestClient implements CountryPolicy {

  private final RestClient restClient;

  public CountryPolicyRestClient(
      RestClient.Builder builder,
      @Value("${itx.country.base-url:http://localhost:8080}") String baseUrl
  ) {
    this.restClient = builder.baseUrl(baseUrl).build();
  }

  @Override
  public boolean isApproved(CountryCode country) {
    try {
      CountryDto dto = restClient.get()
          .uri("/countries/{country}", country.value())
          .retrieve()
          .body(CountryDto.class);

      return dto != null && !dto.isBanned();
    } catch (RestClientResponseException ex) {
      if (ex.getStatusCode() == HttpStatus.NOT_FOUND) return false;
      return false;
    }
  }

  public record CountryDto(String name, boolean isBanned) {}
}