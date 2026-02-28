package com.itxiop.tech.supplier.sandbox.adapters.in.rest;

import com.itxiop.tech.supplier.sandbox.application.port.in.CalculatePotentialSuppliersUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PotentialSuppliersController {

  private final CalculatePotentialSuppliersUseCase useCase;

  @GetMapping("/suppliers/potential")
  public CalculatePotentialSuppliersUseCase.Result potential(
      @RequestParam double rate,
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(defaultValue = "0") int offset
  ) {
    return useCase.calculate(new CalculatePotentialSuppliersUseCase.Command(rate, limit, offset));
  }
}