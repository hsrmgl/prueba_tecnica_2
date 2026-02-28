package com.itxiop.tech.supplier.sandbox.application.service;

import com.itxiop.tech.supplier.sandbox.application.port.in.UpdateSustainabilityRatingUseCase;
import com.itxiop.tech.supplier.sandbox.application.port.out.SupplierRepositoryPort;
import com.itxiop.tech.supplier.sandbox.domain.exception.DomainException;
import com.itxiop.tech.supplier.sandbox.domain.model.SupplierStatus;
import com.itxiop.tech.supplier.sandbox.domain.value.Duns;
import com.itxiop.tech.supplier.sandbox.domain.value.SustainabilityRating;

import java.util.Objects;

public final class UpdateSustainabilityRatingService implements UpdateSustainabilityRatingUseCase {

  private final SupplierRepositoryPort repo;

  public UpdateSustainabilityRatingService(SupplierRepositoryPort repo) {
    this.repo = Objects.requireNonNull(repo);
  }

  @Override
  public void update(Command cmd) {
    var s = repo.findByDuns(new Duns(cmd.duns()))
        .orElseThrow(() -> new DomainException("NOT_FOUND", "Supplier not found"));

    if (s.status() == SupplierStatus.CANDIDATE || s.status() == SupplierStatus.DECLINED) {
      throw new DomainException("CONFLICT", "Supplier not found");
    }

    s.updateRating(SustainabilityRating.valueOf(cmd.sustainabilityRating()));
    repo.save(s);
  }
}