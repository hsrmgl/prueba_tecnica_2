# Pruebas Postman Propuestas — Gestión de Proveedores Inditex

Este documento describe las pruebas Postman recomendadas para validar el correcto funcionamiento de la API, organizadas por endpoint y escenario, basadas en la arquitectura descrita en `wiki/ARQUITECTURA.md` y las especificaciones OpenAPI.

> **Base URL**: `http://localhost:8080`

---

## Índice

1. [Candidatos — Crear candidatura](#1-candidatos--crear-candidatura)
2. [Candidatos — Obtener candidato](#2-candidatos--obtener-candidato)
3. [Candidatos — Aceptar candidato](#3-candidatos--aceptar-candidato)
4. [Candidatos — Rechazar candidato](#4-candidatos--rechazar-candidato)
5. [Proveedores — Obtener proveedor](#5-proveedores--obtener-proveedor)
6. [Proveedores — Descalificar proveedor](#6-proveedores--descalificar-proveedor)
7. [Proveedores — Proveedores potenciales](#7-proveedores--proveedores-potenciales)
8. [Sostenibilidad — Actualizar calificación](#8-sostenibilidad--actualizar-calificación)
9. [Países — Consultar país](#9-países--consultar-país)
10. [Flujos completos (escenarios E2E)](#10-flujos-completos-escenarios-e2e)

---

## 1. Candidatos — Crear candidatura

**Endpoint**: `POST /candidates`

### 1.1 Crear candidato con datos válidos → 201

```
POST /candidates
Content-Type: application/json

{
  "name": "Textiles Europa S.L.",
  "duns": 123456789,
  "country": "ES",
  "annualTurnover": 5000000
}
```

**Respuesta esperada**: `201 Created`
```json
{
  "name": "Textiles Europa S.L.",
  "duns": 123456789,
  "country": "ES",
  "annualTurnover": 5000000
}
```

**Tests Postman**:
```javascript
pm.test("Status code is 201", () => pm.response.to.have.status(201));
pm.test("Response has correct DUNS", () => {
    const body = pm.response.json();
    pm.expect(body.duns).to.eql(123456789);
    pm.expect(body.name).to.eql("Textiles Europa S.L.");
    pm.expect(body.country).to.eql("ES");
    pm.expect(body.annualTurnover).to.eql(5000000);
});
```

---

### 1.2 Crear candidato duplicado → 409

Enviar la misma petición dos veces para el mismo DUNS.

**Respuesta esperada**: `409 Conflict`
```json
{
  "info": "..."
}
```

**Tests Postman**:
```javascript
pm.test("Status code is 409", () => pm.response.to.have.status(409));
pm.test("Error body has info field", () => {
    pm.expect(pm.response.json().info).to.be.a("string");
});
```

---

### 1.3 Crear candidato con DUNS inválido (fuera de rango) → 400

```
POST /candidates
Content-Type: application/json

{
  "name": "Test",
  "duns": 12345,
  "country": "ES",
  "annualTurnover": 5000000
}
```

**Respuesta esperada**: `400 Bad Request`

---

### 1.4 Crear candidato con país inválido (formato incorrecto) → 400/422

```
POST /candidates
Content-Type: application/json

{
  "name": "Test",
  "duns": 123456780,
  "country": "ESPAÑA",
  "annualTurnover": 5000000
}
```

**Respuesta esperada**: `400 Bad Request` o `422 Unprocessable Entity`

---

### 1.5 Crear candidato con facturación negativa → 400/422

```
POST /candidates
Content-Type: application/json

{
  "name": "Test",
  "duns": 123456780,
  "country": "ES",
  "annualTurnover": -100
}
```

**Respuesta esperada**: `400 Bad Request` o `422 Unprocessable Entity`

---

### 1.6 Crear candidato con DUNS de proveedor descalificado → 409

Si existe un proveedor con estado DISQUALIFIED para ese DUNS, no se permite volver a aplicar.

**Respuesta esperada**: `409 Conflict`

---

## 2. Candidatos — Obtener candidato

**Endpoint**: `GET /candidates/{duns}`

### 2.1 Obtener candidato existente → 200

```
GET /candidates/123456789
```

**Respuesta esperada**: `200 OK`
```json
{
  "name": "Textiles Europa S.L.",
  "duns": 123456789,
  "country": "ES",
  "annualTurnover": 5000000
}
```

**Tests Postman**:
```javascript
pm.test("Status code is 200", () => pm.response.to.have.status(200));
pm.test("Response has candidate data", () => {
    const body = pm.response.json();
    pm.expect(body.duns).to.eql(123456789);
    pm.expect(body.name).to.be.a("string");
    pm.expect(body.country).to.be.a("string");
    pm.expect(body.annualTurnover).to.be.a("number");
});
```

---

### 2.2 Obtener candidato inexistente → 404

```
GET /candidates/999999999
```

**Respuesta esperada**: `404 Not Found`

---

### 2.3 Obtener candidato que ya fue aceptado como proveedor → 404

Una vez aceptado, ya no es candidato sino proveedor, por lo que no aparece en el endpoint de candidatos.

**Respuesta esperada**: `404 Not Found`

---

## 3. Candidatos — Aceptar candidato

**Endpoint**: `POST /candidates/{duns}/accept`

### 3.1 Aceptar candidato con rating A (país aprobado, facturación ≥ 1M€) → 204

```
POST /candidates/123456789/accept
Content-Type: application/json

{
  "sustainabilityRating": "A"
}
```

**Respuesta esperada**: `204 No Content` (sin cuerpo)

**Tests Postman**:
```javascript
pm.test("Status code is 204", () => pm.response.to.have.status(204));
pm.test("Response body is empty", () => {
    pm.expect(pm.response.text()).to.be.empty;
});
```

---

### 3.2 Aceptar candidato con rating C → 204 (proveedor queda ON_PROBATION)

```
POST /candidates/234567890/accept
Content-Type: application/json

{
  "sustainabilityRating": "C"
}
```

**Respuesta esperada**: `204 No Content`

Verificación posterior: `GET /suppliers/234567890` debe devolver `"status": "Active"` (ON_PROBATION se muestra como Active en la API).

---

### 3.3 Aceptar candidato con país prohibido → 409

Primero crear un candidato con un país que esté en la lista de prohibidos (verificar primero con `GET /countries/{country}` cuál está baneado).

**Respuesta esperada**: `409 Conflict`

---

### 3.4 Aceptar candidato con facturación inferior a 1M€ → 409

```
POST /candidates
Content-Type: application/json

{
  "name": "Micro Empresa",
  "duns": 111111111,
  "country": "ES",
  "annualTurnover": 500000
}
```

Luego:
```
POST /candidates/111111111/accept
Content-Type: application/json

{
  "sustainabilityRating": "A"
}
```

**Respuesta esperada**: `409 Conflict`

---

### 3.5 Aceptar candidato inexistente → 404

```
POST /candidates/999999999/accept
Content-Type: application/json

{
  "sustainabilityRating": "A"
}
```

**Respuesta esperada**: `404 Not Found`

---

### 3.6 Aceptar candidato con rating inválido → 400

```
POST /candidates/123456789/accept
Content-Type: application/json

{
  "sustainabilityRating": "X"
}
```

**Respuesta esperada**: `400 Bad Request`

---

### 3.7 Aceptar candidato ya rechazado (DECLINED) → 404/409

Un candidato rechazado ya no es un candidato activo.

**Respuesta esperada**: `404 Not Found` o `409 Conflict`

---

## 4. Candidatos — Rechazar candidato

**Endpoint**: `POST /candidates/{duns}/refuse`

### 4.1 Rechazar candidato activo → 204

```
POST /candidates/123456789/refuse
```

**Respuesta esperada**: `204 No Content`

**Tests Postman**:
```javascript
pm.test("Status code is 204", () => pm.response.to.have.status(204));
```

---

### 4.2 Rechazar candidato inexistente → 404

```
POST /candidates/999999999/refuse
```

**Respuesta esperada**: `404 Not Found`

---

### 4.3 Rechazar candidato ya rechazado → 409

Rechazar dos veces el mismo candidato.

**Respuesta esperada**: `409 Conflict`

---

### 4.4 Re-aplicar después de ser rechazado → 201

Tras rechazar, enviar de nuevo `POST /candidates` con los mismos datos.

**Respuesta esperada**: `201 Created`

---

## 5. Proveedores — Obtener proveedor

**Endpoint**: `GET /suppliers/{duns}`

### 5.1 Obtener proveedor activo → 200

```
GET /suppliers/123456789
```

**Respuesta esperada**: `200 OK`
```json
{
  "name": "Textiles Europa S.L.",
  "duns": 123456789,
  "country": "ES",
  "annualTurnover": 5000000,
  "status": "Active",
  "sustainabilityRating": "A"
}
```

**Tests Postman**:
```javascript
pm.test("Status code is 200", () => pm.response.to.have.status(200));
pm.test("Response has supplier fields", () => {
    const body = pm.response.json();
    pm.expect(body.status).to.be.oneOf(["Active", "Disqualified"]);
    pm.expect(body.sustainabilityRating).to.be.oneOf(["A","B","C","D","E"]);
    pm.expect(body.duns).to.be.a("number");
    pm.expect(body.name).to.be.a("string");
});
```

---

### 5.2 Obtener proveedor ON_PROBATION → 200 con status "Active"

Según las reglas de negocio, el estado ON_PROBATION se muestra como `"Active"` en la API.

**Tests Postman**:
```javascript
pm.test("ON_PROBATION shows as Active", () => {
    pm.expect(pm.response.json().status).to.eql("Active");
});
```

---

### 5.3 Obtener proveedor descalificado → 200 con status "Disqualified"

**Tests Postman**:
```javascript
pm.test("Disqualified supplier shows correct status", () => {
    pm.expect(pm.response.json().status).to.eql("Disqualified");
});
```

---

### 5.4 Obtener proveedor inexistente → 404

```
GET /suppliers/999999999
```

**Respuesta esperada**: `404 Not Found`

---

### 5.5 Obtener proveedor con DUNS que es candidato (no proveedor) → 404

**Respuesta esperada**: `404 Not Found`

---

## 6. Proveedores — Descalificar proveedor

**Endpoint**: `POST /suppliers/{duns}/ban`

### 6.1 Descalificar proveedor ON_PROBATION → 204

```
POST /suppliers/234567890/ban
```

**Respuesta esperada**: `204 No Content`

Verificación posterior: `GET /suppliers/234567890` debe devolver `"status": "Disqualified"`.

---

### 6.2 Descalificar proveedor ACTIVE → 409

Un proveedor activo (rating A o B) no puede ser descalificado.

**Respuesta esperada**: `409 Conflict`

---

### 6.3 Descalificar proveedor ya descalificado → 409

**Respuesta esperada**: `409 Conflict`

---

### 6.4 Descalificar proveedor inexistente → 404

```
POST /suppliers/999999999/ban
```

**Respuesta esperada**: `404 Not Found`

---

### 6.5 Re-aplicar con DUNS de proveedor descalificado → 409

Tras descalificar, intentar `POST /candidates` con el mismo DUNS.

**Respuesta esperada**: `409 Conflict`

---

## 7. Proveedores — Proveedores potenciales

**Endpoint**: `GET /suppliers/potential?rate={rate}&limit={limit}&offset={offset}`

### 7.1 Obtener proveedores potenciales con parámetros válidos → 200

```
GET /suppliers/potential?rate=500000&limit=10&offset=0
```

**Respuesta esperada**: `200 OK`
```json
{
  "data": [
    {
      "name": "...",
      "duns": 123456789,
      "country": "ES",
      "annualTurnover": 5000000,
      "status": "Active",
      "sustainabilityRating": "A",
      "score": 500000.0
    }
  ],
  "pagination": {
    "limit": 10,
    "offset": 0,
    "total": 1
  }
}
```

**Tests Postman**:
```javascript
pm.test("Status code is 200", () => pm.response.to.have.status(200));
pm.test("Response has data and pagination", () => {
    const body = pm.response.json();
    pm.expect(body.data).to.be.an("array");
    pm.expect(body.pagination).to.be.an("object");
    pm.expect(body.pagination.limit).to.be.a("number");
    pm.expect(body.pagination.offset).to.be.a("number");
    pm.expect(body.pagination.total).to.be.a("number");
});
pm.test("Suppliers are sorted by score descending", () => {
    const data = pm.response.json().data;
    for (let i = 1; i < data.length; i++) {
        pm.expect(data[i-1].score).to.be.at.least(data[i].score);
    }
});
pm.test("Each supplier has required fields", () => {
    pm.response.json().data.forEach(s => {
        pm.expect(s.score).to.be.a("number");
        pm.expect(s.duns).to.be.a("number");
        pm.expect(s.name).to.be.a("string");
        pm.expect(s.sustainabilityRating).to.be.oneOf(["A","B","C","D","E"]);
    });
});
```

---

### 7.2 Verificar que proveedores descalificados no aparecen → 200

Crear un proveedor, descalificarlo, y verificar que no aparece en la lista de potenciales.

---

### 7.3 Verificar que solo aparecen proveedores con facturación > rate → 200

Solo los proveedores cuya facturación anual es mayor que el `rate` indicado deben aparecer.

**Tests Postman**:
```javascript
pm.test("All suppliers have turnover > rate", () => {
    const rate = 500000;
    pm.response.json().data.forEach(s => {
        pm.expect(s.annualTurnover).to.be.above(rate);
    });
});
```

---

### 7.4 Verificar cálculo de puntuación → 200

```
Puntuación = facturación × 0.1 × constante_rating
```

**Tests Postman**:
```javascript
pm.test("Score is correctly calculated", () => {
    const ratingConstants = { A: 1.0, B: 0.75, C: 0.5, D: 0.25, E: 0.1 };
    pm.response.json().data.forEach(s => {
        const expectedBase = s.annualTurnover * 0.1 * ratingConstants[s.sustainabilityRating];
        // Score might include 25% bonus, so should be >= base
        pm.expect(s.score).to.be.at.least(expectedBase);
    });
});
```

---

### 7.5 Verificar bonificación del 25% para pequeños proveedores → 200

Proveedores cuya facturación es una de las dos más bajas únicas de su país reciben un 25% extra.

---

### 7.6 Verificar paginación → 200

```
GET /suppliers/potential?rate=500000&limit=2&offset=0
GET /suppliers/potential?rate=500000&limit=2&offset=2
```

**Tests Postman**:
```javascript
pm.test("Pagination respects limit", () => {
    const body = pm.response.json();
    pm.expect(body.data.length).to.be.at.most(body.pagination.limit);
});
```

---

### 7.7 Rate menor que 250 → 400

```
GET /suppliers/potential?rate=100
```

**Respuesta esperada**: `400 Bad Request`

---

### 7.8 Sin parámetro rate → 400

```
GET /suppliers/potential
```

**Respuesta esperada**: `400 Bad Request`

---

## 8. Sostenibilidad — Actualizar calificación

**Endpoint**: `POST /sustainability/update`

### 8.1 Actualizar rating de proveedor activo de A a D → proveedor pasa a ON_PROBATION

```
POST /sustainability/update
Content-Type: application/json

{
  "duns": 123456789,
  "rating": "D"
}
```

Verificación posterior: `GET /suppliers/123456789` → `"status": "Active"` (ON_PROBATION se muestra como Active), `"sustainabilityRating": "D"`.

---

### 8.2 Actualizar rating de proveedor ON_PROBATION (C) a A → proveedor pasa a ACTIVE

```
POST /sustainability/update
Content-Type: application/json

{
  "duns": 234567890,
  "rating": "A"
}
```

Verificación posterior: `GET /suppliers/234567890` → `"sustainabilityRating": "A"`.

---

### 8.3 Actualizar rating de proveedor descalificado → sin efecto o error

---

## 9. Países — Consultar país

**Endpoint**: `GET /countries/{country}`

### 9.1 Consultar país existente y aprobado → 200

```
GET /countries/ES
```

**Respuesta esperada**: `200 OK`
```json
{
  "name": "ES",
  "isBanned": false
}
```

---

### 9.2 Consultar país baneado → 200

```
GET /countries/XX
```

**Respuesta esperada**: `200 OK` con `"isBanned": true`

---

### 9.3 Consultar país inexistente → 404

```
GET /countries/ZZ
```

**Respuesta esperada**: `404 Not Found`

---

## 10. Flujos completos (escenarios E2E)

Estos son flujos ordenados que prueban el ciclo de vida completo del proveedor. Se recomienda ejecutarlos en una colección Postman con orden secuencial.

### Flujo 1: Ciclo de vida feliz (Candidato → Proveedor Activo)

| Paso | Petición | Resultado Esperado |
|------|----------|-------------------|
| 1 | `POST /candidates` con datos válidos | `201 Created` |
| 2 | `GET /candidates/{duns}` | `200 OK` — datos del candidato |
| 3 | `POST /candidates/{duns}/accept` con `"sustainabilityRating": "A"` | `204 No Content` |
| 4 | `GET /suppliers/{duns}` | `200 OK` — `status: "Active"`, `sustainabilityRating: "A"` |
| 5 | `GET /candidates/{duns}` | `404 Not Found` — ya no es candidato |

### Flujo 2: Rechazo y re-aplicación

| Paso | Petición | Resultado Esperado |
|------|----------|-------------------|
| 1 | `POST /candidates` con datos válidos | `201 Created` |
| 2 | `POST /candidates/{duns}/refuse` | `204 No Content` |
| 3 | `GET /candidates/{duns}` | `404 Not Found` — candidato rechazado |
| 4 | `POST /candidates` con mismo DUNS | `201 Created` — re-aplicación exitosa |
| 5 | `POST /candidates/{duns}/accept` con rating `"B"` | `204 No Content` |
| 6 | `GET /suppliers/{duns}` | `200 OK` — `status: "Active"` |

### Flujo 3: Proveedor en prueba → Descalificación

| Paso | Petición | Resultado Esperado |
|------|----------|-------------------|
| 1 | `POST /candidates` con datos válidos | `201 Created` |
| 2 | `POST /candidates/{duns}/accept` con `"sustainabilityRating": "D"` | `204 No Content` |
| 3 | `GET /suppliers/{duns}` | `200 OK` — `status: "Active"` (ON_PROBATION se muestra como Active) |
| 4 | `POST /suppliers/{duns}/ban` | `204 No Content` |
| 5 | `GET /suppliers/{duns}` | `200 OK` — `status: "Disqualified"` |
| 6 | `POST /candidates` con mismo DUNS | `409 Conflict` — proveedor descalificado, no puede re-aplicar |

### Flujo 4: Cambio de rating vía evento de sostenibilidad

| Paso | Petición | Resultado Esperado |
|------|----------|-------------------|
| 1 | `POST /candidates` con datos válidos | `201 Created` |
| 2 | `POST /candidates/{duns}/accept` con `"sustainabilityRating": "A"` | `204 No Content` |
| 3 | `GET /suppliers/{duns}` | `200 OK` — `sustainabilityRating: "A"` |
| 4 | `POST /sustainability/update` con `duns` y `rating: "E"` | `200 OK` o `204` |
| 5 | `GET /suppliers/{duns}` | `200 OK` — `sustainabilityRating: "E"`, `status: "Active"` (ON_PROBATION) |
| 6 | `POST /sustainability/update` con `duns` y `rating: "A"` | `200 OK` o `204` |
| 7 | `GET /suppliers/{duns}` | `200 OK` — `sustainabilityRating: "A"`, `status: "Active"` (ACTIVE real) |

### Flujo 5: Proveedores potenciales

| Paso | Petición | Resultado Esperado |
|------|----------|-------------------|
| 1 | Crear y aceptar 3+ candidatos con diferentes facturaciones y ratings | `201` / `204` |
| 2 | `GET /suppliers/potential?rate=1000000&limit=10&offset=0` | `200 OK` — solo proveedores con facturación > 1M€ |
| 3 | Verificar que los resultados están ordenados por score descendente | Score[0] ≥ Score[1] ≥ ... |
| 4 | Verificar que proveedores descalificados no aparecen | Ningún supplier con status Disqualified |
| 5 | Probar paginación con `limit=1&offset=0` y `limit=1&offset=1` | Resultados diferentes, total consistente |

---

## Resumen de cobertura de pruebas

| Categoría | Nº de pruebas | Cobertura |
|-----------|:-------------:|-----------|
| Crear candidato | 6 | Happy path, duplicado, validaciones, descalificado |
| Obtener candidato | 3 | Existente, inexistente, ya aceptado |
| Aceptar candidato | 7 | Happy path (A/B y C/D/E), país prohibido, facturación baja, inexistente, rating inválido, rechazado |
| Rechazar candidato | 4 | Happy path, inexistente, ya rechazado, re-aplicación |
| Obtener proveedor | 5 | Activo, ON_PROBATION, descalificado, inexistente, es candidato |
| Descalificar proveedor | 5 | Happy path, activo, ya descalificado, inexistente, re-aplicación bloqueada |
| Proveedores potenciales | 8 | Happy path, descalificados excluidos, filtro de facturación, cálculo de score, bonificación, paginación, rate inválido |
| Sostenibilidad | 3 | Cambio A→D, cambio C→A, proveedor descalificado |
| Países | 3 | Aprobado, baneado, inexistente |
| Flujos E2E | 5 | Ciclo completo, rechazo/re-aplicación, descalificación, eventos, potenciales |
| **Total** | **49** | |
