package com.bank.bootcamp.currentaccounts.webclient;

import org.springframework.cloud.circuitbreaker.resilience4j.ReactiveResilience4JCircuitBreakerFactory;
import org.springframework.cloud.client.circuitbreaker.ReactiveCircuitBreaker;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.reactive.function.client.WebClient;
import com.bank.bootcamp.currentaccounts.dto.BalanceDTO;
import com.bank.bootcamp.currentaccounts.entity.CustomerType;
import com.bank.bootcamp.currentaccounts.exception.BankValidationException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CreditWebClient {

  private final ReactiveCircuitBreaker reactiveCircuitBreaker;
  private WebClient webClient;
  
  
  public CreditWebClient(ReactiveResilience4JCircuitBreakerFactory reactiveCircuitBreakerFactory, Environment env) {
    this.reactiveCircuitBreaker = reactiveCircuitBreakerFactory.create("products");
    webClient = WebClient.create(env.getProperty("gateway.url"));
  }

  public Flux<BalanceDTO> getAllBalances(String customerId) {
    if (ObjectUtils.isEmpty(customerId)) {
      return Flux.error(new BankValidationException("Customer ID is required"));
    } else {
      
      var credits = webClient.get()
          .uri("/credits/balanceByCustomer/{customerId}/{creditType}", customerId, "PERSONAL")
          .retrieve()
          .bodyToFlux(BalanceDTO.class)
          .transform(balance -> reactiveCircuitBreaker.run(balance, throwable -> Flux.empty()));
      
      return Flux.merge(credits)
      .parallel()
      .sequential();
    }
  }
  
  public Mono<Boolean> hasOverdueDebt(String customerId, CustomerType customerType) {
      return webClient.get()
        .uri("/credits/hasDebt/{customerId}/{creditType}", customerId, customerType)
        .retrieve()
        .bodyToMono(Boolean.class)
        .transform(balance -> reactiveCircuitBreaker.run(balance, throwable -> Mono.error(new BankValidationException("Credit service not respond"))));
  }
  
}
