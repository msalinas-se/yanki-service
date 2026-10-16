package com.nttdata.bank.yanki.event;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nttdata.bank.yanki.service.WalletService;
import io.reactivex.rxjava3.core.Completable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EventListenerTest {

  @Mock
  private WalletService walletService;

  @Test
  void receivesLinkResult() {
    WalletLinkResultEvent event = new WalletLinkResultEvent("w1", "5000", true, null);
    when(walletService.onLinkResult(event)).thenReturn(Completable.complete());

    new EventListener(walletService, new ObjectMapper())
        .onLinkResult("{\"walletId\":\"w1\",\"cardNumber\":\"5000\",\"success\":true}");

    verify(walletService).onLinkResult(event);
  }

  @Test
  void receivesPaymentResult() {
    WalletPaymentResultEvent event = new WalletPaymentResultEvent("p1", false, "no");
    when(walletService.onPaymentResult(event)).thenReturn(Completable.complete());

    new EventListener(walletService, new ObjectMapper())
        .onPaymentResult("{\"paymentId\":\"p1\",\"success\":false,\"reason\":\"no\"}");

    verify(walletService).onPaymentResult(event);
  }
}
