package com.itxiop.tech.supplier.sandbox.adapters.in.rest;

import com.itxiop.tech.supplier.sandbox.adapters.in.rest.dto.ErrorDto;
import com.itxiop.tech.supplier.sandbox.domain.exception.DomainException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(DomainException.class)
  public ResponseEntity<ErrorDto> handle(DomainException ex) {
    int status = switch (ex.code()) {
      case "NOT_FOUND" -> 404;
      case "COUNTRY_NOT_APPROVED", "TURNOVER_TOO_LOW", "BAD_REQUEST" -> 400;
      case "UNPROCESSABLE" -> 422;
      default -> 409;
    };
    return ResponseEntity.status(status).body(new ErrorDto(ex.getMessage()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorDto> handleIllegal(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(new ErrorDto(ex.getMessage()));
  }
}