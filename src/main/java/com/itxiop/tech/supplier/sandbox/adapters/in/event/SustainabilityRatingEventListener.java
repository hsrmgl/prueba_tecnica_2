package com.itxiop.tech.supplier.sandbox.adapters.in.event;

import com.itxiop.tech.supplier.sandbox.application.port.in.UpdateSustainabilityRatingUseCase;
import com.itxiop.tech.supplier.sustainability.SustainabilityRatingEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SustainabilityRatingEventListener {

  private final UpdateSustainabilityRatingUseCase useCase;

  @EventListener
  public void on(SustainabilityRatingEvent event) {
    useCase.update(new UpdateSustainabilityRatingUseCase.Command(
        event.getDuns(),
        event.getScore()
    ));
  }
}