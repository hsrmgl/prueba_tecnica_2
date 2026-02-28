package com.itxiop.tech.supplier.sandbox.config;

import com.itxiop.tech.supplier.sandbox.application.port.in.ApplyCandidateUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
public class ApplyCandidateTxDecorator implements ApplyCandidateUseCase {

  private final ApplyCandidateUseCase delegate;

  public ApplyCandidateTxDecorator(@Qualifier("applyCandidateCore") ApplyCandidateUseCase delegate) {
    this.delegate = delegate;
  }

  @Override
  @Transactional
  public void apply(Command cmd) {
    delegate.apply(cmd);
  }
}