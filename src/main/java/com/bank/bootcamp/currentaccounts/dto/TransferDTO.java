package com.bank.bootcamp.currentaccounts.dto;

import lombok.Data;

@Data
public class TransferDTO {
  
  private String sourceAccountId;
  private AccountType targetAccountType;
  private String targetAccountId;
  private Double amount;

}
