package com.itxiop.tech.supplier.sandbox.adapters.in.rest.dto;

public record PotentialSupplierDto(
    long annualTurnover,
    String country,
    int duns,
    String name,
    String status,
    String sustainabilityRating,
    double score
) {}