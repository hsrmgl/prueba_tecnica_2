package com.itxiop.tech.supplier.sandbox.application.port.in;

import java.util.List;

public interface CalculatePotentialSuppliersUseCase {

  record Command(long rate, int limit, int offset) {}

  record Row(
      String name,
      int duns,
      String country,
      long annualTurnover,
      String status,          
      String sustainabilityRating, 
      double score
  ) {}

  record Result(int limit, int offset, int total, List<Row> data) {}

  Result calculate(Command cmd);
}