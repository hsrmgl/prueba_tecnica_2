package com.itxiop.tech.supplier.sandbox.application.port.in;

public interface GetSupplierUseCase {
  record Result(String name, int duns, String country, long annualTurnover, String status, String sustainabilityRating) {}
  Result get(int duns);
}