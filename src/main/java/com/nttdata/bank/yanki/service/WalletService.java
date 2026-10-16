package com.nttdata.bank.yanki.service;

import com.nttdata.bank.yanki.dto.LinkCardRequest;
import com.nttdata.bank.yanki.dto.PaymentRequest;
import com.nttdata.bank.yanki.dto.WalletRequest;
import com.nttdata.bank.yanki.event.WalletLinkResultEvent;
import com.nttdata.bank.yanki.event.WalletPaymentResultEvent;
import com.nttdata.bank.yanki.model.Wallet;
import com.nttdata.bank.yanki.model.WalletPayment;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** Operations of the Yanki wallet. */
public interface WalletService {

  /** Creates a wallet. */
  Single<Wallet> create(WalletRequest request);

  /** Lists every wallet. */
  Flowable<Wallet> findAll();

  /** Finds a wallet by its phone number. */
  Single<Wallet> findByPhone(String phone);

  /** Updates a wallet. */
  Single<Wallet> update(String phone, WalletRequest request);

  /** Deletes a wallet. */
  Completable delete(String phone);

  /** Requests to link the wallet to a debit card of the bank. */
  Single<Wallet> linkDebitCard(String phone, LinkCardRequest request);

  /** Sends money to another wallet using its phone number. */
  Single<WalletPayment> pay(PaymentRequest request);

  /** Lists the payments sent or received by a wallet. */
  Flowable<WalletPayment> findPayments(String phone);

  /** Saves the debit card when the bank confirms the link. */
  Completable onLinkResult(WalletLinkResultEvent event);

  /** Completes or rejects a payment when the bank answers. */
  Completable onPaymentResult(WalletPaymentResultEvent event);
}
