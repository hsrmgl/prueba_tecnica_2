package com.itxiop.tech.supplier.sandbox.application.port.in;

public interface GetCandidateUseCase {
  record Result(String name, int duns, String country, long annualTurnover) {}
  Result get(int duns);
}