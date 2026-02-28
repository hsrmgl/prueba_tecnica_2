package com.itxiop.tech.supplier.sandbox.domain.model;

import com.itxiop.tech.supplier.sandbox.domain.exception.DomainException;
import com.itxiop.tech.supplier.sandbox.domain.policy.CountryPolicy;
import com.itxiop.tech.supplier.sandbox.domain.value.*;

import java.util.Objects;

public final class Supplier {

  private final Duns duns;
  private String name;
  private CountryCode country;
  private Money annualTurnover;

  private SupplierStatus status;
  private SustainabilityRating rating;

  private Supplier(Duns duns, String name, CountryCode country, Money annualTurnover,
                   SupplierStatus status, SustainabilityRating rating) {
    this.duns = Objects.requireNonNull(duns);
    this.name = requireNonBlank(name, "name");
    this.country = Objects.requireNonNull(country);
    this.annualTurnover = Objects.requireNonNull(annualTurnover);
    this.status = Objects.requireNonNull(status);
    this.rating = rating;
  }

  public static Supplier apply(Duns duns, String name, CountryCode country, Money annualTurnover) {
    return new Supplier(duns, name, country, annualTurnover, SupplierStatus.CANDIDATE, null);
  }

  static Supplier rehydrate(Duns duns, String name, CountryCode country, Money annualTurnover,
                            SupplierStatus status, SustainabilityRating rating) {
    if ((status == SupplierStatus.CANDIDATE || status == SupplierStatus.DECLINED) && rating != null) {
      throw new DomainException("INVALID_STATE", "Candidate/declined cannot have rating");
    }
    if ((status == SupplierStatus.ACTIVE || status == SupplierStatus.ON_PROBATION || status == SupplierStatus.DISQUALIFIED)
        && rating == null) {
      throw new DomainException("INVALID_STATE", "Supplier must have rating");
    }
    return new Supplier(duns, name, country, annualTurnover, status, rating);
  }

  public void reapply(String newName, CountryCode newCountry, Money newTurnover) {
    ensureStatus(SupplierStatus.DECLINED);
    this.name = requireNonBlank(newName, "name");
    this.country = Objects.requireNonNull(newCountry);
    this.annualTurnover = Objects.requireNonNull(newTurnover);
    this.status = SupplierStatus.CANDIDATE;
    this.rating = null;
  }

  public void refuse() {
    ensureStatus(SupplierStatus.CANDIDATE);
    this.status = SupplierStatus.DECLINED;
  }

  public void accept(SustainabilityRating initialRating, CountryPolicy countryPolicy) {
    ensureStatus(SupplierStatus.CANDIDATE);

    if (!countryPolicy.isApproved(country)) {
      throw new DomainException("COUNTRY_NOT_APPROVED", "Country is not approved");
    }
    if (annualTurnover.lessThan(Money.eur(1_000_000L))) {
      throw new DomainException("TURNOVER_TOO_LOW", "Annual turnover must be at least 1,000,000 EUR");
    }

    this.rating = Objects.requireNonNull(initialRating);
    this.status = initialRating.isGood() ? SupplierStatus.ACTIVE : SupplierStatus.ON_PROBATION;
  }

  public void ban() {
    ensureStatus(SupplierStatus.ON_PROBATION);
    this.status = SupplierStatus.DISQUALIFIED;
  }

  public void updateRating(SustainabilityRating newRating) {
    if (status == SupplierStatus.CANDIDATE || status == SupplierStatus.DECLINED) {
      throw new DomainException("NOT_A_SUPPLIER", "Cannot update rating for non supplier");
    }
    this.rating = Objects.requireNonNull(newRating);
    if (status != SupplierStatus.DISQUALIFIED) {
      this.status = newRating.isGood() ? SupplierStatus.ACTIVE : SupplierStatus.ON_PROBATION;
    }
  }

  private void ensureStatus(SupplierStatus expected) {
    if (this.status != expected) {
      throw new DomainException("INVALID_TRANSITION", "Invalid transition from " + status);
    }
  }

  private static String requireNonBlank(String s, String field) {
    if (s == null || s.isBlank()) throw new IllegalArgumentException(field + " blank");
    return s;
  }

  public Duns duns() { return duns; }
  public String name() { return name; }
  public CountryCode country() { return country; }
  public Money annualTurnover() { return annualTurnover; }
  public SupplierStatus status() { return status; }
  public SustainabilityRating rating() { return rating; }
}