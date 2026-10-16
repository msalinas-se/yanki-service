package com.nttdata.bank.yanki.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.nttdata.bank.yanki.dto.LinkCardRequest;
import com.nttdata.bank.yanki.dto.PaymentRequest;
import com.nttdata.bank.yanki.dto.WalletRequest;
import com.nttdata.bank.yanki.exception.BusinessRuleException;
import com.nttdata.bank.yanki.exception.ResourceNotFoundException;
import com.nttdata.bank.yanki.model.Wallet;
import com.nttdata.bank.yanki.model.WalletPayment;
import com.nttdata.bank.yanki.service.WalletService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(WalletController.class)
class WalletControllerTest {

  @Autowired
  private WebTestClient webTestClient;

  @MockBean
  private WalletService walletService;

  private final Map<String, Object> wallet = Map.of("documentType", "DNI",
      "documentNumber", "45678912", "phone", "987654321", "imei", "356938035643809",
      "email", "ana@mail.com");

  @Test
  void walletEndpoints() {
    when(walletService.create(any(WalletRequest.class))).thenReturn(Single.just(new Wallet()));
    when(walletService.findAll()).thenReturn(Flowable.just(new Wallet()));
    when(walletService.findByPhone("1")).thenReturn(Single.just(new Wallet()));
    when(walletService.update(eq("1"), any(WalletRequest.class)))
        .thenReturn(Single.just(new Wallet()));
    when(walletService.delete("1")).thenReturn(Completable.complete());
    when(walletService.linkDebitCard(eq("1"), any(LinkCardRequest.class)))
        .thenReturn(Single.just(new Wallet()));
    when(walletService.pay(any(PaymentRequest.class)))
        .thenReturn(Single.just(new WalletPayment()));
    when(walletService.findPayments("1")).thenReturn(Flowable.just(new WalletPayment()));

    webTestClient.post().uri("/wallets").bodyValue(wallet).exchange()
        .expectStatus().isCreated();
    webTestClient.get().uri("/wallets").exchange().expectStatus().isOk();
    webTestClient.get().uri("/wallets/1").exchange().expectStatus().isOk();
    webTestClient.put().uri("/wallets/1").bodyValue(wallet).exchange().expectStatus().isOk();
    webTestClient.delete().uri("/wallets/1").exchange().expectStatus().isNoContent();
    webTestClient.post().uri("/wallets/1/debit-card").bodyValue(Map.of("cardNumber", "5000"))
        .exchange().expectStatus().isAccepted();
    webTestClient.post().uri("/wallets/payments")
        .bodyValue(Map.of("fromPhone", "1", "toPhone", "2", "amount", 10))
        .exchange().expectStatus().isCreated();
    webTestClient.get().uri("/wallets/1/payments").exchange().expectStatus().isOk();
  }

  @Test
  void errors() {
    when(walletService.findByPhone("1"))
        .thenReturn(Single.error(new ResourceNotFoundException("x")));
    when(walletService.findByPhone("2"))
        .thenReturn(Single.error(new BusinessRuleException("CODE", "x")));
    when(walletService.findByPhone("3")).thenReturn(Single.error(new IllegalStateException("x")));

    webTestClient.get().uri("/wallets/1").exchange().expectStatus().isNotFound();
    webTestClient.get().uri("/wallets/2").exchange().expectStatus().isBadRequest();
    webTestClient.get().uri("/wallets/3").exchange().expectStatus().is5xxServerError();
    webTestClient.post().uri("/wallets").bodyValue(Map.of()).exchange()
        .expectStatus().isBadRequest();
  }
}
