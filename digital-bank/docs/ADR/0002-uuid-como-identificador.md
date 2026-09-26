# ADR-0002: UUID como identificador único en vez de entero autoincremental

**Fecha:** 11 de septiembre de 2026
**Responsable:** Equipo Digital Bank Backend

## Contexto

Todas las entidades principales (`Cliente`, `Usuario`, `Cuenta`, `Transaccion`) necesitan una llave primaria. Como esos IDs los va a exponer la API pública, no era solo una decisión de "cómo guardo el dato" sino que también tenía que ver con seguridad y con cómo se ve la API desde afuera.

## Alternativas que consideramos

- **Entero autoincremental (1, 2, 3...)**: más simple y más rápido de indexar en Postgres porque queda ordenado secuencialmente. Lo descartamos porque es enumerable — cualquiera podría ir probando `/api/clientes/1`, `/api/clientes/2`, etc., y de paso deducir cuántos clientes tiene el banco.
- **UUID**: no se puede adivinar, no necesita que nada esté coordinado para que no se repita, y es lo que se usa normalmente hoy en día en APIs REST.

## Qué decidimos

Usamos UUID v4 como llave primaria en todas las entidades. Lo genera la misma base de datos con `gen_random_uuid()` (que ya viene nativo desde Postgres 13, sin tener que instalar ninguna extensión aparte) como valor por defecto de la columna.

## Qué implica esto

**A favor:** nadie puede enumerar los recursos del sistema ni deducir cuántos registros hay, no hay que coordinar nada entre módulos (o entre futuros servicios) para que los IDs no choquen, y es el estándar que cualquiera espera ver en una API moderna.

**En contra:** un UUID v4 aleatorio hace que el índice de la llave primaria quede más fragmentado que con un entero secuencial, porque los inserts no caen ordenados físicamente. Para el volumen de datos que maneja un proyecto de este tamaño eso no pesa en nada, pero lo dejamos anotado como algo que sí importaría si el proyecto creciera muchísimo — fue una decisión consciente, no algo que se nos pasó por alto.
