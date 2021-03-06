package com.bank.bootcamp.currentaccounts.controller;

import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bank.bootcamp.currentaccounts.dto.BalanceDTO;
import com.bank.bootcamp.currentaccounts.dto.CreateAccountDTO;
import com.bank.bootcamp.currentaccounts.dto.CreateTransactionDTO;
import com.bank.bootcamp.currentaccounts.dto.TransferDTO;
import com.bank.bootcamp.currentaccounts.entity.Account;
import com.bank.bootcamp.currentaccounts.entity.CustomerType;
import com.bank.bootcamp.currentaccounts.entity.Transaction;
import com.bank.bootcamp.currentaccounts.service.AccountService;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("currentAccounts")
@RequiredArgsConstructor
public class AccountController {

  private final AccountService accountService;
  
  @GetMapping("/balance/{accountId}")
  public Mono<BalanceDTO> getBalanceByAccountId(@PathVariable("accountId") String accountId) {
    return accountService.getBalanceByAccountId(accountId);
  }
  
  @GetMapping("/balance/byCustomer/{customerType}/{customerId}")
  public Flux<BalanceDTO> getBalancesByCustomerId(@PathVariable("customerId") String customerId, @PathVariable("customerType") CustomerType customerType) {
    return accountService.getBalancesByCustomerId(customerId, customerType);
  }
  
  @PostMapping
  public Mono<String> createAccount(@RequestBody CreateAccountDTO dto) throws Exception {
    return accountService.createAccount(dto).map(Account::getId);
  }
  
  @PostMapping("/transaction")
  public Mono<Integer> createTransaction(@RequestBody CreateTransactionDTO dto) {
    return accountService.createTransaction(dto).map(Transaction::getOperationNumber);
  }
  
  @PostMapping("/transfer")
  public Mono<Integer> transfer(@RequestBody TransferDTO dto) {
    return accountService.transfer(dto);
  }
  
  @GetMapping("/byCustomer/{customerType}/{customerId}")
  public Flux<Account> getAccountsByCustomer(@PathVariable("customerId") String customerId, @PathVariable("customerType") CustomerType customerType) {
    return accountService.getAccountsByCustomer(customerId, customerType);
  }
  
  @GetMapping("movements/{accountId}/{year}/{month}")
  public Flux<Transaction> getMovementsByAccountAndPeriod(
      @PathVariable("accountId") String accountId,
      @PathVariable("year") Integer year, @PathVariable("month") Integer month) {
    return accountService.getTransactionsByAccountIdAndPeriod(accountId, LocalDate.of(year, month, 1));
  }
  
}