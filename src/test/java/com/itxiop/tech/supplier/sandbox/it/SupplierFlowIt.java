package com.itxiop.tech.supplier.sandbox.it;

import com.itxiop.tech.supplier.sandbox.adapters.in.rest.dto.CandidateAcceptDto;
import com.itxiop.tech.supplier.sandbox.adapters.in.rest.dto.CandidateDto;
import com.itxiop.tech.supplier.sandbox.adapters.in.rest.dto.PotentialSuppliersDto;
import com.itxiop.tech.supplier.sandbox.adapters.in.rest.dto.SupplierDto;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "itx.country.base-url=http://localhost:${local.server.port}",
                "spring.jpa.hibernate.ddl-auto=create-drop"
        }
)
class SupplierFlowIT {

    @LocalServerPort
    int port;

    private final TestRestTemplate rest = new TestRestTemplate();

    @Test
    void full_flow_candidate_to_supplier_to_potential_and_event() {
        String base = "http://localhost:" + port;

        CandidateDto candidate = new CandidateDto(2_000_000L, "ES", 123456789, "Zippers & Buttons");
        ResponseEntity<CandidateDto> created =
                rest.postForEntity(base + "/candidates", candidate, CandidateDto.class);
        assertEquals(201, created.getStatusCode().value());
        assertNotNull(created.getBody());
        assertEquals(candidate.duns(), created.getBody().duns());

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<CandidateAcceptDto> acceptReq = new HttpEntity<>(new CandidateAcceptDto("A"), h);
        ResponseEntity<Void> accepted = rest.exchange(
                base + "/candidates/123456789/accept",
                HttpMethod.POST,
                acceptReq,
                Void.class
        );
        assertEquals(204, accepted.getStatusCode().value());

        SupplierDto supplier = rest.getForObject(base + "/suppliers/123456789", SupplierDto.class);
        assertNotNull(supplier);
        assertEquals(123456789, supplier.duns());
        assertEquals("Active", supplier.status());
        assertEquals("A", supplier.sustainabilityRating());

        PotentialSuppliersDto potential = rest.getForObject(
                base + "/suppliers/potential?rate=250&limit=10&offset=0",
                PotentialSuppliersDto.class
        );
        assertNotNull(potential);
        assertNotNull(potential.pagination());
        assertTrue(potential.pagination().total() >= 1);
        assertNotNull(potential.data());
        assertFalse(potential.data().isEmpty());

        String json = """
                {
                  "duns": 123456789,
                  "score": "C"
                }
                """;
        HttpEntity<String> evtReq = new HttpEntity<>(json, h);
        ResponseEntity<Void> evt = rest.exchange(
                base + "/sustainability/update",
                HttpMethod.POST,
                evtReq,
                Void.class
        );
        assertTrue(evt.getStatusCode().is2xxSuccessful());

        SupplierDto after = rest.getForObject(base + "/suppliers/123456789", SupplierDto.class);
        assertNotNull(after);
        assertEquals("C", after.sustainabilityRating());
        assertEquals("Active", after.status());
    }
}