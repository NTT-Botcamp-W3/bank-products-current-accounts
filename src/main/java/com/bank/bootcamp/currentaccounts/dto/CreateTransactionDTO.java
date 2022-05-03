package com.bank.bootcamp.currentaccounts.dto;

import lombok.Data;

@Data
public class CreateTransactionDTO {
  
  private String accountId;
  private String agent;
  private String description;
  private Double amount;
  private Boolean createByMaintenanceFee = Boolean.FALSE;
  private Boolean createByComission = Boolean.FALSE;
  
}
