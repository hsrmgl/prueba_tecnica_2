package com.itxiop.tech.supplier.sandbox.application.port.out;

import com.itxiop.tech.supplier.sandbox.domain.model.Supplier;
import com.itxiop.tech.supplier.sandbox.domain.value.Duns;

import java.util.List;
import java.util.Optional;

public interface SupplierRepositoryPort {

  Optional<Supplier> findByDuns(Duns duns);
  void save(Supplier supplier);

  List<SupplierRow> findRowsForScoring();

  record SupplierRow(
      String name,
      int duns,
      String country,
      long annualTurnover,
      String status,
      String rating
  ) {}
}