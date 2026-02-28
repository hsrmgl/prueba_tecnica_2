# Arquitectura del Proyecto — Gestión de Proveedores Inditex

## Índice

1. [Visión General](#visión-general)
2. [Stack Tecnológico](#stack-tecnológico)
3. [Arquitectura Hexagonal (Puertos y Adaptadores)](#arquitectura-hexagonal-puertos-y-adaptadores)
4. [Estructura de Paquetes](#estructura-de-paquetes)
5. [Capa de Dominio](#capa-de-dominio)
6. [Capa de Aplicación](#capa-de-aplicación)
7. [Capa de Adaptadores](#capa-de-adaptadores)
8. [Configuración e Inyección de Dependencias](#configuración-e-inyección-de-dependencias)
9. [Ciclo de Vida del Proveedor](#ciclo-de-vida-del-proveedor)
10. [Reglas de Negocio](#reglas-de-negocio)
11. [Proveedores Potenciales](#proveedores-potenciales)
12. [Manejo de Errores](#manejo-de-errores)
13. [Persistencia](#persistencia)
14. [Eventos](#eventos)
15. [Consideraciones de Rendimiento y Escalabilidad](#consideraciones-de-rendimiento-y-escalabilidad)
16. [Diagrama de Flujo del Proveedor](#diagrama-de-flujo-del-proveedor)

---

## Visión General

Este proyecto implementa un servicio de gestión de proveedores para Inditex siguiendo los principios de **Domain-Driven Design (DDD)** y una **Arquitectura Hexagonal** (Puertos y Adaptadores). El servicio gestiona el ciclo de vida completo de los proveedores: desde la solicitud como candidato, pasando por su aceptación o rechazo, hasta su posible descalificación.

La solución se implementa sobre **Spring Boot 3.2.5** con **Java 21**, utilizando una base de datos en memoria **H2** como persistencia.

---

## Stack Tecnológico

| Tecnología | Versión | Propósito |
|---|---|---|
| Java | 21 | Lenguaje de programación |
| Spring Boot | 3.2.5 | Framework base |
| Spring Web | — | API REST |
| Spring Data JPA | — | Persistencia ORM |
| H2 Database | — | Base de datos en memoria |
| Lombok | — | Reducción de código boilerplate |
| Maven | — | Gestión de dependencias y build |

---

## Arquitectura Hexagonal (Puertos y Adaptadores)

El proyecto sigue la arquitectura hexagonal, que separa el núcleo de negocio de la infraestructura. Esta separación garantiza que la lógica de dominio sea independiente de frameworks, bases de datos o servicios externos.

```
                    ┌─────────────────────────────────┐
                    │       Adaptadores de Entrada     │
                    │  (REST Controllers, Listeners)   │
                    └──────────────┬──────────────────┘
                                   │
                    ┌──────────────▼──────────────────┐
                    │     Puertos de Entrada (in)      │
                    │        (Use Cases)               │
                    └──────────────┬──────────────────┘
                                   │
                    ┌──────────────▼──────────────────┐
                    │     Capa de Aplicación           │
                    │       (Servicios)                │
                    └──────────────┬──────────────────┘
                                   │
                    ┌──────────────▼──────────────────┐
                    │     Capa de Dominio              │
                    │  (Modelos, Value Objects,        │
                    │   Políticas, Excepciones)        │
                    └──────────────┬──────────────────┘
                                   │
                    ┌──────────────▼──────────────────┐
                    │     Puertos de Salida (out)      │
                    │   (SupplierRepositoryPort,       │
                    │    CountryPolicy)                │
                    └──────────────┬──────────────────┘
                                   │
                    ┌──────────────▼──────────────────┐
                    │     Adaptadores de Salida        │
                    │  (JPA, REST Client externo)      │
                    └─────────────────────────────────┘
```

### Principios clave

- **Independencia del dominio**: La capa de dominio no depende de ningún framework o librería externa.
- **Inversión de dependencias**: Las capas externas dependen de las internas, nunca al revés.
- **Contratos mediante interfaces (puertos)**: Los adaptadores implementan los puertos definidos por la capa de aplicación.

---

## Estructura de Paquetes

```
com.itxiop.tech.supplier
├── Application.java                          # Punto de entrada Spring Boot
├── country/                                  # Servicio externo de países (proporcionado)
│   ├── Country.java
│   └── CountryController.java
├── sustainability/                           # Eventos de sostenibilidad (proporcionado)
│   ├── SustainabilityRating.java
│   ├── SustainabilityRatingController.java
│   └── SustainabilityRatingEvent.java
└── sandbox/                                  # Implementación de la solución
    ├── domain/                               # Capa de Dominio
    │   ├── model/
    │   │   ├── Supplier.java                 # Raíz del agregado
    │   │   └── SupplierStatus.java           # Estados del proveedor
    │   ├── value/
    │   │   ├── CountryCode.java              # Código de país ISO alpha-2
    │   │   ├── Duns.java                     # Número DUNS (9 dígitos)
    │   │   ├── Money.java                    # Valor monetario
    │   │   └── SustainabilityRating.java     # Calificación A-E
    │   ├── policy/
    │   │   └── CountryPolicy.java            # Interfaz de política de país
    │   └── exception/
    │       └── DomainException.java          # Excepciones de dominio
    ├── application/                          # Capa de Aplicación
    │   ├── port/
    │   │   ├── in/                           # Puertos de entrada (casos de uso)
    │   │   │   ├── AcceptCandidateUseCase.java
    │   │   │   ├── ApplyCandidateUseCase.java
    │   │   │   ├── BanSupplierUseCase.java
    │   │   │   ├── CalculatePotentialSuppliersUseCase.java
    │   │   │   ├── GetCandidateUseCase.java
    │   │   │   ├── GetSupplierUseCase.java
    │   │   │   ├── RefuseCandidateUseCase.java
    │   │   │   └── UpdateSustainabilityRatingUseCase.java
    │   │   └── out/                          # Puertos de salida
    │   │       └── SupplierRepositoryPort.java
    │   └── service/                          # Implementación de servicios
    │       ├── AcceptCandidateService.java
    │       ├── ApplyCandidateService.java
    │       ├── BanSupplierService.java
    │       ├── CalculatePotentialSuppliersService.java
    │       ├── GetCandidateService.java
    │       ├── GetSupplierService.java
    │       ├── RefuseCandidateService.java
    │       ├── TwoMinUnique.java
    │       └── UpdateSustainabilityRatingService.java
    ├── adapters/                             # Capa de Adaptadores
    │   ├── in/                               # Adaptadores de entrada
    │   │   ├── rest/
    │   │   │   ├── CandidateController.java
    │   │   │   ├── SupplierController.java
    │   │   │   ├── ApiExceptionHandler.java
    │   │   │   └── dto/                      # Objetos de transferencia
    │   │   │       ├── CandidateDto.java
    │   │   │       ├── CandidateAcceptDto.java
    │   │   │       ├── SupplierDto.java
    │   │   │       ├── PotentialSupplierDto.java
    │   │   │       ├── PotentialSuppliersDto.java
    │   │   │       ├── PaginationDto.java
    │   │   │       └── ErrorDto.java
    │   │   └── event/
    │   │       └── SustainabilityRatingEventListener.java
    │   └── out/                              # Adaptadores de salida
    │       ├── persistence/
    │       │   ├── SupplierEntity.java
    │       │   ├── SupplierJpaRepository.java
    │       │   ├── SupplierMapper.java
    │       │   └── SupplierRepositoryJpaAdapter.java
    │       └── country/
    │           └── CountryPolicyRestClient.java
    └── config/                               # Configuración y cableado
        ├── CoreWiringConfig.java
        ├── AcceptCandidateTxDecorator.java
        ├── ApplyCandidateTxDecorator.java
        ├── BanSupplierTxDecorator.java
        ├── PotentialSuppliersTxDecorator.java
        ├── RefuseCandidateTxDecorator.java
        └── UpdateRatingTxDecorator.java
```

---

## Capa de Dominio

La capa de dominio es el corazón de la aplicación. Contiene toda la lógica de negocio y no tiene dependencias de frameworks externos.

### Modelo de Dominio

#### `Supplier` (Raíz del Agregado)

Es la entidad principal que encapsula el ciclo de vida del proveedor. Contiene:

- **Atributos**: nombre, DUNS, código de país, facturación anual, estado, calificación de sostenibilidad.
- **Transiciones de estado**: `apply()`, `accept()`, `refuse()`, `reapply()`, `ban()`, `updateRating()`.
- **Validaciones de negocio**: Verifica país aprobado, facturación mínima de 1 millón de euros, calificación de sostenibilidad válida, y restricciones de transición de estado.

#### `SupplierStatus` (Enum)

Define los posibles estados del proveedor:

| Estado | Descripción |
|---|---|
| `CANDIDATE` | Candidato que ha aplicado |
| `DECLINED` | Candidato rechazado (puede volver a aplicar) |
| `ACTIVE` | Proveedor activo (calificación A o B) |
| `ON_PROBATION` | Proveedor en periodo de prueba (calificación C, D o E) |
| `DISQUALIFIED` | Proveedor descalificado permanentemente |

### Value Objects

Los Value Objects son objetos inmutables (implementados como `record` de Java) que representan conceptos del dominio con validación incorporada:

- **`Duns`**: Número DUNS de 9 dígitos. Valida que esté en el rango válido (100.000.000 – 999.999.999).
- **`CountryCode`**: Código de país ISO 3166-1 alpha-2. Valida que sea exactamente 2 letras.
- **`Money`**: Representa una cantidad monetaria no negativa con operaciones de comparación.
- **`SustainabilityRating`**: Calificación de sostenibilidad (A-E) con constante de puntuación asociada (A=1.0, B=0.75, C=0.5, D=0.25, E=0.1) y clasificación de calidad (A/B = alta calidad).

### Políticas

- **`CountryPolicy`**: Interfaz de dominio que define el contrato para verificar si un país está aprobado. Implementada por un adaptador que consulta un servicio REST externo.

### Excepciones

- **`DomainException`**: Excepción personalizada que incluye un código de error y un mensaje descriptivo, usada para comunicar violaciones de reglas de negocio.

---

## Capa de Aplicación

La capa de aplicación orquesta los casos de uso del dominio. Define los **puertos de entrada** (interfaces de casos de uso) y los **puertos de salida** (interfaces de repositorios y servicios externos).

### Puertos de Entrada (Casos de Uso)

Cada caso de uso se define como una interfaz funcional que representa una operación de negocio:

| Caso de Uso | Descripción | Parámetros |
|---|---|---|
| `ApplyCandidateUseCase` | Crear candidatura | nombre, DUNS, país, facturación |
| `AcceptCandidateUseCase` | Aceptar candidato | DUNS, calificación de sostenibilidad |
| `RefuseCandidateUseCase` | Rechazar candidato | DUNS |
| `GetCandidateUseCase` | Obtener candidato | DUNS |
| `GetSupplierUseCase` | Obtener proveedor | DUNS |
| `BanSupplierUseCase` | Descalificar proveedor | DUNS |
| `CalculatePotentialSuppliersUseCase` | Calcular proveedores potenciales | coste, límite, offset |
| `UpdateSustainabilityRatingUseCase` | Actualizar calificación | DUNS, nueva calificación |

### Puertos de Salida

- **`SupplierRepositoryPort`**: Define las operaciones de persistencia necesarias para el dominio: `findByDuns()`, `save()`, `findRowsForScoring()`.

### Servicios de Aplicación

Los servicios implementan los puertos de entrada y orquestan la lógica de negocio utilizando el modelo de dominio y los puertos de salida:

- **`ApplyCandidateService`**: Gestiona nuevas solicitudes o re-aplicaciones de candidatos rechazados.
- **`AcceptCandidateService`**: Valida la política de país y la facturación, y establece la calificación inicial del proveedor.
- **`RefuseCandidateService`**: Rechaza una candidatura activa.
- **`GetCandidateService`**: Recupera información del candidato (estados CANDIDATE o DECLINED).
- **`GetSupplierService`**: Recupera información del proveedor (estados ACTIVE, ON_PROBATION o DISQUALIFIED).
- **`BanSupplierService`**: Descalifica un proveedor en periodo de prueba.
- **`CalculatePotentialSuppliersService`**: Calcula y ordena proveedores potenciales según facturación, calificación y bonificación por país.
- **`UpdateSustainabilityRatingService`**: Actualiza la calificación de sostenibilidad de un proveedor existente.

#### Utilidad: `TwoMinUnique`

Clase auxiliar que identifica los dos valores mínimos únicos de facturación por país, utilizada para aplicar la bonificación del 25% a proveedores pequeños.

---

## Capa de Adaptadores

Los adaptadores conectan el núcleo de la aplicación con el mundo exterior.

### Adaptadores de Entrada

#### REST Controllers

- **`CandidateController`**: Expone los endpoints para la gestión de candidatos.
  - `POST /candidates` — Crear candidatura
  - `GET /candidates/{duns}` — Obtener candidato por DUNS
  - `POST /candidates/{duns}/acceptance` — Aceptar candidato
  - `POST /candidates/{duns}/refusal` — Rechazar candidato

- **`SupplierController`**: Expone los endpoints para la gestión de proveedores.
  - `GET /suppliers/{duns}` — Obtener proveedor por DUNS
  - `POST /suppliers/{duns}/ban` — Descalificar proveedor
  - `GET /suppliers/potential?rate={rate}&limit={limit}&offset={offset}` — Proveedores potenciales

#### Listener de Eventos

- **`SustainabilityRatingEventListener`**: Escucha eventos de tipo `SustainabilityRatingEvent` (eventos de Spring) y delega al caso de uso `UpdateSustainabilityRatingUseCase` para actualizar la calificación.

#### DTOs

Los objetos de transferencia de datos (DTOs) desacoplan la representación REST del modelo de dominio:

- `CandidateDto`, `CandidateAcceptDto` — Para operaciones con candidatos
- `SupplierDto` — Para operaciones con proveedores
- `PotentialSupplierDto`, `PotentialSuppliersDto`, `PaginationDto` — Para proveedores potenciales con paginación
- `ErrorDto` — Para respuestas de error estructuradas

#### Manejo Global de Excepciones

- **`ApiExceptionHandler`**: Traduce las excepciones de dominio (`DomainException`) a respuestas HTTP con códigos de estado apropiados (404, 400, 422, 409).

### Adaptadores de Salida

#### Persistencia (JPA)

- **`SupplierEntity`**: Entidad JPA mapeada a la tabla `suppliers` con DUNS como clave primaria.
- **`SupplierJpaRepository`**: Repositorio Spring Data JPA con consulta personalizada `findRowsForScoring()` para la lógica de puntuación.
- **`SupplierMapper`**: Mapeador estático entre entidades JPA y objetos de dominio (`toEntity()`, `toDomain()`).
- **`SupplierRepositoryJpaAdapter`**: Implementa `SupplierRepositoryPort` adaptando las operaciones de Spring Data JPA al contrato definido por el dominio.

#### Cliente REST Externo

- **`CountryPolicyRestClient`**: Implementa `CountryPolicy` consumiendo la API REST externa de países para verificar si un país está aprobado o prohibido. La URL base es configurable mediante la propiedad `itx.country.base-url`.

---

## Configuración e Inyección de Dependencias

### `CoreWiringConfig`

Clase de configuración de Spring (`@Configuration`) que conecta todos los componentes:
- Crea los beans de servicios de aplicación inyectando los puertos de salida (repositorio, política de país).
- Mantiene la lógica de dominio desacoplada del framework.

### Decoradores Transaccionales

Se utilizan decoradores para añadir el comportamiento transaccional (`@Transactional`) sin contaminar los servicios de dominio con anotaciones de Spring:

- `AcceptCandidateTxDecorator`
- `ApplyCandidateTxDecorator`
- `BanSupplierTxDecorator`
- `PotentialSuppliersTxDecorator`
- `RefuseCandidateTxDecorator`
- `UpdateRatingTxDecorator`

Cada decorador está marcado como `@Primary` para ser preferido sobre el bean de servicio base, garantizando que todas las operaciones se ejecuten dentro de una transacción.

---

## Ciclo de Vida del Proveedor

El ciclo de vida del proveedor sigue una máquina de estados finitos:

```
     ┌──────────────┐
     │   CANDIDATE   │◄──────────────────────────────┐
     └──────┬───────┘                                │
            │                                        │
     ┌──────▼───────┐                         ┌─────┴──────┐
     │   Aceptar    │                         │  Reapply   │
     │  (accept)    │                         │ (reapply)  │
     └──────┬───────┘                         └─────┬──────┘
            │                                       │
    ┌───────▼────────┐          ┌──────────┐  ┌────▼───────┐
    │  Rating A o B? ├── Sí ──► │  ACTIVE  │  │  DECLINED  │
    └───────┬────────┘          └──────────┘  └────────────┘
            │ No                       ▲
    ┌───────▼──────────┐               │
    │  ON_PROBATION    │───(rating)────┘
    └───────┬──────────┘
            │
    ┌───────▼──────────┐
    │  DISQUALIFIED    │
    └──────────────────┘
```

### Transiciones permitidas

| Estado Actual | Acción | Estado Resultante |
|---|---|---|
| — | `apply()` | CANDIDATE |
| CANDIDATE | `accept(rating A/B)` | ACTIVE |
| CANDIDATE | `accept(rating C/D/E)` | ON_PROBATION |
| CANDIDATE | `refuse()` | DECLINED |
| DECLINED | `reapply()` | CANDIDATE |
| ON_PROBATION | `ban()` | DISQUALIFIED |
| ACTIVE/ON_PROBATION | `updateRating(A/B)` | ACTIVE |
| ACTIVE/ON_PROBATION | `updateRating(C/D/E)` | ON_PROBATION |

---

## Reglas de Negocio

1. **Candidato activo**: Un candidato cuya candidatura no ha sido rechazada.
2. **Unicidad por DUNS**: Solo puede existir una candidatura activa por DUNS.
3. **Unicidad de proveedor**: Solo puede existir un proveedor por DUNS.
4. **Exclusión mutua**: No pueden coexistir un candidato activo y un proveedor para el mismo DUNS.
5. **Visibilidad API**: El estado `ON_PROBATION` se muestra como `Active` en la API; solo se distingue internamente.
6. **Requisitos de aceptación**:
   - El país no debe estar en la lista de países prohibidos.
   - La facturación anual debe ser de al menos 1.000.000 €.
7. **Re-aplicación**: Un candidato rechazado puede volver a aplicar.
8. **Descalificación**: Solo proveedores en estado `ON_PROBATION` pueden ser descalificados, y no pueden volver a ser proveedores.

---

## Proveedores Potenciales

El cálculo de proveedores potenciales para un pedido sigue esta lógica:

### Criterios de Evaluación

1. **Elegibilidad**: La facturación anual del proveedor debe ser mayor que el coste del pedido.
2. **Exclusión**: Los proveedores descalificados no son considerados.

### Cálculo de Puntuación

```
Puntuación = Facturación anual × 0.1 × Constante de calificación
```

Donde las constantes de calificación son:

| Calificación | Constante |
|---|---|
| A | 1.0 |
| B | 0.75 |
| C | 0.5 |
| D | 0.25 |
| E | 0.1 |

### Bonificación por País

Para impulsar a los pequeños proveedores de cada país, se aplica una **bonificación del 25%** a todos los proveedores cuya facturación anual sea una de las **dos facturaciones únicas más bajas** dentro de su país.

> **Ejemplo**: Dados 5 proveedores con facturaciones (200, 200, 200, 210, 250), las dos facturaciones únicas más bajas son 200 y 210, por lo que los proveedores s1, s2, s3 y s4 reciben la bonificación.

---

## Manejo de Errores

El `ApiExceptionHandler` traduce las excepciones de dominio a respuestas HTTP:

| Código de Error | HTTP Status | Descripción |
|---|---|---|
| `NOT_FOUND` | 404 | Recurso no encontrado |
| `BAD_REQUEST` | 400 | Solicitud inválida |
| `UNPROCESSABLE` | 422 | Entidad no procesable (regla de negocio violada) |
| `CONFLICT` | 409 | Conflicto (ej. candidatura duplicada) |

---

## Persistencia

- **Base de datos**: H2 en memoria, configurada con generación automática de esquema (`spring.jpa.hibernate.ddl-auto`).
- **Entidad**: `SupplierEntity` mapeada a la tabla `suppliers` con DUNS como identificador.
- **Repositorio**: Spring Data JPA con consultas personalizadas para la optimización de operaciones de lectura (proyecciones para scoring).

---

## Eventos

El sistema utiliza **eventos de Spring** para gestionar las actualizaciones de calificación de sostenibilidad:

1. Se publica un `SustainabilityRatingEvent` (mediante `POST /sustainability/update`).
2. El `SustainabilityRatingEventListener` captura el evento.
3. Se delega al `UpdateSustainabilityRatingUseCase` que actualiza la calificación del proveedor.
4. El cambio de calificación puede provocar una transición de estado (ACTIVE ↔ ON_PROBATION).

---

## Consideraciones de Rendimiento y Escalabilidad

- **Rango de datos**: El sistema está diseñado para manejar entre 100.000 y 1.000.000 de proveedores.
- **Consultas optimizadas**: La consulta de scoring utiliza proyecciones JPA para transferir solo los datos necesarios.
- **Clase `TwoMinUnique`**: Algoritmo eficiente O(n) para calcular los dos mínimos únicos por país, evitando ordenaciones costosas.
- **Paginación**: Los endpoints de proveedores potenciales soportan paginación con `limit` y `offset`.
- **Transacciones**: Los decoradores transaccionales garantizan la consistencia de datos en operaciones concurrentes.
- **Base de datos en memoria**: H2 proporciona acceso rápido sin overhead de red para el entorno de desarrollo/pruebas.

---

## Diagrama de Flujo del Proveedor

![FSM Supplier](_files/iop-techtest-fsm-supplier.png)

Este diagrama representa la máquina de estados finitos que gobierna el ciclo de vida del proveedor en el sistema.
