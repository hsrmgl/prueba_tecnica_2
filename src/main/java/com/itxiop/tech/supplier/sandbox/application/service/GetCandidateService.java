package com.itxiop.tech.supplier.sandbox.application.service;

import com.itxiop.tech.supplier.sandbox.application.port.in.GetCandidateUseCase;
import com.itxiop.tech.supplier.sandbox.application.port.out.SupplierRepositoryPort;
import com.itxiop.tech.supplier.sandbox.domain.exception.DomainException;
import com.itxiop.tech.supplier.sandbox.domain.model.SupplierStatus;
import com.itxiop.tech.supplier.sandbox.domain.value.Duns;

import java.util.Objects;

public final class GetCandidateService implements GetCandidateUseCase {

  private final SupplierRepositoryPort repo;

  public GetCandidateService(SupplierRepositoryPort repo) {
    this.repo = Objects.requireNonNull(repo);
  }

  @Override
  public Result get(int duns) {
    var s = repo.findByDuns(new Duns(duns))
        .orElseThrow(() -> new DomainException("NOT_FOUND", "Candidate not found"));

    if (s.status() != SupplierStatus.CANDIDATE && s.status() != SupplierStatus.DECLINED) {
      throw new DomainException("NOT_FOUND", "Candidate not found");
    }

    return new Result(s.name(), s.duns().value(), s.country().value(), s.annualTurnover().amount());
  }
}