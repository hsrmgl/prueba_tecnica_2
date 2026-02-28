package com.itxiop.tech.supplier.sandbox.application.service;

import com.itxiop.tech.supplier.sandbox.application.port.in.CalculatePotentialSuppliersUseCase;
import com.itxiop.tech.supplier.sandbox.application.port.out.SupplierRepositoryPort;
import com.itxiop.tech.supplier.sandbox.domain.model.SupplierStatus;
import com.itxiop.tech.supplier.sandbox.domain.value.SustainabilityRating;

import java.util.*;

public final class CalculatePotentialSuppliersService implements CalculatePotentialSuppliersUseCase {

  private final SupplierRepositoryPort repo;

  public CalculatePotentialSuppliersService(SupplierRepositoryPort repo) {
    this.repo = Objects.requireNonNull(repo);
  }

  @Override
  public Result calculate(Command cmd) {
    if (cmd.rate() < 250) {
      throw new IllegalArgumentException("rate must be >= 250");
    }
    if (cmd.limit() < 1 || cmd.limit() > 10) {
      throw new IllegalArgumentException("limit must be between 1 and 10");
    }
    if (cmd.offset() < 0) {
      throw new IllegalArgumentException("offset must be >= 0");
    }

    var rows = repo.findRowsForScoring();

    Map<String, TwoMinUnique> minsByCountry = new HashMap<>();
    for (var r : rows) {
      SupplierStatus st = SupplierStatus.valueOf(r.status());
      if (st == SupplierStatus.DISQUALIFIED) continue;
      if (st == SupplierStatus.CANDIDATE || st == SupplierStatus.DECLINED) continue;

      minsByCountry.computeIfAbsent(r.country(), c -> new TwoMinUnique())
          .accept(r.annualTurnover());
    }

    List<Row> scored = new ArrayList<>();
    for (var r : rows) {
      SupplierStatus st = SupplierStatus.valueOf(r.status());
      if (st == SupplierStatus.DISQUALIFIED) continue;
      if (st == SupplierStatus.CANDIDATE || st == SupplierStatus.DECLINED) continue;

      if (!(r.annualTurnover() > cmd.rate())) continue;

      SustainabilityRating rating = SustainabilityRating.valueOf(r.rating());
      double score = r.annualTurnover() * 0.1 * rating.constant();

      TwoMinUnique mins = minsByCountry.get(r.country());
      if (mins != null && mins.isBonus(r.annualTurnover())) {
        score *= 1.25;
      }

      String apiStatus = st.apiShowsActive() ? "Active" : "Disqualified";

      scored.add(new Row(
          r.name(),
          r.duns(),
          r.country(),
          r.annualTurnover(),
          apiStatus,
          rating.name(),
          score
      ));
    }

    scored.sort(Comparator.comparingDouble(Row::score).reversed());

    int total = scored.size();
    int from = Math.min(cmd.offset(), total);
    int to = Math.min(from + cmd.limit(), total);

    return new Result(cmd.limit(), cmd.offset(), total, scored.subList(from, to));
  }
}