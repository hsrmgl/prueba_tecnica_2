package com.itxiop.tech.supplier.sandbox.adapters.out.persistence;

import com.itxiop.tech.supplier.sandbox.domain.model.*;
import com.itxiop.tech.supplier.sandbox.domain.value.*;

final class SupplierMapper {

  static SupplierEntity toEntity(Supplier s) {
    SupplierEntity e = new SupplierEntity();
    e.setDuns(s.duns().value());
    e.setName(s.name());
    e.setCountry(s.country().value());
    e.setAnnualTurnover(s.annualTurnover().amount());
    e.setStatus(s.status().name());
    e.setRating(s.rating() == null ? null : s.rating().name());
    return e;
  }

  static Supplier toDomain(SupplierEntity e) {
    return Supplier.rehydrate(
        new Duns(e.getDuns()),
        e.getName(),
        new CountryCode(e.getCountry()),
        new Money(e.getAnnualTurnover()),
        SupplierStatus.valueOf(e.getStatus()),
        e.getRating() == null ? null : SustainabilityRating.valueOf(e.getRating())
    );
  }
}