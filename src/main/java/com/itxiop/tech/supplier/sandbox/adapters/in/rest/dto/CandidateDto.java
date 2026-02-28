package com.itxiop.tech.supplier.sandbox.adapters.in.rest.dto;

public record CandidateDto(
    long annualTurnover,
    String country,
    int duns,
    String name
) {}