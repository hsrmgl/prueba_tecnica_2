package com.itxiop.tech.supplier.sandbox.domain.model;

public enum SupplierStatus {
  CANDIDATE,
  DECLINED,
  ACTIVE,
  ON_PROBATION,
  DISQUALIFIED;

  public boolean apiShowsActive() {
    return this == ACTIVE || this == ON_PROBATION;
  }
}