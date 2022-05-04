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
import org.modelmapper.ModelMapper;
import org.springframework.core.env.Environment;
import com.bank.bootcamp.currentaccounts.dto.AccountType;
import com.bank.bootcamp.currentaccounts.dto.BalanceDTO;
import com.bank.bootcamp.currentaccounts.dto.CreateAccountDTO;
import com.bank.bootcamp.currentaccounts.dto.CreateTransactionDTO;
import com.bank.bootcamp.currentaccounts.dto.TransferDTO;
import com.bank.bootcamp.currentaccounts.entity.Account;
import com.bank.bootcamp.currentaccounts.entity.CustomerType;
import com.bank.bootcamp.currentaccounts.entity.Transaction;
import com.bank.bootcamp.currentaccounts.exception.BankValidationException;
import com.bank.bootcamp.currentaccounts.repository.AccountRepository;
import com.bank.bootcamp.currentaccounts.repository.TransactionRepository;
import com.bank.bootcamp.currentaccounts.service.AccountService;
import com.bank.bootcamp.currentaccounts.service.NextSequenceService;
import com.bank.bootcamp.currentaccounts.webclient.AccountWebClient;
import com.bank.bootcamp.currentaccounts.webclient.CreditWebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class CurrentAccountsApplicationTests {

  private static AccountService accountService;
  private static AccountRepository accountRepository;
  private static TransactionRepository transactionRepository;
  private static NextSequenceService nextSequenceService;
  private static Environment env;
  private ModelMapper mapper = new ModelMapper();
  private static CreditWebClient creditWebClient;
  private static AccountWebClient accountWebClient;
  
  @BeforeAll
  public static void setup() {
    accountRepository = mock(AccountRepository.class);
    transactionRepository = mock(TransactionRepository.class);
    nextSequenceService = mock(NextSequenceService.class);
    env = mock(Environment.class);
    creditWebClient = mock(CreditWebClient.class);
    accountWebClient = mock(AccountWebClient.class);
    accountService = new AccountService(accountRepository, transactionRepository, nextSequenceService, env, creditWebClient, accountWebClient);
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
    var personalAccountDTO = mapper.map(personalAccount, CreateAccountDTO.class);
    personalAccountDTO.setOpeningAmount(100d);
    
    var savedPersonalAccount = mapper.map(personalAccount, Account.class);
    savedPersonalAccount.setId(UUID.randomUUID().toString());
    
    when(accountRepository.findByCustomerIdAndCustomerType(personalAccount.getCustomerId(), personalAccount.getCustomerType())).thenReturn(Flux.empty());
    when(accountRepository.save(Mockito.any(Account.class))).thenReturn(Mono.just(savedPersonalAccount));
    
    var mono = accountService.createAccount(personalAccountDTO);
    StepVerifier.create(mono).assertNext(acc -> {
      assertThat(acc.getId()).isNotNull();
    }).verifyComplete();
    
    var personalAccount2 = getPersonalAccount();
    var personalAccount2DTO = mapper.map(personalAccount2, CreateAccountDTO.class);
    var savedPersonalAccount2 = mapper.map(personalAccount2, Account.class);
    savedPersonalAccount2.setId(UUID.randomUUID().toString());
    
    when(accountRepository.findByCustomerIdAndCustomerType(personalAccount2.getCustomerId(), personalAccount2.getCustomerType())).thenReturn(Flux.just(savedPersonalAccount2));
    when(accountRepository.save(Mockito.any(Account.class))).thenReturn(Mono.just(savedPersonalAccount2));
    
    var mono2 = accountService.createAccount(personalAccount2DTO);
    StepVerifier.create(mono2)
      .expectError(BankValidationException.class)
      .verify();
    
  }
  
  @Test
  public void createTwoBusinessAccountWithAllData() throws Exception {
    
    var businessAccount = getBusinessAccount();
    var businessAccountDTO = mapper.map(businessAccount, CreateAccountDTO.class);
    businessAccountDTO.setOpeningAmount(100d);
    
    var savedBusinessAccount = mapper.map(businessAccount, Account.class);
    savedBusinessAccount.setId(UUID.randomUUID().toString());
    
    when(accountRepository.findByCustomerIdAndCustomerType(businessAccount.getCustomerId(), businessAccount.getCustomerType())).thenReturn(Flux.empty());
    when(accountRepository.save(Mockito.any(Account.class))).thenReturn(Mono.just(savedBusinessAccount));
    when(creditWebClient.hasOverdueDebt(Mockito.anyString(), Mockito.any(CustomerType.class))).thenReturn(Mono.just(Boolean.FALSE));
    
    var mono = accountService.createAccount(businessAccountDTO);
    StepVerifier.create(mono).assertNext(acc -> {
      assertThat(acc.getId()).isNotNull();
    }).verifyComplete();
    
    var businessAccount2 = getBusinessAccount();
    var businessAccount2DTO = mapper.map(businessAccount2, CreateAccountDTO.class);
    businessAccount2DTO.setOpeningAmount(100d);
    
    var savedBusinessAccount2 = mapper.map(businessAccount2, Account.class);
    savedBusinessAccount2.setId(UUID.randomUUID().toString());
    
    when(accountRepository.findByCustomerIdAndCustomerType(businessAccount2.getCustomerId(), businessAccount2.getCustomerType())).thenReturn(Flux.just(savedBusinessAccount));
    when(accountRepository.save(Mockito.any(Account.class))).thenReturn(Mono.just(savedBusinessAccount2));
    
    var mono2 = accountService.createAccount(businessAccount2DTO);
    StepVerifier.create(mono2).assertNext(acc -> {
      assertThat(acc.getId()).isNotNull();
    }).verifyComplete();
    
  }
  
  
  @Test
  public void createBusinessAccountWithPYMEProfile() throws Exception {
    
    var businessAccount = getBusinessAccount();
    var businessAccountDTO = mapper.map(businessAccount, CreateAccountDTO.class);
    businessAccountDTO.setOpeningAmount(100d);
    businessAccountDTO.setProfile("PYME");
    
    var savedBusinessAccount = mapper.map(businessAccount, Account.class);
    savedBusinessAccount.setId(UUID.randomUUID().toString());
    
    when(accountRepository.findByCustomerIdAndCustomerType(businessAccount.getCustomerId(), businessAccount.getCustomerType())).thenReturn(Flux.empty());
    when(accountRepository.save(Mockito.any(Account.class))).thenReturn(Mono.just(savedBusinessAccount));
    when(creditWebClient.getAllBalances(businessAccount.getCustomerId())).thenReturn(Flux.just(new BalanceDTO()));
    when(creditWebClient.hasOverdueDebt(Mockito.anyString(), Mockito.any(CustomerType.class))).thenReturn(Mono.just(Boolean.FALSE));
    
    var mono = accountService.createAccount(businessAccountDTO);
    StepVerifier.create(mono).assertNext(acc -> {
      assertThat(acc.getId()).isNotNull();
    }).verifyComplete();
    
  }
  
  @Test
  public void createBusinessAccountWithPYMEProfileFail() throws Exception {
    
    var businessAccount = getBusinessAccount();
    var businessAccountDTO = mapper.map(businessAccount, CreateAccountDTO.class);
    businessAccountDTO.setOpeningAmount(100d);
    businessAccountDTO.setProfile("PYME");
    
    var savedBusinessAccount = mapper.map(businessAccount, Account.class);
    savedBusinessAccount.setId(UUID.randomUUID().toString());
    
    when(accountRepository.findByCustomerIdAndCustomerType(businessAccount.getCustomerId(), businessAccount.getCustomerType())).thenReturn(Flux.empty());
    when(accountRepository.save(Mockito.any(Account.class))).thenReturn(Mono.just(savedBusinessAccount));
    when(creditWebClient.getAllBalances(businessAccount.getCustomerId())).thenReturn(Flux.empty());
    
    var mono = accountService.createAccount(businessAccountDTO);
    StepVerifier.create(mono).expectError(BankValidationException.class).verify();
    
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
    
    var transactionSaved = mapper.map(createTransactionDTO, Transaction.class);
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
    
    var transactionSaved = mapper.map(createTransactionDTO, Transaction.class);
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
    
    var transactionSaved = mapper.map(createTransactionDTO, Transaction.class);
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
  
  @Test
  public void transfer() {
    var transferDTO = new TransferDTO();
    transferDTO.setAmount(100d);
    transferDTO.setSourceAccountId("CA-001");
    transferDTO.setTargetAccountType(AccountType.SAVING);
    transferDTO.setTargetAccountId("SA-001");
    var amount = 100d;
    //  /transfer
    when(nextSequenceService.getNextSequence("TransactionSequences")).thenReturn(Mono.just(1));
    when(transactionRepository.getBalanceByAccountId(transferDTO.getSourceAccountId())).thenReturn(Mono.just(amount));
    when(accountRepository.findById(transferDTO.getSourceAccountId())).thenReturn(Mono.just(new Account()));
    
    var existentTransaction = new Transaction();
    existentTransaction.setAmount(100d);
    
    when(transactionRepository.findByAccountIdAndRegisterDateBetween(Mockito.anyString(), Mockito.any(LocalDateTime.class), Mockito.any(LocalDateTime.class))).thenReturn(Flux.just(existentTransaction));
    
    var tx = new Transaction();
    tx.setAccountId(transferDTO.getSourceAccountId());
    tx.setAgent("-");
    tx.setOperationNumber(1);
    tx.setAmount(amount);
    tx.setId(UUID.randomUUID().toString());
    tx.setRegisterDate(LocalDateTime.now());
    
    when(transactionRepository.save(Mockito.any(Transaction.class))).thenReturn(Mono.just(tx));
    when(accountWebClient.createTransaction(Mockito.any(AccountType.class), Mockito.any(CreateTransactionDTO.class))).thenReturn(Mono.just(4));
    var mono = accountService.transfer(transferDTO);
    StepVerifier.create(mono).assertNext(operationNumber -> {
      assertThat(operationNumber).isNotNull();
    }).verifyComplete();
  }
  
  @Test
  public void clienteSiPuedeAdquirirProducto() {
    //Un cliente no podrá adquirir un producto si posee alguna deuda vencida en algún producto de crédito.
    
    var personalAccount = getPersonalAccount();
    var personalAccountDTO = mapper.map(personalAccount, CreateAccountDTO.class);
    personalAccountDTO.setOpeningAmount(100d);
    
    var savedPersonalAccount = mapper.map(personalAccount, Account.class);
    savedPersonalAccount.setId(UUID.randomUUID().toString());
    
    when(accountRepository.findByCustomerIdAndCustomerType(personalAccount.getCustomerId(), personalAccount.getCustomerType())).thenReturn(Flux.empty());
    when(accountRepository.save(Mockito.any(Account.class))).thenReturn(Mono.just(savedPersonalAccount));
    when(creditWebClient.hasOverdueDebt(personalAccount.getCustomerId(), personalAccount.getCustomerType())).thenReturn(Mono.just(Boolean.FALSE));
    when(nextSequenceService.getNextSequence(Mockito.anyString())).thenReturn(Mono.just(1));
    var transaction = new Transaction();
    transaction.setAccountId(personalAccount.getId());
    when(transactionRepository.save(Mockito.any(Transaction.class))).thenReturn(Mono.just(transaction));
    
    var mono = accountService.createAccount(personalAccountDTO);
    StepVerifier.create(mono).assertNext(acc -> {
      assertThat(acc.getId()).isNotNull();
    }).verifyComplete();

    
  }
  
  
  @Test
  public void clienteNoPuedeAdquirirProducto() {
    //Un cliente no podrá adquirir un producto si posee alguna deuda vencida en algún producto de crédito.
    
    var personalAccount = getPersonalAccount();
    var personalAccountDTO = mapper.map(personalAccount, CreateAccountDTO.class);
    personalAccountDTO.setOpeningAmount(100d);
    
    var savedPersonalAccount = mapper.map(personalAccount, Account.class);
    savedPersonalAccount.setId(UUID.randomUUID().toString());
    
    when(accountRepository.findByCustomerIdAndCustomerType(personalAccount.getCustomerId(), personalAccount.getCustomerType())).thenReturn(Flux.empty());
    when(accountRepository.save(Mockito.any(Account.class))).thenReturn(Mono.just(savedPersonalAccount));
    when(creditWebClient.hasOverdueDebt(personalAccount.getCustomerId(), personalAccount.getCustomerType())).thenReturn(Mono.just(Boolean.TRUE));
    when(nextSequenceService.getNextSequence(Mockito.anyString())).thenReturn(Mono.just(1));
    var transaction = new Transaction();
    transaction.setAccountId(personalAccount.getId());
    when(transactionRepository.save(Mockito.any(Transaction.class))).thenReturn(Mono.just(transaction));
    
    var mono = accountService.createAccount(personalAccountDTO);
    StepVerifier.create(mono).expectError().verify();

    
  }
  
}
