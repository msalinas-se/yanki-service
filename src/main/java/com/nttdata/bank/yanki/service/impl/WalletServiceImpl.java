package com.nttdata.bank.yanki.service.impl;

import com.nttdata.bank.yanki.dto.LinkCardRequest;
import com.nttdata.bank.yanki.dto.PaymentRequest;
import com.nttdata.bank.yanki.dto.WalletRequest;
import com.nttdata.bank.yanki.event.EventPublisher;
import com.nttdata.bank.yanki.event.WalletLinkEvent;
import com.nttdata.bank.yanki.event.WalletLinkResultEvent;
import com.nttdata.bank.yanki.event.WalletPaymentEvent;
import com.nttdata.bank.yanki.event.WalletPaymentResultEvent;
import com.nttdata.bank.yanki.exception.BusinessRuleException;
import com.nttdata.bank.yanki.exception.ResourceNotFoundException;
import com.nttdata.bank.yanki.model.PaymentStatus;
import com.nttdata.bank.yanki.model.Wallet;
import com.nttdata.bank.yanki.model.WalletPayment;
import com.nttdata.bank.yanki.repository.WalletPaymentRepository;
import com.nttdata.bank.yanki.repository.WalletRepository;
import com.nttdata.bank.yanki.service.WalletService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Implementation of WalletService, talks to the bank only through Kafka. */
@Slf4j
@Service
public class WalletServiceImpl implements WalletService {
  private final WalletRepository walletRepository;
  private final WalletPaymentRepository paymentRepository;
  private final EventPublisher eventPublisher;
  private final String walletLinkTopic;
  private final String walletPaymentTopic;

  /** Creates the service with the topics used to talk to the bank. */
  public WalletServiceImpl(WalletRepository walletRepository,
      WalletPaymentRepository paymentRepository, EventPublisher eventPublisher,
      @Value("${topics.wallet-link}") String walletLinkTopic,
      @Value("${topics.wallet-payment}") String walletPaymentTopic) {
    this.walletRepository = walletRepository;
    this.paymentRepository = paymentRepository;
    this.eventPublisher = eventPublisher;
    this.walletLinkTopic = walletLinkTopic;
    this.walletPaymentTopic = walletPaymentTopic;
  }

  /** Creates a wallet. */
  @Override
  public Single<Wallet> create(WalletRequest request) {
    return walletRepository.existsByPhone(request.phone())
        .flatMap(exists -> {
          if (exists) {
            return Single.error(new BusinessRuleException("DUPLICATE_PHONE",
                "A wallet with phone " + request.phone() + " already exists"));
          }
          return walletRepository.save(Wallet.builder()
              .documentType(request.documentType())
              .documentNumber(request.documentNumber())
              .phone(request.phone())
              .imei(request.imei())
              .email(request.email())
              .balance(BigDecimal.ZERO)
              .build());
        })
        .doOnSuccess(wallet -> log.info("Wallet created: {}", wallet.getPhone()));
  }

  /** Lists every wallet. */
  @Override
  public Flowable<Wallet> findAll() {
    return walletRepository.findAll();
  }

  /** Finds a wallet by its phone number. */
  @Override
  public Single<Wallet> findByPhone(String phone) {
    return walletRepository.findByPhone(phone)
        .switchIfEmpty(Single.error(new ResourceNotFoundException("Wallet not found: " + phone)));
  }

  /** Updates a wallet. */
  @Override
  public Single<Wallet> update(String phone, WalletRequest request) {
    return findByPhone(phone)
        .flatMap(wallet -> {
          wallet.setDocumentType(request.documentType());
          wallet.setDocumentNumber(request.documentNumber());
          wallet.setImei(request.imei());
          wallet.setEmail(request.email());
          return walletRepository.save(wallet);
        });
  }

  /** Deletes a wallet. */
  @Override
  public Completable delete(String phone) {
    return findByPhone(phone).flatMapCompletable(walletRepository::delete);
  }

  /** Requests to link the wallet to a debit card of the bank. */
  @Override
  public Single<Wallet> linkDebitCard(String phone, LinkCardRequest request) {
    return findByPhone(phone)
        .flatMap(wallet -> eventPublisher.publish(walletLinkTopic, wallet.getId(),
                new WalletLinkEvent(wallet.getId(), request.cardNumber()))
            .toSingleDefault(wallet));
  }

