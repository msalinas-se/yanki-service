package com.nttdata.bank.yanki.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Yanki wallet, the owner does not need to be a customer of the bank. */
@Document("wallets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Wallet {
  @Id
  private String id;
  private DocumentType documentType;
  private String documentNumber;
  private String phone;
  private String imei;
  private String email;
  private BigDecimal balance;
  private String debitCardNumber;
}
