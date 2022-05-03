package com.bank.bootcamp.currentaccounts.dto;

import lombok.Data;

@Data
public class TransferOperation {

  private String sourceTransactionId;
  private String targetTransactionId;
}
