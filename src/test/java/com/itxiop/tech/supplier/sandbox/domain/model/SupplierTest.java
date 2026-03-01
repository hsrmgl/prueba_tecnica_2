package com.itxiop.tech.supplier.sandbox.domain.model;

import com.itxiop.tech.supplier.sandbox.domain.exception.DomainException;
import com.itxiop.tech.supplier.sandbox.domain.policy.CountryPolicy;
import com.itxiop.tech.supplier.sandbox.domain.value.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SupplierTest {

    private static final CountryPolicy APPROVED = c -> true;
    private static final CountryPolicy REJECTED = c -> false;

    @Test
    void accept_candidate_with_good_rating_becomes_active() {
        Supplier s = Supplier.apply(new Duns(123456789), "A", new CountryCode("ES"), new Money(2_000_000));

        s.accept(SustainabilityRating.A, APPROVED);

        assertEquals(SupplierStatus.ACTIVE, s.status());
        assertEquals(SustainabilityRating.A, s.rating());
    }

    @Test
    void accept_candidate_with_bad_rating_becomes_on_probation() {
        Supplier s = Supplier.apply(new Duns(123456789), "A", new CountryCode("ES"), new Money(2_000_000));

        s.accept(SustainabilityRating.C, APPROVED);

        assertEquals(SupplierStatus.ON_PROBATION, s.status());
        assertEquals(SustainabilityRating.C, s.rating());
    }

    @Test
    void accept_fails_if_country_not_approved() {
        Supplier s = Supplier.apply(new Duns(123456789), "A", new CountryCode("ES"), new Money(2_000_000));

        DomainException ex = assertThrows(DomainException.class,
                () -> s.accept(SustainabilityRating.A, REJECTED));

        assertEquals("COUNTRY_NOT_APPROVED", ex.code());
    }

    @Test
    void accept_fails_if_turnover_too_low() {
        Supplier s = Supplier.apply(new Duns(123456789), "A", new CountryCode("ES"), new Money(999_999));

        DomainException ex = assertThrows(DomainException.class,
                () -> s.accept(SustainabilityRating.A, APPROVED));

        assertEquals("TURNOVER_TOO_LOW", ex.code());
    }

    @Test
    void refuse_only_allowed_from_candidate() {
        Supplier s = Supplier.apply(new Duns(123456789), "A", new CountryCode("ES"), new Money(2_000_000));
        s.accept(SustainabilityRating.A, APPROVED);

        assertThrows(DomainException.class, s::refuse);
    }

    @Test
    void ban_only_allowed_from_on_probation() {
        Supplier s = Supplier.apply(new Duns(123456789), "A", new CountryCode("ES"), new Money(2_000_000));
        s.accept(SustainabilityRating.C, APPROVED);

        s.ban();
        assertEquals(SupplierStatus.DISQUALIFIED, s.status());
    }

    @Test
    void rating_update_recalculates_status_except_disqualified() {
        Supplier s = Supplier.apply(new Duns(123456789), "A", new CountryCode("ES"), new Money(2_000_000));
        s.accept(SustainabilityRating.C, APPROVED);

        s.updateRating(SustainabilityRating.A);
        assertEquals(SupplierStatus.ACTIVE, s.status());

        s.updateRating(SustainabilityRating.C);
        assertEquals(SupplierStatus.ON_PROBATION, s.status());

        s.ban();
        assertEquals(SupplierStatus.DISQUALIFIED, s.status());

        s.updateRating(SustainabilityRating.A);
        assertEquals(SupplierStatus.DISQUALIFIED, s.status(), "disqualified should remain forever");
    }
}