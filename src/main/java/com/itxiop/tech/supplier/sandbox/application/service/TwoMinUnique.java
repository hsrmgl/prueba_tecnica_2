package com.itxiop.tech.supplier.sandbox.application.service;

final class TwoMinUnique {
  private Long min1 = null;
  private Long min2 = null;

  void accept(long v) {
    if (min1 == null) { min1 = v; return; }
    if (v == min1) return;

    if (v < min1) { min2 = min1; min1 = v; return; }

    if (min2 == null) { min2 = v; return; }
    if (v == min2) return;

    if (v < min2) { min2 = v; }
  }

  boolean isBonus(long v) {
    return (min1 != null && v == min1) || (min2 != null && v == min2);
  }
}