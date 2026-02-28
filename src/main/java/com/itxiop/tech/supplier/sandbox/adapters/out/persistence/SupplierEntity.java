package com.itxiop.tech.supplier.sandbox.adapters.out.persistence;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "suppliers")
@Getter @Setter
public class SupplierEntity {

  @Id
  private Integer duns;

  private String name;
  private String country;
  private Long annualTurnover;

  private String status;
  private String rating;
}