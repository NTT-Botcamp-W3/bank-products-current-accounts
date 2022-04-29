package com.bank.bootcamp.currentaccounts.dto;

import com.bank.bootcamp.currentaccounts.entity.Account;
import com.bank.bootcamp.currentaccounts.entity.CustomerType;
import lombok.Data;

@Data
public class CreateAccountDTO {

  private String customerId;
  private Double maintenanceFee;
  private CustomerType customerType;
  private Double openingAmount;
  
  public Account toAccount() {
    var account = new Account();
    account.setCustomerId(customerId);
    account.setCustomerType(customerType);
    account.setMaintenanceFee(maintenanceFee);
    return account;
  }
}
