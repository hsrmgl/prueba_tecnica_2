package com.itxiop.tech.supplier.sandbox.application.service;

import com.itxiop.tech.supplier.sandbox.application.port.in.CalculatePotentialSuppliersUseCase;
import com.itxiop.tech.supplier.sandbox.application.port.out.SupplierRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CalculatePotentialSuppliersServiceTest {

    @Test
    void applies_bonus_to_two_lowest_unique_per_country_and_sorts_desc() {
        SupplierRepositoryPort repo = new SupplierRepositoryPort() {
            @Override
            public Optional<com.itxiop.tech.supplier.sandbox.domain.model.Supplier> findByDuns(
                    com.itxiop.tech.supplier.sandbox.domain.value.Duns duns) {
                return Optional.empty();
            }

            @Override
            public void save(com.itxiop.tech.supplier.sandbox.domain.model.Supplier supplier) {
            }

            @Override
            public List<SupplierRow> findRowsForScoring() {
                return List.of(
                        new SupplierRow("S1", 111111111, "ES", 2000000, "ACTIVE", "B"),
                        new SupplierRow("S2", 222222222, "ES", 2000000, "ACTIVE", "B"),
                        new SupplierRow("S3", 333333333, "ES", 2100000, "ACTIVE", "B"),
                        new SupplierRow("S4", 444444444, "ES", 2500000, "ACTIVE", "B")
                );
            }
        };

        var svc = new CalculatePotentialSuppliersService(repo);

        var res = svc.calculate(new CalculatePotentialSuppliersUseCase.Command(250, 10, 0));

        assertEquals(4, res.total());

        // S3 (2100000, B, bonus) has highest score: 2100000*0.1*0.75*1.25 = 196875
        assertEquals(333333333, res.data().get(0).duns());
        assertTrue(res.data().stream().anyMatch(r -> r.duns() == 333333333 && r.score() > (2100000 * 0.1 * 0.75)));
        assertTrue(res.data().stream().anyMatch(r -> r.duns() == 111111111 && r.score() > (2000000 * 0.1 * 0.75)));
    }

    @Test
    void paginates_limit_offset() {
        SupplierRepositoryPort repo = new SupplierRepositoryPort() {
            @Override
            public Optional<com.itxiop.tech.supplier.sandbox.domain.model.Supplier> findByDuns(
                    com.itxiop.tech.supplier.sandbox.domain.value.Duns duns) {
                return Optional.empty();
            }

            @Override
            public void save(com.itxiop.tech.supplier.sandbox.domain.model.Supplier supplier) {
            }

            @Override
            public List<SupplierRow> findRowsForScoring() {
                return List.of(
                        new SupplierRow("S1", 111111111, "ES", 1000, "ACTIVE", "A"),
                        new SupplierRow("S2", 222222222, "ES", 2000, "ACTIVE", "A"),
                        new SupplierRow("S3", 333333333, "ES", 3000, "ACTIVE", "A")
                );
            }
        };

        var svc = new CalculatePotentialSuppliersService(repo);
        var res = svc.calculate(new CalculatePotentialSuppliersUseCase.Command(250, 2, 1));

        assertEquals(3, res.total());
        assertEquals(2, res.data().size());
    }
}