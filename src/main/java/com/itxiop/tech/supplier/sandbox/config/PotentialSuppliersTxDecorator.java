package com.itxiop.tech.supplier.sandbox.config;

import com.itxiop.tech.supplier.sandbox.application.port.in.CalculatePotentialSuppliersUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
public class PotentialSuppliersTxDecorator implements CalculatePotentialSuppliersUseCase {

  private final CalculatePotentialSuppliersUseCase delegate;

  public PotentialSuppliersTxDecorator(
      @Qualifier("potentialSuppliersCore") CalculatePotentialSuppliersUseCase delegate
  ) {
    this.delegate = delegate;
  }

  @Override
  @Transactional(readOnly = true)
  public Result calculate(Command cmd) {
    return delegate.calculate(cmd);
  }
}