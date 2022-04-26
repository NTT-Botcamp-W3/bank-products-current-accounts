package com.bank.bootcamp.currentaccounts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

@SpringBootApplication
@EnableEurekaClient
public class CurrentAccountsApplication {

  public static void main(String[] args) {
    System.setProperty("jdk.tls.client.protocols", "TLSv1.2");
    SpringApplication.run(CurrentAccountsApplication.class, args);
  }

}