  /** Sends money to another wallet using its phone number. */
  @Override
  public Single<WalletPayment> pay(PaymentRequest request) {
    if (request.fromPhone().equals(request.toPhone())) {
      return Single.error(new BusinessRuleException("SAME_WALLET",
          "Origin and destination must be different"));
    }
    return Single.zip(findByPhone(request.fromPhone()), findByPhone(request.toPhone()), List::of)
        .flatMap(wallets -> {
          Wallet from = wallets.get(0);
          Wallet to = wallets.get(1);
          if (from.getDebitCardNumber() == null
              && from.getBalance().compareTo(request.amount()) < 0) {
            return Single.error(new BusinessRuleException("INSUFFICIENT_FUNDS",
                "Insufficient funds"));
          }
          WalletPayment payment = WalletPayment.builder()
              .fromPhone(from.getPhone())
              .toPhone(to.getPhone())
              .amount(request.amount())
              .date(LocalDateTime.now())
              .build();
          if (from.getDebitCardNumber() == null && to.getDebitCardNumber() == null) {
            payment.setStatus(PaymentStatus.COMPLETED);
            return applyOnWallets(from, to, request.amount())
                .andThen(Single.defer(() -> paymentRepository.save(payment)));
          }
          payment.setStatus(PaymentStatus.PENDING);
          return paymentRepository.save(payment)
              .flatMap(saved -> eventPublisher.publish(walletPaymentTopic, saved.getId(),
                      new WalletPaymentEvent(saved.getId(), saved.getAmount(),
                          from.getDebitCardNumber(), to.getDebitCardNumber()))
                  .toSingleDefault(saved));
        })
        .doOnSuccess(payment -> log.info("Payment {} from {} to {}: {}", payment.getAmount(),
            payment.getFromPhone(), payment.getToPhone(), payment.getStatus()));
  }

  /** Lists the payments sent or received by a wallet. */
  @Override
  public Flowable<WalletPayment> findPayments(String phone) {
    return paymentRepository.findByFromPhoneOrToPhoneOrderByDateDesc(phone, phone);
  }

  /** Saves the debit card when the bank confirms the link. */
  @Override
  public Completable onLinkResult(WalletLinkResultEvent event) {
    if (!event.success()) {
      log.warn("Debit card {} was not linked: {}", event.cardNumber(), event.reason());
      return Completable.complete();
    }
    return walletRepository.findById(event.walletId())
        .flatMapSingle(wallet -> {
          wallet.setDebitCardNumber(event.cardNumber());
          return walletRepository.save(wallet);
        })
        .ignoreElement()
        .doOnComplete(() -> log.info("Wallet {} linked to card {}", event.walletId(),
            event.cardNumber()));
  }

  /** Completes or rejects a payment when the bank answers. */
  @Override
  public Completable onPaymentResult(WalletPaymentResultEvent event) {
    return paymentRepository.findById(event.paymentId())
        .flatMapCompletable(payment -> {
          if (!event.success()) {
            payment.setStatus(PaymentStatus.REJECTED);
            payment.setReason(event.reason());
            return paymentRepository.save(payment).ignoreElement();
          }
          payment.setStatus(PaymentStatus.COMPLETED);
          return Single.zip(findByPhone(payment.getFromPhone()),
                  findByPhone(payment.getToPhone()), List::of)
              .flatMapCompletable(wallets -> applyOnWallets(wallets.get(0), wallets.get(1),
                  payment.getAmount()))
              .andThen(paymentRepository.save(payment).ignoreElement());
        });
  }

  private Completable applyOnWallets(Wallet from, Wallet to, BigDecimal amount) {
    Completable debit = Completable.complete();
    Completable credit = Completable.complete();
    if (from.getDebitCardNumber() == null) {
      from.setBalance(from.getBalance().subtract(amount));
      debit = walletRepository.save(from).ignoreElement();
    }
    if (to.getDebitCardNumber() == null) {
      to.setBalance(to.getBalance().add(amount));
      credit = walletRepository.save(to).ignoreElement();
    }
    return debit.andThen(credit);
  }
}
