package com.nttdata.bank.yanki.repository;

import com.nttdata.bank.yanki.model.Wallet;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import org.springframework.data.repository.reactive.RxJava3CrudRepository;

/** Reactive repository for wallets. */
public interface WalletRepository extends RxJava3CrudRepository<Wallet, String> {

  /** Finds a wallet by its phone number. */
  Maybe<Wallet> findByPhone(String phone);

  /** Checks if a wallet already uses the phone number. */
  Single<Boolean> existsByPhone(String phone);
}
