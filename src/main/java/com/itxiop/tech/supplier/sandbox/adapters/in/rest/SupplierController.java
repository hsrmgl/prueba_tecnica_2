package com.itxiop.tech.supplier.sandbox.adapters.in.rest;

import com.itxiop.tech.supplier.sandbox.adapters.in.rest.dto.*;
import com.itxiop.tech.supplier.sandbox.application.port.in.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SupplierController {

  private final GetSupplierUseCase getSupplier;
  private final BanSupplierUseCase banSupplier;
  private final CalculatePotentialSuppliersUseCase potentialSuppliers;

  @GetMapping("/suppliers/{duns}")
  public SupplierDto getSupplier(@PathVariable int duns) {
    var s = getSupplier.get(duns);
    return new SupplierDto(
        s.annualTurnover(), s.country(), s.duns(), s.name(),
        s.status(), s.sustainabilityRating()
    );
  }

  @PostMapping("/suppliers/{duns}/ban")
  public ResponseEntity<Void> ban(@PathVariable int duns) {
    banSupplier.ban(new BanSupplierUseCase.Command(duns));
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/suppliers/potential")
  public PotentialSuppliersDto potential(
      @RequestParam long rate,
      @RequestParam(defaultValue = "10") int limit,
      @RequestParam(defaultValue = "0") int offset
  ) {
    var res = potentialSuppliers.calculate(new CalculatePotentialSuppliersUseCase.Command(rate, limit, offset));

    List<PotentialSupplierDto> data = res.data().stream()
        .map(x -> new PotentialSupplierDto(
            x.annualTurnover(), x.country(), x.duns(), x.name(),
            x.status(), x.sustainabilityRating(), x.score()
        ))
        .toList();

    return new PotentialSuppliersDto(
        data,
        new PaginationDto(res.limit(), res.offset(), res.total())
    );
  }
}