package com.nttdata.bank.yanki.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Payment sent from one wallet to another using the phone number. */
@Document("wallet_payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletPayment {
  @Id
  private String id;
  private String fromPhone;
  private String toPhone;
  private BigDecimal amount;
  private PaymentStatus status;
  private String reason;
  private LocalDateTime date;
}
