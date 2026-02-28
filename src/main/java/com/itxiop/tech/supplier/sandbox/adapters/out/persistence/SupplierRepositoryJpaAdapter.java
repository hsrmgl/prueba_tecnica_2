package com.itxiop.tech.supplier.sandbox.adapters.out.persistence;

import com.itxiop.tech.supplier.sandbox.application.port.out.SupplierRepositoryPort;
import com.itxiop.tech.supplier.sandbox.domain.model.Supplier;
import com.itxiop.tech.supplier.sandbox.domain.value.Duns;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class SupplierRepositoryJpaAdapter implements SupplierRepositoryPort {

  private final SupplierJpaRepository jpa;

  public SupplierRepositoryJpaAdapter(SupplierJpaRepository jpa) {
    this.jpa = jpa;
  }

  @Override
  public Optional<Supplier> findByDuns(Duns duns) {
    return jpa.findById(duns.value()).map(SupplierMapper::toDomain);
  }

  @Override
  public void save(Supplier supplier) {
    jpa.save(SupplierMapper.toEntity(supplier));
  }

  @Override
  public List<SupplierRow> findRowsForScoring() {
    return jpa.findRowsForScoring();
  }
}