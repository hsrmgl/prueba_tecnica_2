package com.itxiop.tech.supplier.sandbox.application.service;

import com.itxiop.tech.supplier.sandbox.application.port.in.GetSupplierUseCase;
import com.itxiop.tech.supplier.sandbox.application.port.out.SupplierRepositoryPort;
import com.itxiop.tech.supplier.sandbox.domain.exception.DomainException;
import com.itxiop.tech.supplier.sandbox.domain.model.SupplierStatus;
import com.itxiop.tech.supplier.sandbox.domain.value.Duns;

import java.util.Objects;

public final class GetSupplierService implements GetSupplierUseCase {

  private final SupplierRepositoryPort repo;

  public GetSupplierService(SupplierRepositoryPort repo) {
    this.repo = Objects.requireNonNull(repo);
  }

  @Override
  public Result get(int duns) {
    var s = repo.findByDuns(new Duns(duns))
        .orElseThrow(() -> new DomainException("NOT_FOUND", "Supplier not found"));

    if (s.status() == SupplierStatus.CANDIDATE || s.status() == SupplierStatus.DECLINED) {
      throw new DomainException("NOT_FOUND", "Supplier not found");
    }

    String apiStatus = s.status().apiShowsActive() ? "Active" : "Disqualified";

    return new Result(
        s.name(),
        s.duns().value(),
        s.country().value(),
        s.annualTurnover().amount(),
        apiStatus,
        s.rating().name()
    );
  }
}