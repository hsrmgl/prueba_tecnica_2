package com.itxiop.tech.supplier.sandbox.config;

import com.itxiop.tech.supplier.sandbox.application.port.in.AcceptCandidateUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
public class AcceptCandidateTxDecorator implements AcceptCandidateUseCase {

  private final AcceptCandidateUseCase delegate;

  public AcceptCandidateTxDecorator(@Qualifier("acceptCandidateCore") AcceptCandidateUseCase delegate) {
    this.delegate = delegate;
  }

  @Override
  @Transactional
  public void accept(Command cmd) {
    delegate.accept(cmd);
  }
}