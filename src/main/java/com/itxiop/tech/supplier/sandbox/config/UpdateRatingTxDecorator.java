package com.itxiop.tech.supplier.sandbox.config;

import com.itxiop.tech.supplier.sandbox.application.port.in.UpdateSustainabilityRatingUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Primary
public class UpdateRatingTxDecorator implements UpdateSustainabilityRatingUseCase {

  private final UpdateSustainabilityRatingUseCase delegate;

  public UpdateRatingTxDecorator(@Qualifier("updateRatingCore") UpdateSustainabilityRatingUseCase delegate) {
    this.delegate = delegate;
  }

  @Override
  @Transactional
  public void update(Command cmd) {
    delegate.update(cmd);
  }
}