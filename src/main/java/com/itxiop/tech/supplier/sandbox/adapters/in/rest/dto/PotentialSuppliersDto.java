package com.itxiop.tech.supplier.sandbox.adapters.in.rest.dto;

import java.util.List;

public record PotentialSuppliersDto(
    List<PotentialSupplierDto> data,
    PaginationDto pagination
) {}