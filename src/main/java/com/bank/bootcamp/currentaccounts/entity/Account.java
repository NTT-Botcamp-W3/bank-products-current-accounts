package com.bank.bootcamp.currentaccounts.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;

@Document("Accounts")
@Data
public class Account {

  @Id
  private String id;
  
  private CustomerType customerType;
  private String customerId;
  private Double maintenanceFee;
}
