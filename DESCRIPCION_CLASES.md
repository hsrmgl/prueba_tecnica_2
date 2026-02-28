# Descripción de Clases — Prueba Técnica Gestión de Proveedores Inditex

Este documento describe en detalle cada clase del proyecto, su propósito y cómo se relaciona con el resto de la arquitectura. Está pensado para ayudar a comprender y defender la solución implementada.

---

## Índice

1. [Visión general de la solución](#visión-general-de-la-solución)
2. [Capa de Dominio](#capa-de-dominio)
   - [Modelo](#modelo)
   - [Value Objects](#value-objects)
   - [Políticas](#políticas)
   - [Excepciones](#excepciones)
3. [Capa de Aplicación](#capa-de-aplicación)
   - [Puertos de Entrada (Casos de Uso)](#puertos-de-entrada-casos-de-uso)
   - [Puertos de Salida](#puertos-de-salida)
   - [Servicios de Aplicación](#servicios-de-aplicación)
4. [Capa de Adaptadores](#capa-de-adaptadores)
   - [Adaptadores de Entrada (REST)](#adaptadores-de-entrada-rest)
   - [Adaptadores de Entrada (Eventos)](#adaptadores-de-entrada-eventos)
   - [DTOs](#dtos)
   - [Manejo de Errores](#manejo-de-errores)
   - [Adaptadores de Salida (Persistencia)](#adaptadores-de-salida-persistencia)
   - [Adaptadores de Salida (Cliente REST)](#adaptadores-de-salida-cliente-rest)
5. [Configuración](#configuración)
   - [Cableado de dependencias](#cableado-de-dependencias)
   - [Decoradores transaccionales](#decoradores-transaccionales)
6. [Clases proporcionadas (no modificables)](#clases-proporcionadas-no-modificables)
7. [Cómo responde la solución a cada requisito](#cómo-responde-la-solución-a-cada-requisito)

---

## Visión general de la solución

La solución sigue una **Arquitectura Hexagonal** (Puertos y Adaptadores) combinada con principios de **Domain-Driven Design (DDD)**. La idea clave es que toda la lógica de negocio vive en las capas internas (Dominio y Aplicación) y no depende de ningún framework. Las capas externas (Adaptadores) conectan el dominio con el mundo real (HTTP, base de datos, eventos).

```
  API REST (Controllers) ──► Puertos de Entrada (Interfaces) ──► Servicios ──► Dominio
                                                                      │
                                                               Puertos de Salida
                                                                      │
                                                    Adaptadores de Salida (JPA, REST Client)
```

**¿Por qué esta arquitectura?** Porque permite:
- Testear la lógica de negocio sin necesidad de levantar Spring ni una base de datos.
- Cambiar la persistencia (de H2 a PostgreSQL, por ejemplo) sin tocar el dominio.
- Evolucionar la API REST sin afectar las reglas de negocio.

---

## Capa de Dominio

> **Paquete**: `com.itxiop.tech.supplier.sandbox.domain`

La capa de dominio es el núcleo de la aplicación. No tiene ninguna dependencia de Spring, JPA ni ningún framework. Solo Java puro.

---

### Modelo

#### `Supplier` — Raíz del Agregado

> **Paquete**: `sandbox.domain.model`

Es la clase principal del dominio. Representa un proveedor en cualquier estado de su ciclo de vida.

**¿Qué problema resuelve?**
Encapsula todas las reglas de negocio del ciclo de vida del proveedor en un único lugar. Cualquier transición de estado (aplicar, aceptar, rechazar, etc.) pasa por esta clase, garantizando que nunca se viole una regla de negocio.

**Atributos principales**:
| Atributo | Tipo | Descripción |
|----------|------|-------------|
| `name` | `String` | Nombre del proveedor |
| `duns` | `Duns` | Identificador único (9 dígitos) |
| `country` | `CountryCode` | País de la sede (ISO alpha-2) |
| `annualTurnover` | `Money` | Facturación anual en euros |
| `status` | `SupplierStatus` | Estado actual en el ciclo de vida |
| `rating` | `SustainabilityRating` | Calificación de sostenibilidad (A-E) |

**Métodos clave**:

| Método | Qué hace | Transición de estado |
|--------|----------|---------------------|
| `apply(name, duns, country, turnover)` | Crea un nuevo candidato. Es un método estático (factory). | → `CANDIDATE` |
| `rehydrate(...)` | Reconstruye un Supplier desde persistencia con validación. | (sin cambio) |
| `accept(rating, countryPolicy)` | Acepta al candidato como proveedor. Verifica que el país esté aprobado y la facturación sea ≥ 1M€. | `CANDIDATE` → `ACTIVE` o `ON_PROBATION` |
| `refuse()` | Rechaza la candidatura. | `CANDIDATE` → `DECLINED` |
| `ban()` | Descalifica al proveedor. Solo permitido si está en `ON_PROBATION`. | `ON_PROBATION` → `DISQUALIFIED` |
| `updateRating(rating)` | Actualiza la calificación. Puede provocar cambio entre `ACTIVE` y `ON_PROBATION`. | `ACTIVE` ↔ `ON_PROBATION` |

**¿Por qué es importante?** Toda la lógica de negocio está aquí. Los servicios simplemente delegan a estos métodos. Si un revisor quiere entender las reglas de negocio, esta es la clase que debe leer.

---

#### `SupplierStatus` — Estados del proveedor

> **Paquete**: `sandbox.domain.model`

Enum que define los cinco estados posibles del proveedor.

| Estado | Significado | Visible en API como |
|--------|-------------|-------------------|
| `CANDIDATE` | Ha solicitado ser proveedor | (no visible como supplier) |
| `DECLINED` | Candidatura rechazada | (no visible como supplier) |
| `ACTIVE` | Proveedor activo (rating A o B) | `"Active"` |
| `ON_PROBATION` | Proveedor en prueba (rating C, D o E) | `"Active"` (se oculta) |
| `DISQUALIFIED` | Proveedor descalificado permanentemente | `"Disqualified"` |

**Método importante**: `apiShowsActive()` → devuelve `true` si el estado es `ACTIVE` o `ON_PROBATION`. Esto implementa la regla de negocio de que ON_PROBATION se muestra como "Active" en la API.

---

### Value Objects

Los Value Objects son objetos inmutables (implementados como `record` de Java 21) que encapsulan validación en el momento de la creación. Si los datos son inválidos, lanzan una excepción inmediatamente.

#### `Duns` — Número DUNS

> **Paquete**: `sandbox.domain.value`

```java
public record Duns(int value) { ... }
```

**Validación**: El valor debe estar entre 100.000.000 y 999.999.999 (exactamente 9 dígitos).

**¿Por qué un Value Object?** Evita que se pase un `int` cualquiera como DUNS. Si alguna parte del código intenta crear un `Duns` con un número inválido, falla inmediatamente, no más tarde en la base de datos.

---

#### `CountryCode` — Código de País

> **Paquete**: `sandbox.domain.value`

```java
public record CountryCode(String value) { ... }
```

**Validación**: Debe ser exactamente 2 letras (formato ISO 3166-1 alpha-2, ej: `"ES"`, `"FR"`, `"PT"`).

---

#### `Money` — Cantidad Monetaria

> **Paquete**: `sandbox.domain.value`

```java
public record Money(long cents) { ... }
```

**Validación**: No puede ser negativo.

**Métodos útiles**:
- `eur(long euros)` — Factory method para crear desde euros.
- `lessThan(Money other)` — Comparación de cantidades.

**¿Por qué `long` en céntimos?** Para evitar problemas de precisión con decimales (`double`/`float`). Es una práctica estándar en aplicaciones financieras.

---

#### `SustainabilityRating` — Calificación de Sostenibilidad

> **Paquete**: `sandbox.domain.value`

Enum con los valores A, B, C, D, E.

| Rating | `constant()` | `isGood()` |
|--------|:-----------:|:---------:|
| A | 1.0 | ✅ true |
| B | 0.75 | ✅ true |
| C | 0.5 | ❌ false |
| D | 0.25 | ❌ false |
| E | 0.1 | ❌ false |

- `constant()` → Se usa para el cálculo de puntuación de proveedores potenciales.
- `isGood()` → Determina si el proveedor es ACTIVE (true) o ON_PROBATION (false).

---

### Políticas

#### `CountryPolicy` — Política de País

> **Paquete**: `sandbox.domain.policy`

```java
public interface CountryPolicy {
    boolean isApproved(CountryCode country);
}
```

Es una **interfaz de dominio** (puerto de salida del dominio). Define el contrato: "Dado un código de país, ¿está aprobado para aceptar proveedores?"

**¿Quién la implementa?** El adaptador `CountryPolicyRestClient` que consulta un servicio REST externo.

**¿Por qué es una interfaz?** Para que el dominio no dependa de cómo se obtiene esta información (puede ser REST, base de datos, archivo de configuración, etc.).

---

### Excepciones

#### `DomainException` — Excepción de Dominio

> **Paquete**: `sandbox.domain.exception`

```java
public class DomainException extends RuntimeException {
    private final String code;
    // ...
}
```

Excepción que incluye un **código de error** (`NOT_FOUND`, `BAD_REQUEST`, `UNPROCESSABLE`, `CONFLICT`) y un **mensaje descriptivo**.

**¿Cómo se usa?** El modelo de dominio lanza `DomainException` cuando se violan reglas de negocio. El `ApiExceptionHandler` la captura y la traduce a un código HTTP.

---

## Capa de Aplicación

> **Paquete**: `com.itxiop.tech.supplier.sandbox.application`

La capa de aplicación orquesta los casos de uso. No contiene lógica de negocio (esa está en el dominio), sino que coordina: buscar datos → ejecutar operación de dominio → persistir.

---

### Puertos de Entrada (Casos de Uso)

> **Paquete**: `sandbox.application.port.in`

Cada caso de uso se define como una **interfaz funcional**. Esto hace explícito qué operaciones ofrece la aplicación.

| Interfaz | Operación | Input | Output |
|----------|-----------|-------|--------|
| `ApplyCandidateUseCase` | Crear candidatura | nombre, DUNS, país, facturación | Supplier |
| `AcceptCandidateUseCase` | Aceptar candidato | DUNS, rating | void |
| `RefuseCandidateUseCase` | Rechazar candidato | DUNS | void |
| `GetCandidateUseCase` | Obtener candidato | DUNS | Supplier |
| `GetSupplierUseCase` | Obtener proveedor | DUNS | Supplier |
| `BanSupplierUseCase` | Descalificar proveedor | DUNS | void |
| `CalculatePotentialSuppliersUseCase` | Proveedores potenciales | coste, límite, offset | Lista + paginación |
| `UpdateSustainabilityRatingUseCase` | Actualizar rating | DUNS, rating | void |

**¿Por qué interfaces separadas?** Principio de Segregación de Interfaces (ISP). Cada consumidor solo conoce la operación que necesita.

---

### Puertos de Salida

#### `SupplierRepositoryPort` — Puerto del Repositorio

> **Paquete**: `sandbox.application.port.out`

```java
public interface SupplierRepositoryPort {
    Optional<Supplier> findByDuns(Duns duns);
    Supplier save(Supplier supplier);
    List<SupplierRow> findRowsForScoring();
}
```

Define las operaciones de persistencia que necesita la aplicación, sin decir cómo se implementan.

**`SupplierRow`**: Record interno que es una proyección plana (solo los campos necesarios para el cálculo de scoring), optimizando la consulta a base de datos.

---

### Servicios de Aplicación

> **Paquete**: `sandbox.application.service`

Cada servicio implementa un puerto de entrada y sigue el mismo patrón:
1. Buscar en el repositorio
2. Delegar al modelo de dominio
3. Persistir el resultado

#### `ApplyCandidateService`

**Flujo**: Recibe los datos → Verifica si ya existe un supplier con ese DUNS → Si existe y está DECLINED, ejecuta `reapply()` → Si no existe, ejecuta `Supplier.apply()` → Guarda y devuelve.

**Reglas que aplica**: No permite re-aplicar si hay un candidato activo, un proveedor activo/on-probation, o un proveedor descalificado.

---

#### `AcceptCandidateService`

**Flujo**: Busca el candidato por DUNS → Ejecuta `supplier.accept(rating, countryPolicy)` → Guarda.

**Reglas que aplica**: El país debe estar aprobado (usa `CountryPolicy`), la facturación debe ser ≥ 1M€, el candidato debe estar en estado `CANDIDATE`.

---

#### `RefuseCandidateService`

**Flujo**: Busca el candidato por DUNS → Ejecuta `supplier.refuse()` → Guarda.

**Reglas que aplica**: Solo se puede rechazar un candidato en estado `CANDIDATE`.

---

#### `GetCandidateService`

**Flujo**: Busca por DUNS → Verifica que el estado es `CANDIDATE` o `DECLINED` → Devuelve.

**Reglas que aplica**: Solo devuelve candidatos, no proveedores.

---

#### `GetSupplierService`

**Flujo**: Busca por DUNS → Verifica que el estado es `ACTIVE`, `ON_PROBATION` o `DISQUALIFIED` → Devuelve.

**Reglas que aplica**: Solo devuelve proveedores, no candidatos.

---

#### `BanSupplierService`

**Flujo**: Busca el proveedor por DUNS → Ejecuta `supplier.ban()` → Guarda.

**Reglas que aplica**: Solo se puede descalificar un proveedor en estado `ON_PROBATION`.

---

#### `CalculatePotentialSuppliersService`

**Flujo**:
1. Obtiene todos los proveedores elegibles vía `findRowsForScoring()`.
2. Filtra: facturación > coste del pedido y no descalificado.
3. Calcula la puntuación base: `facturación × 0.1 × constante_rating`.
4. Agrupa por país y calcula las dos facturaciones únicas más bajas por país.
5. Aplica bonificación del 25% a proveedores con esas facturaciones.
6. Ordena por puntuación descendente.
7. Aplica paginación (limit/offset).

**Reglas que aplica**: Toda la lógica de scoring y bonificación.

---

#### `UpdateSustainabilityRatingService`

**Flujo**: Busca el proveedor por DUNS → Ejecuta `supplier.updateRating(newRating)` → Guarda.

**Reglas que aplica**: Solo proveedores activos u ON_PROBATION pueden recibir actualización de rating.

---

#### `TwoMinUnique` — Utilidad auxiliar

> **Paquete**: `sandbox.application.service`

Clase auxiliar que calcula eficientemente los dos valores mínimos únicos de facturación dentro de un grupo de proveedores por país.

**¿Por qué existe?** El cálculo de la bonificación del 25% necesita saber cuáles son las dos facturaciones únicas más bajas por país. Esta clase lo hace en O(n) sin necesidad de ordenar, lo cual es importante para rendimiento con 100.000-1.000.000 de proveedores.

---

## Capa de Adaptadores

> **Paquete**: `com.itxiop.tech.supplier.sandbox.adapters`

Los adaptadores conectan el dominio con el exterior. Hay adaptadores de entrada (reciben peticiones) y de salida (acceden a infraestructura).

---

### Adaptadores de Entrada (REST)

#### `CandidateController`

> **Paquete**: `sandbox.adapters.in.rest`

Controlador REST que expone los endpoints de gestión de candidatos.

| Endpoint | Método HTTP | Caso de Uso que invoca |
|----------|:-----------:|----------------------|
| `/candidates` | POST | `ApplyCandidateUseCase` |
| `/candidates/{duns}` | GET | `GetCandidateUseCase` |
| `/candidates/{duns}/accept` | POST | `AcceptCandidateUseCase` |
| `/candidates/{duns}/refuse` | POST | `RefuseCandidateUseCase` |

**Responsabilidad**: Recibe la petición HTTP, extrae los datos del DTO, llama al caso de uso correspondiente, y devuelve la respuesta HTTP adecuada.

---

#### `SupplierController`

> **Paquete**: `sandbox.adapters.in.rest`

Controlador REST que expone los endpoints de gestión de proveedores.

| Endpoint | Método HTTP | Caso de Uso que invoca |
|----------|:-----------:|----------------------|
| `/suppliers/{duns}` | GET | `GetSupplierUseCase` |
| `/suppliers/{duns}/ban` | POST | `BanSupplierUseCase` |
| `/suppliers/potential` | GET | `CalculatePotentialSuppliersUseCase` |

---

### Adaptadores de Entrada (Eventos)

#### `SustainabilityRatingEventListener`

> **Paquete**: `sandbox.adapters.in.event`

Escucha eventos de Spring de tipo `SustainabilityRatingEvent` y delega al `UpdateSustainabilityRatingUseCase`.

**¿Cómo funciona?** Cuando se hace `POST /sustainability/update`, el sistema de Spring publica un evento. Este listener lo captura y actualiza el rating del proveedor.

**¿Por qué un listener y no un controller?** Porque se nos proporcionó el evento como mecanismo de comunicación (desacoplamiento por eventos).

---

### DTOs

> **Paquete**: `sandbox.adapters.in.rest.dto`

Los DTOs (Data Transfer Objects) desacoplan la representación REST del modelo de dominio.

| DTO | Uso |
|-----|-----|
| `CandidateDto` | Request/Response para crear y obtener candidatos |
| `CandidateAcceptDto` | Request para aceptar candidato (contiene `sustainabilityRating`) |
| `SupplierDto` | Response para obtener proveedor (incluye `status` y `sustainabilityRating`) |
| `PotentialSupplierDto` | Response para cada proveedor potencial (incluye `score`) |
| `PotentialSuppliersDto` | Response wrapper con `data` (lista) y `pagination` |
| `PaginationDto` | Información de paginación (`limit`, `offset`, `total`) |
| `ErrorDto` | Response de error con campo `info` |

**¿Por qué DTOs separados del dominio?** Para que los cambios en la API no afecten al dominio y viceversa. Es una práctica estándar en arquitectura hexagonal.

---

### Manejo de Errores

#### `ApiExceptionHandler`

> **Paquete**: `sandbox.adapters.in.rest`

Clase anotada con `@RestControllerAdvice` que captura todas las `DomainException` y las traduce a respuestas HTTP estructuradas.

| Código de DomainException | HTTP Status | Ejemplo |
|--------------------------|:-----------:|---------|
| `NOT_FOUND` | 404 | Candidato/proveedor no encontrado |
| `BAD_REQUEST` | 400 | Datos de entrada inválidos |
| `UNPROCESSABLE` | 422 | Regla de negocio violada |
| (otros) | 409 | Conflicto (duplicado, transición inválida) |

---

### Adaptadores de Salida (Persistencia)

#### `SupplierEntity`

> **Paquete**: `sandbox.adapters.out.persistence`

Entidad JPA mapeada a la tabla `suppliers` en la base de datos H2.

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `duns` | `int` (PK) | Identificador único |
| `name` | `String` | Nombre del proveedor |
| `country` | `String` | Código de país |
| `annualTurnover` | `long` | Facturación anual |
| `status` | `String` | Estado actual |
| `rating` | `String` | Calificación de sostenibilidad |

---

#### `SupplierJpaRepository`

> **Paquete**: `sandbox.adapters.out.persistence`

Interfaz de Spring Data JPA. Extiende `JpaRepository` y añade una consulta personalizada:

- `findRowsForScoring()` — Consulta JPQL optimizada que devuelve proyecciones `SupplierRow` con solo los campos necesarios para el cálculo de scoring, evitando cargar entidades completas.

---

#### `SupplierMapper`

> **Paquete**: `sandbox.adapters.out.persistence`

Clase estática con métodos de mapeo:
- `toEntity(Supplier)` → `SupplierEntity` — Para guardar en BD.
- `toDomain(SupplierEntity)` → `Supplier` — Para recuperar desde BD.

---

#### `SupplierRepositoryJpaAdapter`

> **Paquete**: `sandbox.adapters.out.persistence`

Implementa `SupplierRepositoryPort` utilizando `SupplierJpaRepository` y `SupplierMapper`.

**¿Qué hace?** Traduce entre el contrato del dominio y Spring Data JPA. Es el "pegamento" entre el mundo del dominio y la base de datos.

---

### Adaptadores de Salida (Cliente REST)

#### `CountryPolicyRestClient`

> **Paquete**: `sandbox.adapters.out.country`

Implementa `CountryPolicy` consultando el servicio REST de países.

**Flujo**: Hace `GET /countries/{code}` → Si responde 200 y `isBanned: false`, devuelve `true` (aprobado). Si responde 404, devuelve `false`.

**Configuración**: La URL base se configura con la propiedad `itx.country.base-url`.

---

## Configuración

> **Paquete**: `com.itxiop.tech.supplier.sandbox.config`

### Cableado de dependencias

#### `CoreWiringConfig`

Clase `@Configuration` de Spring que crea los beans de los servicios de aplicación inyectando manualmente las dependencias.

**¿Por qué no usar `@Service` directamente?** Para mantener los servicios de aplicación como clases Java puras (sin anotaciones de Spring). La configuración explícita deja claro qué depende de qué.

---

### Decoradores transaccionales

Los decoradores añaden comportamiento transaccional sin contaminar los servicios con `@Transactional`:

| Decorador | Servicio que decora | Tipo de transacción |
|-----------|--------------------|--------------------|
| `ApplyCandidateTxDecorator` | `ApplyCandidateService` | Read-Write |
| `AcceptCandidateTxDecorator` | `AcceptCandidateService` | Read-Write |
| `RefuseCandidateTxDecorator` | `RefuseCandidateService` | Read-Write |
| `BanSupplierTxDecorator` | `BanSupplierService` | Read-Write |
| `PotentialSuppliersTxDecorator` | `CalculatePotentialSuppliersService` | Read-Only |
| `UpdateRatingTxDecorator` | `UpdateSustainabilityRatingService` | Read-Write |

**¿Cómo funciona?** Cada decorador:
1. Está anotado con `@Component` y `@Primary` (Spring lo elige por defecto).
2. Implementa la misma interfaz de caso de uso.
3. Recibe el servicio "core" (con `@Qualifier`).
4. Su método está anotado con `@Transactional`.
5. Delega la ejecución al servicio core.

**¿Por qué este patrón?** Para que los servicios del dominio sean testeables unitariamente sin Spring y sin transacciones. El decorador es una capa fina que solo añade el aspecto transaccional.

---

## Clases proporcionadas (no modificables)

Estas clases vienen con el proyecto base y no deben modificarse:

| Clase | Paquete | Descripción |
|-------|---------|-------------|
| `Application` | `com.itxiop.tech.supplier` | Punto de entrada Spring Boot (`@SpringBootApplication`) |
| `Country` | `supplier.country` | Record con datos de país (`name`, `isBanned`) |
| `CountryController` | `supplier.country` | Controller REST del servicio de países (`GET /countries/{country}`) |
| `SustainabilityRating` | `supplier.sustainability` | Record del evento de rating |
| `SustainabilityRatingController` | `supplier.sustainability` | Controller que publica el evento (`POST /sustainability/update`) |
| `SustainabilityRatingEvent` | `supplier.sustainability` | Evento de Spring para cambios de rating |

---

## Cómo responde la solución a cada requisito

### Requisitos obligatorios

| Requisito | Implementado en | Cómo |
|-----------|----------------|------|
| **Crear candidato** | `ApplyCandidateService` → `Supplier.apply()` | Valida datos, crea candidatura, persiste |
| **Aceptar candidato** | `AcceptCandidateService` → `Supplier.accept()` | Verifica país y facturación, asigna rating, transiciona estado |
| **Proveedores potenciales** | `CalculatePotentialSuppliersService` | Filtra, calcula score con bonificación, ordena, pagina |

### Requisitos opcionales

| Requisito | Implementado en | Cómo |
|-----------|----------------|------|
| **Actualizar rating** | `UpdateSustainabilityRatingService` + `SustainabilityRatingEventListener` | Escucha evento, actualiza rating, transiciona estado si necesario |
| **Paginación potenciales** | `CalculatePotentialSuppliersService` | Soporta `limit` y `offset`, devuelve total |
| **Rechazar candidato** | `RefuseCandidateService` → `Supplier.refuse()` | Transiciona a DECLINED |
| **Descalificar proveedor** | `BanSupplierService` → `Supplier.ban()` | Solo desde ON_PROBATION, permanente |
| **Obtener proveedor** | `GetSupplierService` | Filtra por estados de proveedor, ON_PROBATION muestra como Active |
| **Obtener candidato** | `GetCandidateService` | Filtra por estados de candidato |

### Reglas de negocio implementadas

| Regla | Dónde se aplica |
|-------|----------------|
| DUNS único por candidatura activa | `ApplyCandidateService` |
| País no prohibido para aceptar | `Supplier.accept()` usando `CountryPolicy` |
| Facturación ≥ 1M€ para aceptar | `Supplier.accept()` |
| ON_PROBATION se muestra como Active | `SupplierStatus.apiShowsActive()` → DTOs |
| Re-aplicación tras rechazo | `ApplyCandidateService` → `Supplier.reapply()` |
| Descalificado no puede re-aplicar | `ApplyCandidateService` verifica estado |
| Bonificación 25% a 2 facturaciones más bajas | `TwoMinUnique` en `CalculatePotentialSuppliersService` |
| Descalificados excluidos de potenciales | Filtro en `CalculatePotentialSuppliersService` |

### Decisiones de diseño destacables

| Decisión | Justificación |
|----------|--------------|
| **Value Objects como `record`** | Inmutabilidad garantizada por Java 21, validación en constructor |
| **Decoradores transaccionales** | Dominio limpio de anotaciones Spring, testeable sin framework |
| **Proyección `SupplierRow`** | Optimización para no cargar entidades completas en scoring |
| **`TwoMinUnique` en O(n)** | Rendimiento con 100K-1M proveedores |
| **Interfaz `CountryPolicy`** | Desacoplamiento del servicio externo, fácil de mockear en tests |
| **`DomainException` con códigos** | Traducción limpia a HTTP en un único punto (`ApiExceptionHandler`) |
