package com.nttdata.bank.yanki.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.nttdata.bank.yanki.model.DocumentType;
import com.nttdata.bank.yanki.model.PaymentStatus;
import com.nttdata.bank.yanki.model.Wallet;
import com.nttdata.bank.yanki.model.WalletPayment;
import com.nttdata.bank.yanki.repository.WalletPaymentRepository;
import com.nttdata.bank.yanki.repository.WalletRepository;
import com.nttdata.bank.yanki.service.impl.WalletServiceImpl;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

  @Mock
  private WalletRepository walletRepository;

  @Mock
  private WalletPaymentRepository paymentRepository;

  @Mock
  private EventPublisher eventPublisher;

  private WalletServiceImpl walletService;

  private final WalletRequest request = new WalletRequest(DocumentType.DNI, "45678912",
      "987654321", "356938035643809", "ana@mail.com");

  @BeforeEach
  void setUp() {
    walletService = new WalletServiceImpl(walletRepository, paymentRepository, eventPublisher,
        "wallet-link", "wallet-payment");
  }

  private Wallet wallet(String phone, String balance, String card) {
    return Wallet.builder().id("w" + phone).phone(phone).balance(new BigDecimal(balance))
        .debitCardNumber(card).build();
  }

  private void saveWallets() {
    when(walletRepository.save(any(Wallet.class)))
        .thenAnswer(invocation -> Single.just(invocation.getArgument(0)));
  }

  private void savePayments() {
    when(paymentRepository.save(any(WalletPayment.class))).thenAnswer(invocation -> {
      WalletPayment payment = invocation.getArgument(0);
      payment.setId("p1");
      return Single.just(payment);
    });
  }

  @Test
  void createWallet() {
    when(walletRepository.existsByPhone("987654321")).thenReturn(Single.just(false));
    saveWallets();

    walletService.create(request).test()
        .assertValue(wallet -> wallet.getBalance().signum() == 0
            && wallet.getImei().equals("356938035643809"));
  }

  @Test
  void createWalletWithDuplicatePhone() {
    when(walletRepository.existsByPhone("987654321")).thenReturn(Single.just(true));

    walletService.create(request).test().assertError(BusinessRuleException.class);
  }

  @Test
  void findWallets() {
    when(walletRepository.findAll()).thenReturn(Flowable.just(wallet("1", "0", null)));

    walletService.findAll().test().assertValueCount(1);
  }

  @Test
  void walletNotFound() {
    when(walletRepository.findByPhone("1")).thenReturn(Maybe.empty());

    walletService.findByPhone("1").test().assertError(ResourceNotFoundException.class);
  }

  @Test
  void updateWallet() {
    when(walletRepository.findByPhone("987654321"))
        .thenReturn(Maybe.just(wallet("987654321", "0", null)));
    saveWallets();

    walletService.update("987654321", request).test()
        .assertValue(wallet -> wallet.getEmail().equals("ana@mail.com"));
  }

  @Test
  void deleteWallet() {
    Wallet wallet = wallet("1", "0", null);
    when(walletRepository.findByPhone("1")).thenReturn(Maybe.just(wallet));
    when(walletRepository.delete(wallet)).thenReturn(Completable.complete());

    walletService.delete("1").test().assertComplete();
  }

  @Test
  void linkDebitCardPublishesEvent() {
    when(walletRepository.findByPhone("1")).thenReturn(Maybe.just(wallet("1", "0", null)));
    when(eventPublisher.publish("wallet-link", "w1", new WalletLinkEvent("w1", "5000")))
        .thenReturn(Completable.complete());

    walletService.linkDebitCard("1", new LinkCardRequest("5000")).test().assertComplete();
    verify(eventPublisher).publish("wallet-link", "w1", new WalletLinkEvent("w1", "5000"));
  }

  @Test
  void paymentBetweenWalletsWithoutCards() {
    Wallet from = wallet("1", "100", null);
    Wallet to = wallet("2", "0", null);
    when(walletRepository.findByPhone("1")).thenReturn(Maybe.just(from));
    when(walletRepository.findByPhone("2")).thenReturn(Maybe.just(to));
    saveWallets();
    savePayments();

    walletService.pay(new PaymentRequest("1", "2", new BigDecimal("30"))).test()
        .assertValue(payment -> payment.getStatus() == PaymentStatus.COMPLETED);
    assertEquals(70, from.getBalance().intValue());
    assertEquals(30, to.getBalance().intValue());
    verify(eventPublisher, never()).publish(any(), any(), any());
  }

  @Test
  void paymentWithoutFunds() {
    when(walletRepository.findByPhone("1")).thenReturn(Maybe.just(wallet("1", "10", null)));
    when(walletRepository.findByPhone("2")).thenReturn(Maybe.just(wallet("2", "0", null)));

    walletService.pay(new PaymentRequest("1", "2", new BigDecimal("30"))).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void paymentToSameWallet() {
    walletService.pay(new PaymentRequest("1", "1", BigDecimal.ONE)).test()
        .assertError(BusinessRuleException.class);
  }

  @Test
  void paymentWithLinkedCardIsSentToTheBank() {
    when(walletRepository.findByPhone("1")).thenReturn(Maybe.just(wallet("1", "0", "5000")));
    when(walletRepository.findByPhone("2")).thenReturn(Maybe.just(wallet("2", "0", null)));
    savePayments();
    when(eventPublisher.publish(eq("wallet-payment"), eq("p1"), any(WalletPaymentEvent.class)))
        .thenReturn(Completable.complete());

    walletService.pay(new PaymentRequest("1", "2", new BigDecimal("30"))).test()
        .assertValue(payment -> payment.getStatus() == PaymentStatus.PENDING);
    verify(eventPublisher).publish("wallet-payment", "p1",
        new WalletPaymentEvent("p1", new BigDecimal("30"), "5000", null));
  }

  @Test
  void findPayments() {
    when(paymentRepository.findByFromPhoneOrToPhoneOrderByDateDesc("1", "1"))
        .thenReturn(Flowable.just(new WalletPayment()));

    walletService.findPayments("1").test().assertValueCount(1);
  }

  @Test
  void linkResultSavesCard() {
    Wallet wallet = wallet("1", "0", null);
    when(walletRepository.findById("w1")).thenReturn(Maybe.just(wallet));
    saveWallets();

    walletService.onLinkResult(new WalletLinkResultEvent("w1", "5000", true, null)).test()
        .assertComplete();
    assertEquals("5000", wallet.getDebitCardNumber());
  }

  @Test
  void failedLinkResultIsIgnored() {
    walletService.onLinkResult(new WalletLinkResultEvent("w1", "9", false, "not found")).test()
        .assertComplete();
    verify(walletRepository, never()).findById(any(String.class));
  }

  @Test
  void successfulPaymentResultCreditsWalletWithoutCard() {
    WalletPayment payment = WalletPayment.builder().id("p1").fromPhone("1").toPhone("2")
        .amount(new BigDecimal("30")).status(PaymentStatus.PENDING).build();
    Wallet to = wallet("2", "0", null);
    when(paymentRepository.findById("p1")).thenReturn(Maybe.just(payment));
    when(walletRepository.findByPhone("1")).thenReturn(Maybe.just(wallet("1", "0", "5000")));
    when(walletRepository.findByPhone("2")).thenReturn(Maybe.just(to));
    saveWallets();
    savePayments();

    walletService.onPaymentResult(new WalletPaymentResultEvent("p1", true, null)).test()
        .assertComplete();
    assertEquals(PaymentStatus.COMPLETED, payment.getStatus());
    assertEquals(30, to.getBalance().intValue());
  }

  @Test
  void rejectedPaymentResult() {
    WalletPayment payment = WalletPayment.builder().id("p1").status(PaymentStatus.PENDING)
        .build();
    when(paymentRepository.findById("p1")).thenReturn(Maybe.just(payment));
    savePayments();

    walletService.onPaymentResult(new WalletPaymentResultEvent("p1", false, "Insufficient funds"))
        .test().assertComplete();
    assertEquals(PaymentStatus.REJECTED, payment.getStatus());
  }
}
