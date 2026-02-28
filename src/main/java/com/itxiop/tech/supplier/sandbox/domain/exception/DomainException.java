package com.itxiop.tech.supplier.sandbox.domain.exception;

public class DomainException extends RuntimeException {
  private final String code;

  public DomainException(String code, String message) {
    super(message);
    this.code = code;
  }

  public String code() { return code; }
}