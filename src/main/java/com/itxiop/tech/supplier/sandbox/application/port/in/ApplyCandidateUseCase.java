package com.itxiop.tech.supplier.sandbox.application.port.in;

public interface ApplyCandidateUseCase {
  record Command(String name, int duns, String country, long annualTurnover) {}
  void apply(Command cmd);
}