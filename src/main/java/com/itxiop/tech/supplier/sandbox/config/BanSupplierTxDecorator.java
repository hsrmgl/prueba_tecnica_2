package com.itxiop.tech.supplier.sandbox.config;

import com.itxiop.tech.supplier.sandbox.application.port.in.BanSupplierUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
public class BanSupplierTxDecorator implements BanSupplierUseCase {

  private final BanSupplierUseCase delegate;

  public BanSupplierTxDecorator(@Qualifier("banSupplierCore") BanSupplierUseCase delegate) {
    this.delegate = delegate;
  }

  @Override
  @Transactional
  public void ban(Command cmd) {
    delegate.ban(cmd);
  }
}