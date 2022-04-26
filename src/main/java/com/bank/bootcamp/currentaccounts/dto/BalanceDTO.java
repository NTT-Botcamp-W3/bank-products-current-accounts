package com.bank.bootcamp.currentaccounts.dto;

import lombok.Data;

@Data
public class BalanceDTO {
  private String accountId;
  private String type;
  private Integer accountNumber;
  private Double amount;
  private Double maintenanceFee;
}