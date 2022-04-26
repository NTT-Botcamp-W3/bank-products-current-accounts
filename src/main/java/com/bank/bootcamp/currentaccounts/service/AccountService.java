package com.bank.bootcamp.currentaccounts.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Optional;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import com.bank.bootcamp.currentaccounts.dto.BalanceDTO;
import com.bank.bootcamp.currentaccounts.dto.CreateTransactionDTO;
import com.bank.bootcamp.currentaccounts.entity.Account;
import com.bank.bootcamp.currentaccounts.entity.CustomerType;
import com.bank.bootcamp.currentaccounts.entity.Transaction;
import com.bank.bootcamp.currentaccounts.entity.TransactionSequences;
import com.bank.bootcamp.currentaccounts.exception.BankValidationException;
import com.bank.bootcamp.currentaccounts.repository.AccountRepository;
import com.bank.bootcamp.currentaccounts.repository.TransactionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class AccountService {
  
  private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
  
  private final AccountRepository accountRepository;
  private final TransactionRepository transactionRepository;
  private final NextSequenceService nextSequenceService;
  
  private ObjectMapper objectMapper = new ObjectMapper();

  public Mono<Account> createAccount(Account account) {
    return Mono.just(account)
        .then(check(account, acc -> Optional.of(acc).isEmpty(), "Account has not data"))
        .then(check(account, acc -> ObjectUtils.isEmpty(acc.getCustomerType()), "Customer Type is required"))
        .then(check(account, acc -> ObjectUtils.isEmpty(acc.getCustomerId()), "Customer ID is required"))
        .then(check(account, acc -> ObjectUtils.isEmpty(acc.getMaintenanceFee()), "Maintenance fee is required"))
        .then(check(account, acc -> acc.getMaintenanceFee() <= 0, "Maintenance fee must be greater than or equal to zero"))
        .then(accountRepository.findByCustomerIdAndCustomerType(account.getCustomerId(), account.getCustomerType()).count()
            .<Long>handle((record, sink) -> {
              if (record > 0 && account.getCustomerType() == CustomerType.PERSONAL) {
                sink.error(new BankValidationException("Customer already has a current account"));
              } else {
                sink.next(record);
              }
            })
        )
        .flatMap(acc -> {
            return accountRepository.save(account);
         });
  }
  
  private <T> Mono<Void> check(T customer, Predicate<T> predicate, String messageForException) {
    return Mono.create(sink -> {
      if (predicate.test(customer)) {
        sink.error(new BankValidationException(messageForException));
        return;
      } else {
        sink.success();
      }
    });
  }

  public Mono<Transaction> createTransaction(CreateTransactionDTO createTransactionDTO) {
    return Mono.just(createTransactionDTO)
        .then(check(createTransactionDTO, dto -> Optional.of(dto).isEmpty(), "No data for create transaction"))
        .then(check(createTransactionDTO, dto -> ObjectUtils.isEmpty(dto.getAccountId()), "Account ID is required"))
        .then(check(createTransactionDTO, dto -> ObjectUtils.isEmpty(dto.getAgent()), "Agent is required"))
        .then(check(createTransactionDTO, dto -> ObjectUtils.isEmpty(dto.getAmount()), "Amount is required"))
        .then(check(createTransactionDTO, dto -> ObjectUtils.isEmpty(dto.getDescription()), "Description is required"))
        .then(accountRepository.findById(createTransactionDTO.getAccountId()).switchIfEmpty(Mono.error(new BankValidationException("Account not found"))))
        .flatMap(acc -> {
          return transactionRepository.getBalanceByAccountId(createTransactionDTO.getAccountId()).switchIfEmpty(Mono.just(0d));
        })
        .flatMap(balance -> {
          if (balance + createTransactionDTO.getAmount() < 0)
            return Mono.error(new BankValidationException("Insuficient balance"));
          else {
            return nextSequenceService.getNextSequence(TransactionSequences.class.getSimpleName()).<Transaction>flatMap(nextSeq -> {
              try {
                var transaction = objectMapper.readValue(objectMapper.writeValueAsString(createTransactionDTO), Transaction.class);
                transaction.setOperationNumber(nextSeq);
                transaction.setRegisterDate(LocalDateTime.now());
                return transactionRepository.save(transaction);
              } catch (Exception ex) {
                logger.error("Error en mapper", ex);
                return Mono.error(ex);
              }
            });
            
          }
        });
  }

  public Mono<BalanceDTO> getBalanceByAccountId(String accountId) {
    return Mono.just(accountId)
    .switchIfEmpty(Mono.error(new BankValidationException("Account Id is required")))
    .flatMap(accId -> accountRepository.findById(accId))
    .switchIfEmpty(Mono.error(new BankValidationException("Account not found")))
    .flatMap(account -> {
      var x = transactionRepository.getBalanceByAccountId(account.getId()).switchIfEmpty(Mono.just(0d))
          .flatMap(balance -> {
            var yearMonth = YearMonth.from(LocalDateTime.now());
            var currentMonthStart = yearMonth.atDay(1).atStartOfDay();
            var currentMonthEnd = yearMonth.atEndOfMonth().atTime(23, 59, 59);
            
            return transactionRepository.findByAccountIdAndRegisterDateBetween(accountId, currentMonthStart, currentMonthEnd)
                .count().switchIfEmpty(Mono.just(0L))
                .map(qty -> {
                  var balanceDTO = new BalanceDTO();
                  balanceDTO.setAccountId(account.getId());
                  balanceDTO.setMaintenanceFee(account.getMaintenanceFee());
                  //balanceDTO.setAccountNumber(account.getAccountNumber());
                  balanceDTO.setType("Current Account");
                  balanceDTO.setAmount(balance);
                  return balanceDTO;
                });
          });
      return x;
    });
  }

  public Flux<BalanceDTO> getBalancesByCustomerId(String customerId, CustomerType customerType) {
    return Mono.just(customerId)
    .switchIfEmpty(Mono.error(new BankValidationException("Customer ID is required")))
    .flatMapMany(custId -> accountRepository.findByCustomerIdAndCustomerType(custId, customerType)
        .flatMap(account -> getBalanceByAccountId(account.getId())));
  }

  public Flux<Account> getAccountsByCustomer(String customerId, CustomerType customerType) {
    return Mono.just(customerId)
        .switchIfEmpty(Mono.error(new BankValidationException("Customer ID is required")))
        .then(check(customerType, ct -> ObjectUtils.isEmpty(ct), "Customer Type is required"))
        .flatMapMany(custId -> {
          return accountRepository.findByCustomerIdAndCustomerType(customerId, customerType);
        });
  }

  public Flux<Transaction> getTransactionsByAccountIdAndPeriod(String accountId, LocalDate period) {
    return Flux.just(accountId)
        .switchIfEmpty(Flux.error(new BankValidationException("Account Id is required")))
        .map(accId -> {
          if (Optional.ofNullable(period).isEmpty())
            return Flux.error(new BankValidationException("Period is required"));
          else
            return accId;
        }).flatMap(accId -> {
          var yearMonth = YearMonth.from(period);
          var currentMonthStart = yearMonth.atDay(1).atStartOfDay();
          var currentMonthEnd = yearMonth.atEndOfMonth().atTime(23, 59, 59);
          return transactionRepository.findByAccountIdAndRegisterDateBetween(accountId, currentMonthStart, currentMonthEnd);
        });
  }
}
