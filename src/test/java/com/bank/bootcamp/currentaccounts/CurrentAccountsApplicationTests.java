package com.bank.bootcamp.currentaccounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.bank.bootcamp.currentaccounts.dto.CreateTransactionDTO;
import com.bank.bootcamp.currentaccounts.entity.Account;
import com.bank.bootcamp.currentaccounts.entity.CustomerType;
import com.bank.bootcamp.currentaccounts.entity.Transaction;
import com.bank.bootcamp.currentaccounts.exception.BankValidationException;
import com.bank.bootcamp.currentaccounts.repository.AccountRepository;
import com.bank.bootcamp.currentaccounts.repository.TransactionRepository;
import com.bank.bootcamp.currentaccounts.service.AccountService;
import com.bank.bootcamp.currentaccounts.service.NextSequenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class CurrentAccountsApplicationTests {

  private static AccountService accountService;
  private static AccountRepository accountRepository;
  private static TransactionRepository transactionRepository;
  private static NextSequenceService nextSequenceService;
  private ObjectMapper mapper = new ObjectMapper();
  
  @BeforeAll
  public static void setup() {
    accountRepository = mock(AccountRepository.class);
    transactionRepository = mock(TransactionRepository.class);
    nextSequenceService = mock(NextSequenceService.class);
    accountService = new AccountService(accountRepository, transactionRepository, nextSequenceService);
  }
  
  private Account getPersonalAccount() {
    var account = new Account();
    account.setCustomerId("id123456");
    account.setCustomerType(CustomerType.PERSONAL);
    account.setMaintenanceFee(5d);
    return account;
  }
  
  private Account getBusinessAccount() {
    var account = new Account();
    account.setCustomerId("bb123456");
    account.setCustomerType(CustomerType.BUSINESS);
    account.setMaintenanceFee(5d);
    return account;
  }

  @Test
  public void createTwoPersonalAccountWithAllData() throws Exception {
    
    var personalAccount = getPersonalAccount();
    
    var savedPersonalAccount = mapper.readValue(mapper.writeValueAsString(personalAccount), Account.class);
    savedPersonalAccount.setId(UUID.randomUUID().toString());
    
    when(accountRepository.findByCustomerIdAndCustomerType(personalAccount.getCustomerId(), personalAccount.getCustomerType())).thenReturn(Flux.empty());
    when(accountRepository.save(personalAccount)).thenReturn(Mono.just(savedPersonalAccount));
    
    var mono = accountService.createAccount(personalAccount);
    StepVerifier.create(mono).assertNext(acc -> {
      assertThat(acc.getId()).isNotNull();
    }).verifyComplete();
    
    var personalAccount2 = getPersonalAccount();
    var savedPersonalAccount2 = mapper.readValue(mapper.writeValueAsString(personalAccount2), Account.class);
    savedPersonalAccount2.setId(UUID.randomUUID().toString());
    
    when(accountRepository.findByCustomerIdAndCustomerType(personalAccount2.getCustomerId(), personalAccount2.getCustomerType())).thenReturn(Flux.just(savedPersonalAccount2));
    when(accountRepository.save(personalAccount2)).thenReturn(Mono.just(savedPersonalAccount2));
    
    var mono2 = accountService.createAccount(personalAccount2);
    StepVerifier.create(mono2)
      .expectError(BankValidationException.class)
      .verify();
    
  }
  
  @Test
  public void createTwoBusinessAccountWithAllData() throws Exception {
    
    var businessAccount = getBusinessAccount();
    
    var savedBusinessAccount = mapper.readValue(mapper.writeValueAsString(businessAccount), Account.class);
    savedBusinessAccount.setId(UUID.randomUUID().toString());
    
    when(accountRepository.findByCustomerIdAndCustomerType(businessAccount.getCustomerId(), businessAccount.getCustomerType())).thenReturn(Flux.empty());
    when(accountRepository.save(businessAccount)).thenReturn(Mono.just(savedBusinessAccount));
    
    var mono = accountService.createAccount(businessAccount);
    StepVerifier.create(mono).assertNext(acc -> {
      assertThat(acc.getId()).isNotNull();
    }).verifyComplete();
    
    var businessAccount2 = getBusinessAccount();
    var savedBusinessAccount2 = mapper.readValue(mapper.writeValueAsString(businessAccount2), Account.class);
    savedBusinessAccount2.setId(UUID.randomUUID().toString());
    
    when(accountRepository.findByCustomerIdAndCustomerType(businessAccount2.getCustomerId(), businessAccount2.getCustomerType())).thenReturn(Flux.just(savedBusinessAccount));
    when(accountRepository.save(businessAccount2)).thenReturn(Mono.just(savedBusinessAccount2));
    
    var mono2 = accountService.createAccount(businessAccount2);
    StepVerifier.create(mono2).assertNext(acc -> {
      assertThat(acc.getId()).isNotNull();
    }).verifyComplete();
    
  }
  
  @Test
  public void createPositiveTransactionWithExistentAccount() throws Exception {
    var accountId = "acc123";
    
    var account = new Account();
    account.setId(accountId);
    account.setMaintenanceFee(5d);
    
    var createTransactionDTO = new CreateTransactionDTO();
    createTransactionDTO.setAgent("BCP Huacho - Cajero 021");
    createTransactionDTO.setAmount(100d);
    createTransactionDTO.setAccountId(accountId);
    createTransactionDTO.setDescription("Deposito cajero");
    
    var transactionSaved = mapper.readValue(mapper.writeValueAsString(createTransactionDTO), Transaction.class);
    transactionSaved.setId(UUID.randomUUID().toString());
    transactionSaved.setOperationNumber(1);
    transactionSaved.setRegisterDate(LocalDateTime.now());
    
    when(nextSequenceService.getNextSequence("TransactionSequences")).thenReturn(Mono.just(1));
    when(transactionRepository.getBalanceByAccountId(accountId)).thenReturn(Mono.just(0d));
    when(accountRepository.findById(accountId)).thenReturn(Mono.just(account));
    Mockito.doReturn(Flux.empty()).when(transactionRepository).findByAccountIdAndRegisterDateBetween(Mockito.anyString(), Mockito.any(LocalDateTime.class), Mockito.any(LocalDateTime.class));
    Mockito.doReturn(Mono.just(transactionSaved)).when(transactionRepository).save(Mockito.any());
    
    var mono = accountService.createTransaction(createTransactionDTO);
    StepVerifier.create(mono).assertNext((saved) -> {
      assertThat(saved).isNotNull();
    }).verifyComplete();
  }
  
  @Test
  public void createImposibleTransactionWithExistentAccount() throws Exception {
    var accountId = "acc123";
    var createTransactionDTO = new CreateTransactionDTO();
    createTransactionDTO.setAgent("BCP Huacho - Cajero 021");
    createTransactionDTO.setAmount(-100d); // negative tx with balance 0
    createTransactionDTO.setAccountId(accountId);
    createTransactionDTO.setDescription("Deposito cajero");
    
    var transactionSaved = mapper.readValue(mapper.writeValueAsString(createTransactionDTO), Transaction.class);
    transactionSaved.setId(UUID.randomUUID().toString());
    transactionSaved.setOperationNumber(1);
    transactionSaved.setRegisterDate(LocalDateTime.now());
    
    when(nextSequenceService.getNextSequence("TransactionSequences")).thenReturn(Mono.just(1));
    when(transactionRepository.getBalanceByAccountId(accountId)).thenReturn(Mono.just(0d));
    when(accountRepository.findById(accountId)).thenReturn(Mono.just(new Account()));
    Mockito.doReturn(Mono.just(transactionSaved)).when(transactionRepository).save(Mockito.any());
    
    var mono = accountService.createTransaction(createTransactionDTO);
    StepVerifier.create(mono).expectError().verify();
  }
  
  @Test
  public void createPositiveTransactionWithInexistentAccount() throws Exception {
    var accountId = "acc123";
    var createTransactionDTO = new CreateTransactionDTO();
    createTransactionDTO.setAgent("BCP Huacho - Cajero 021");
    createTransactionDTO.setAmount(100d);
    createTransactionDTO.setAccountId(accountId);
    createTransactionDTO.setDescription("Deposito cajero");
    
    var transactionSaved = mapper.readValue(mapper.writeValueAsString(createTransactionDTO), Transaction.class);
    transactionSaved.setId(UUID.randomUUID().toString());
    transactionSaved.setOperationNumber(1);
    transactionSaved.setRegisterDate(LocalDateTime.now());
    
    when(nextSequenceService.getNextSequence("TransactionSequences")).thenReturn(Mono.just(1));
    when(transactionRepository.getBalanceByAccountId(accountId)).thenReturn(Mono.just(0d));
    when(accountRepository.findById(accountId)).thenReturn(Mono.empty()); // inexistent account
    Mockito.doReturn(Mono.just(transactionSaved)).when(transactionRepository).save(Mockito.any());
    
    var mono = accountService.createTransaction(createTransactionDTO);
    StepVerifier.create(mono).expectError().verify();
  }
  
  @Test
  public void getBalanceTest() {
    var accountId = "account_123";
    var account = new Account();
    account.setId(accountId);
    account.setMaintenanceFee(5d);
    
    when(transactionRepository.getBalanceByAccountId(accountId)).thenReturn(Mono.just(100d));
    when(accountRepository.findById(accountId)).thenReturn(Mono.just(account));
    var transaction = new Transaction();
    transaction.setAmount(100d);
    when(transactionRepository.findByAccountIdAndRegisterDateBetween(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(Flux.just(transaction));
    var mono = accountService.getBalanceByAccountId(accountId);
    StepVerifier.create(mono).assertNext(balance -> {
      assertThat(balance.getAmount()).isEqualTo(100d);
    }).verifyComplete();
  }
  
  @Test
  public void getTransactionsByAccountAndPeriod() {
    
    String accountId = "ACC123";
    when(transactionRepository.findByAccountIdAndRegisterDateBetween(Mockito.any(), Mockito.any(), Mockito.any()))
      .thenReturn(Flux.just(new Transaction()));
    var flux = accountService.getTransactionsByAccountIdAndPeriod(accountId, LocalDate.of(2022, 4, 1));
    StepVerifier.create(flux).assertNext(tx -> {
      assertThat(tx).isNotNull();
    }).verifyComplete();
  }
  

}
