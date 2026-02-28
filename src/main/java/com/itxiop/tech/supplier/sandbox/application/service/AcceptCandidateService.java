package com.itxiop.tech.supplier.sandbox.application.service;

import com.itxiop.tech.supplier.sandbox.application.port.in.AcceptCandidateUseCase;
import com.itxiop.tech.supplier.sandbox.application.port.out.SupplierRepositoryPort;
import com.itxiop.tech.supplier.sandbox.domain.exception.DomainException;
import com.itxiop.tech.supplier.sandbox.domain.policy.CountryPolicy;
import com.itxiop.tech.supplier.sandbox.domain.value.Duns;
import com.itxiop.tech.supplier.sandbox.domain.value.SustainabilityRating;

import java.util.Objects;

public final class AcceptCandidateService implements AcceptCandidateUseCase {

  private final SupplierRepositoryPort repo;
  private final CountryPolicy countryPolicy;

  public AcceptCandidateService(SupplierRepositoryPort repo, CountryPolicy countryPolicy) {
    this.repo = Objects.requireNonNull(repo);
    this.countryPolicy = Objects.requireNonNull(countryPolicy);
  }

  @Override
  public void accept(Command cmd) {
    var supplier = repo.findByDuns(new Duns(cmd.duns()))
        .orElseThrow(() -> new DomainException("NOT_FOUND", "Candidate not found"));

    try {
      supplier.accept(SustainabilityRating.valueOf(cmd.sustainabilityRating()), countryPolicy);
    } catch (DomainException ex) {
      if ("INVALID_TRANSITION".equals(ex.code())) {
        throw new DomainException("CONFLICT", "Candidate can not be accepted");
      }
      throw ex;
    }

    repo.save(supplier);
  }
}