package com.nttdata.bank.yanki;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the Yanki mobile wallet microservice. */
@SpringBootApplication
public class YankiServiceApplication {

  /** Starts the Yanki microservice. */
  public static void main(String[] args) {
    SpringApplication.run(YankiServiceApplication.class, args);
  }
}
