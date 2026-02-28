package com.itxiop.tech.supplier.sandbox.application.port.in;

public interface RefuseCandidateUseCase {
  record Command(int duns) {}
  void refuse(Command cmd);
}