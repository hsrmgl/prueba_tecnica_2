package com.itxiop.tech.supplier.sandbox.domain.value;

public record Duns(int value) {
  public Duns {
    if (value < 100_000_000 || value > 999_999_999) {
      throw new IllegalArgumentException("duns must be a 9-digit number");
    }
  }
}