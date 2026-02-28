package com.itxiop.tech.supplier.sandbox.adapters.in.rest;

import com.itxiop.tech.supplier.sandbox.adapters.in.rest.dto.CandidateAcceptDto;
import com.itxiop.tech.supplier.sandbox.adapters.in.rest.dto.CandidateDto;
import com.itxiop.tech.supplier.sandbox.application.port.in.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CandidateController {

  private final ApplyCandidateUseCase applyCandidate;
  private final GetCandidateUseCase getCandidate;
  private final AcceptCandidateUseCase acceptCandidate;
  private final RefuseCandidateUseCase refuseCandidate;

  @PostMapping("/candidates")
  public ResponseEntity<CandidateDto> addCandidate(@RequestBody CandidateDto req) {
    applyCandidate.apply(new ApplyCandidateUseCase.Command(
        req.name(), req.duns(), req.country(), req.annualTurnover()
    ));
    return ResponseEntity.status(201).body(req);
  }

  @GetMapping("/candidates/{duns}")
  public CandidateDto getCandidate(@PathVariable int duns) {
    var c = getCandidate.get(duns);
    return new CandidateDto(c.annualTurnover(), c.country(), c.duns(), c.name());
  }

  @PostMapping("/candidates/{duns}/accept")
  public ResponseEntity<Void> accept(@PathVariable int duns, @RequestBody CandidateAcceptDto body) {
    acceptCandidate.accept(new AcceptCandidateUseCase.Command(duns, body.sustainabilityRating()));
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/candidates/{duns}/refuse")
  public ResponseEntity<Void> refuse(@PathVariable int duns) {
    refuseCandidate.refuse(new RefuseCandidateUseCase.Command(duns));
    return ResponseEntity.noContent().build();
  }
}