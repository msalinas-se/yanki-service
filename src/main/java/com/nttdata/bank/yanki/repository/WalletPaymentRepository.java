package com.nttdata.bank.yanki.repository;

import com.nttdata.bank.yanki.model.WalletPayment;
import io.reactivex.rxjava3.core.Flowable;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;

/** Reactive repository for wallet payments. */
public interface WalletPaymentRepository extends RxJava3CrudRepository<WalletPayment, String> {

  /** Lists the payments sent or received by a phone number. */
  Flowable<WalletPayment> findByFromPhoneOrToPhoneOrderByDateDesc(String fromPhone,
      String toPhone);
}
