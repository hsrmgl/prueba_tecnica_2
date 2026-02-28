package com.itxiop.tech.supplier.sandbox.application.service;

import com.itxiop.tech.supplier.sandbox.application.port.in.BanSupplierUseCase;
import com.itxiop.tech.supplier.sandbox.application.port.out.SupplierRepositoryPort;
import com.itxiop.tech.supplier.sandbox.domain.exception.DomainException;
import com.itxiop.tech.supplier.sandbox.domain.model.SupplierStatus;
import com.itxiop.tech.supplier.sandbox.domain.value.Duns;

import java.util.Objects;

public final class BanSupplierService implements BanSupplierUseCase {

  private final SupplierRepositoryPort repo;

  public BanSupplierService(SupplierRepositoryPort repo) {
    this.repo = Objects.requireNonNull(repo);
  }

  @Override
  public void ban(Command cmd) {
    var s = repo.findByDuns(new Duns(cmd.duns()))
        .orElseThrow(() -> new DomainException("NOT_FOUND", "Supplier not found"));

    if (s.status() != SupplierStatus.ON_PROBATION) {
      throw new DomainException("CONFLICT", "Supplier can not be banned");
    }

    s.ban();
    repo.save(s);
  }
}