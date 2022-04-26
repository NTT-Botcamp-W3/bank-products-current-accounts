package com.bank.bootcamp.currentaccounts.dto;

import lombok.Data;

@Data
public class CreateAccountDTO {

  private String customerId;
  private Integer monthlyMovementLimit;
}
