package com.bank.bootcamp.currentaccounts.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import com.bank.bootcamp.currentaccounts.entity.Account;
import com.bank.bootcamp.currentaccounts.entity.CustomerType;
import reactor.core.publisher.Flux;

public interface AccountRepository extends ReactiveMongoRepository<Account, String> {

  Flux<Account> findByCustomerIdAndCustomerType(String customerId, CustomerType customerType);

}
