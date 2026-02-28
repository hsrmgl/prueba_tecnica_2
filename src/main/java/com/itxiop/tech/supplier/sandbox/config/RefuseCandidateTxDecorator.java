package com.itxiop.tech.supplier.sandbox.config;

import com.itxiop.tech.supplier.sandbox.application.port.in.RefuseCandidateUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
public class RefuseCandidateTxDecorator implements RefuseCandidateUseCase {

  private final RefuseCandidateUseCase delegate;

  public RefuseCandidateTxDecorator(@Qualifier("refuseCandidateCore") RefuseCandidateUseCase delegate) {
    this.delegate = delegate;
  }

  @Override
  @Transactional
  public void refuse(Command cmd) {
    delegate.refuse(cmd);
  }
}