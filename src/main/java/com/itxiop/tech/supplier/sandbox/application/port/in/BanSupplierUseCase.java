package com.itxiop.tech.supplier.sandbox.application.port.in;

public interface BanSupplierUseCase {
  record Command(int duns) {}
  void ban(Command cmd);
}