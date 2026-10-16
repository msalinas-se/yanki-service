package com.nttdata.bank.yanki.controller;

import com.nttdata.bank.yanki.dto.LinkCardRequest;
import com.nttdata.bank.yanki.dto.PaymentRequest;
import com.nttdata.bank.yanki.dto.WalletRequest;
import com.nttdata.bank.yanki.model.Wallet;
import com.nttdata.bank.yanki.model.WalletPayment;
import com.nttdata.bank.yanki.service.WalletService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoints of the Yanki wallet. */
@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {
  private final WalletService walletService;

  /** Creates a wallet. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public Single<Wallet> create(@Valid @RequestBody WalletRequest request) {
    return walletService.create(request);
  }

  /** Lists every wallet. */
  @GetMapping
  public Flowable<Wallet> findAll() {
    return walletService.findAll();
  }

  /** Gets a wallet by phone number. */
  @GetMapping("/{phone}")
  public Single<Wallet> findByPhone(@PathVariable String phone) {
    return walletService.findByPhone(phone);
  }

  /** Updates a wallet. */
  @PutMapping("/{phone}")
  public Single<Wallet> update(@PathVariable String phone,
      @Valid @RequestBody WalletRequest request) {
    return walletService.update(phone, request);
  }

  /** Deletes a wallet. */
  @DeleteMapping("/{phone}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public Completable delete(@PathVariable String phone) {
    return walletService.delete(phone);
  }

  /** Requests to link the wallet to a debit card, the bank answers asynchronously. */
  @PostMapping("/{phone}/debit-card")
  @ResponseStatus(HttpStatus.ACCEPTED)
  public Single<Wallet> linkDebitCard(@PathVariable String phone,
      @Valid @RequestBody LinkCardRequest request) {
    return walletService.linkDebitCard(phone, request);
  }

  /** Sends money to another wallet using its phone number. */
  @PostMapping("/payments")
  @ResponseStatus(HttpStatus.CREATED)
  public Single<WalletPayment> pay(@Valid @RequestBody PaymentRequest request) {
    return walletService.pay(request);
  }

  /** Lists the payments of a wallet. */
  @GetMapping("/{phone}/payments")
  public Flowable<WalletPayment> findPayments(@PathVariable String phone) {
    return walletService.findPayments(phone);
  }
}
