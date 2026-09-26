# ADR-0001: Monolito modular como estilo arquitectónico

**Fecha:** 2 de septiembre de 2026
**Responsable:** Equipo Digital Bank Backend

## Contexto

Desde el arranque del proyecto teníamos claro que el backend iba a tener varios dominios distintos (autenticación, clientes, cuentas, transacciones), y que esos dominios iban a ir creciendo sprint tras sprint. El reto era escoger una arquitectura que nos dejara separar bien las responsabilidades sin meternos en la complejidad de microservicios, que para un proyecto de un solo semestre y con el equipo que tenemos no tenía sentido (habría que estar orquestando varios servicios, manejar comunicación entre ellos, desplegar cada uno por separado, etc.).

## Alternativas que consideramos

- **Microservicios**: un servicio aparte por cada dominio. La descartamos de una porque la sobrecarga de manejar varios despliegues, comunicación por red entre servicios y consistencia distribuida no daba con el tiempo que teníamos para el sprint.
- **Capas simples sin módulos**: un solo paquete `controller`/`service`/`repository` para todo el proyecto, mezclando todos los dominios. La descartamos porque terminaría siendo un solo bloque de código difícil de mantener entre varios, sin fronteras claras.
- **Monolito modular**: un solo desplegable, pero organizado internamente por módulos según el dominio de negocio.

## Qué decidimos

Nos fuimos por monolito modular. El código quedó organizado en módulos por dominio (`auth`, `cliente`, `cuenta` y `core` como módulo transversal), y cada módulo tiene sus propias capas por dentro (`controller`, `dto`, `entity`, `exception`, `interfaces`, `repository`, `service`). La regla que nos impusimos: un módulo solo puede hablar con otro a través de su paquete `interfaces` — nunca importando directamente la entidad, el repositorio o el service de otro módulo por dentro.

## Qué implica esto

**A favor:** cada uno pudo trabajar en su módulo sin pisarse con los demás, un solo despliegue nos simplificó la vida en Render, y si en algún momento el proyecto crece más de lo que da un semestre, la estructura ya queda lista para separar en microservicios sin rehacer todo desde cero.

**En contra:** toca ser disciplinados para no romper la regla de módulos — de hecho en el camino nos pasó (por ejemplo `CuentaServiceImpl` terminó importando directamente clases internas de `cliente` en vez de pasar por una interfaz), y quedó identificado en revisión de PR como algo pendiente por corregir en el sprint 2. También significa que cada vez que un módulo necesita algo de otro, toca crear una interfaz nueva (como pasó con `UsuarioQueryService`), lo cual es un paso extra pero vale la pena por la organización que da.
