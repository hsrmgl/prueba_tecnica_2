package com.itxiop.tech.supplier.sandbox.application.port.in;

public interface UpdateSustainabilityRatingUseCase {
  record Command(int duns, String sustainabilityRating) {}
  void update(Command cmd);
}