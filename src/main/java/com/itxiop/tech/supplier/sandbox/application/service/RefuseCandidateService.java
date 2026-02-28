package com.itxiop.tech.supplier.sandbox.application.service;

import com.itxiop.tech.supplier.sandbox.application.port.in.RefuseCandidateUseCase;
import com.itxiop.tech.supplier.sandbox.application.port.out.SupplierRepositoryPort;
import com.itxiop.tech.supplier.sandbox.domain.exception.DomainException;
import com.itxiop.tech.supplier.sandbox.domain.model.SupplierStatus;
import com.itxiop.tech.supplier.sandbox.domain.value.Duns;

import java.util.Objects;

public final class RefuseCandidateService implements RefuseCandidateUseCase {

  private final SupplierRepositoryPort repo;

  public RefuseCandidateService(SupplierRepositoryPort repo) {
    this.repo = Objects.requireNonNull(repo);
  }

  @Override
  public void refuse(Command cmd) {
    var s = repo.findByDuns(new Duns(cmd.duns()))
        .orElseThrow(() -> new DomainException("NOT_FOUND", "Candidate not found"));

    if (s.status() != SupplierStatus.CANDIDATE) {
      throw new DomainException("CONFLICT", "Candidate can not be refused");
    }

    s.refuse();
    repo.save(s);
  }
}