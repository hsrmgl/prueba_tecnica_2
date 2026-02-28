package com.itxiop.tech.supplier.sandbox.adapters.in.rest.dto;

public record SupplierDto(
    long annualTurnover,
    String country,
    int duns,
    String name,
    String status,
    String sustainabilityRating
) {}