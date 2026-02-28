package com.itxiop.tech.supplier.sandbox.domain.value;

import java.util.Objects;

public record CountryCode(String value) {
  public CountryCode {
    Objects.requireNonNull(value, "country");
    if (!value.matches("^[A-Z]{2}$")) throw new IllegalArgumentException("country must be ISO alpha-2");
  }
}