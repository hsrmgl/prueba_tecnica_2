package com.itxiop.tech.supplier.sandbox.adapters.out.persistence;

import com.itxiop.tech.supplier.sandbox.application.port.out.SupplierRepositoryPort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SupplierJpaRepository extends JpaRepository<SupplierEntity, Integer> {

  @Query("""
    select new com.itxiop.tech.supplier.sandbox.application.port.out.SupplierRepositoryPort$SupplierRow(
      s.name, s.duns, s.country, s.annualTurnover, s.status, s.rating
    )
    from SupplierEntity s
  """)
  List<SupplierRepositoryPort.SupplierRow> findRowsForScoring();
}