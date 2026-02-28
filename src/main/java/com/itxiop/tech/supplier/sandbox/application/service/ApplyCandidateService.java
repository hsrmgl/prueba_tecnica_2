package com.itxiop.tech.supplier.sandbox.application.service;

import com.itxiop.tech.supplier.sandbox.application.port.in.ApplyCandidateUseCase;
import com.itxiop.tech.supplier.sandbox.application.port.out.SupplierRepositoryPort;
import com.itxiop.tech.supplier.sandbox.domain.exception.DomainException;
import com.itxiop.tech.supplier.sandbox.domain.model.Supplier;
import com.itxiop.tech.supplier.sandbox.domain.model.SupplierStatus;
import com.itxiop.tech.supplier.sandbox.domain.value.*;

import java.util.Objects;

public final class ApplyCandidateService implements ApplyCandidateUseCase {

  private final SupplierRepositoryPort repo;

  public ApplyCandidateService(SupplierRepositoryPort repo) {
    this.repo = Objects.requireNonNull(repo);
  }

  @Override
  public void apply(Command cmd) {
    var duns = new Duns(cmd.duns());
    var country = new CountryCode(cmd.country());
    var turnover = new Money(cmd.annualTurnover());

    var existingOpt = repo.findByDuns(duns);
    if (existingOpt.isEmpty()) {
      repo.save(Supplier.apply(duns, cmd.name(), country, turnover));
      return;
    }

    var existing = existingOpt.get();

    if (existing.status() == SupplierStatus.DISQUALIFIED) {
      throw new DomainException("CONFLICT", "Supplier banned");
    }

    if (existing.status() == SupplierStatus.DECLINED) {
      existing.reapply(cmd.name(), country, turnover);
      repo.save(existing);
      return;
    }

    throw new DomainException("CONFLICT", "Candidate already exists");
  }
}