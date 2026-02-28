package com.itxiop.tech.supplier.sandbox.domain.value;

public record Money(long amount) {
  public Money {
    if (amount < 0) throw new IllegalArgumentException("money must be >= 0");
  }

  public static Money eur(long amount) {
    return new Money(amount);
  }

  public boolean lessThan(Money other) {
    return this.amount < other.amount;
  }
}