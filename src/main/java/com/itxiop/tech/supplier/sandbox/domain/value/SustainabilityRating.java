package com.itxiop.tech.supplier.sandbox.domain.value;

public enum SustainabilityRating {
  A(1.0, true),
  B(0.75, true),
  C(0.5, false),
  D(0.25, false),
  E(0.1, false);

  private final double constant;
  private final boolean good;

  SustainabilityRating(double constant, boolean good) {
    this.constant = constant;
    this.good = good;
  }

  public double constant() { return constant; }
  public boolean isGood() { return good; }
}