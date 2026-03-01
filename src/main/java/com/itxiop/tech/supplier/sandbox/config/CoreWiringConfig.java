package com.itxiop.tech.supplier.sandbox.config;

import com.itxiop.tech.supplier.sandbox.application.port.in.*;
import com.itxiop.tech.supplier.sandbox.application.port.out.SupplierRepositoryPort;
import com.itxiop.tech.supplier.sandbox.application.service.*;
import com.itxiop.tech.supplier.sandbox.domain.policy.CountryPolicy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class CoreWiringConfig {

  @Bean(name = "applyCandidateCore")
  public ApplyCandidateUseCase applyCandidateCore(SupplierRepositoryPort repo) {
    return new ApplyCandidateService(repo);
  }

  @Bean(name = "acceptCandidateCore")
  public AcceptCandidateUseCase acceptCandidateCore(SupplierRepositoryPort repo, @Lazy CountryPolicy policy) {
    return new AcceptCandidateService(repo, policy);
  }

  @Bean(name = "refuseCandidateCore")
  public RefuseCandidateUseCase refuseCandidateCore(SupplierRepositoryPort repo) {
    return new RefuseCandidateService(repo);
  }

  @Bean(name = "banSupplierCore")
  public BanSupplierUseCase banSupplierCore(SupplierRepositoryPort repo) {
    return new BanSupplierService(repo);
  }

  @Bean(name = "getCandidateCore")
  public GetCandidateUseCase getCandidateCore(SupplierRepositoryPort repo) {
    return new GetCandidateService(repo);
  }

  @Bean(name = "getSupplierCore")
  public GetSupplierUseCase getSupplierCore(SupplierRepositoryPort repo) {
    return new GetSupplierService(repo);
  }

  @Bean(name = "potentialSuppliersCore")
  public CalculatePotentialSuppliersUseCase potentialSuppliersCore(SupplierRepositoryPort repo) {
    return new CalculatePotentialSuppliersService(repo);
  }

  @Bean(name = "updateRatingCore")
  public UpdateSustainabilityRatingUseCase updateRatingCore(SupplierRepositoryPort repo) {
    return new UpdateSustainabilityRatingService(repo);
  }

}