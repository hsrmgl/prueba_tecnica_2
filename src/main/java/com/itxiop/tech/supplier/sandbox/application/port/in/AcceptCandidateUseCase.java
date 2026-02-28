package com.itxiop.tech.supplier.sandbox.application.port.in;

public interface AcceptCandidateUseCase {
  record Command(int duns, String sustainabilityRating) {}
  void accept(Command cmd);
}