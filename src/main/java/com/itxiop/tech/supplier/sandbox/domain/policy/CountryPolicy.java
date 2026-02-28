package com.itxiop.tech.supplier.sandbox.domain.policy;

import com.itxiop.tech.supplier.sandbox.domain.value.CountryCode;

public interface CountryPolicy {
  boolean isApproved(CountryCode country);
}