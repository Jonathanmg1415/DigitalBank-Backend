# ADR-0003: Separar Cliente y Usuario en dos entidades distintas

**Fecha:** 11 de septiembre de 2026
**Responsable:** Equipo Digital Bank Backend

## Contexto

El sistema tiene que guardar dos tipos de información bien distinta sobre una persona: sus datos de identidad como cliente del banco (nombres, documento, fecha de nacimiento) y sus credenciales para entrar al sistema (correo, contraseña, si está bloqueado o no). Además, el sistema también tiene que soportar personal del banco (administradores y cajeros) que necesitan iniciar sesión pero que no son clientes — no tienen documento de cliente ni cuenta bancaria.

## Alternativas que consideramos

- **Una sola entidad `Cliente` con las credenciales metidas ahí mismo**: era la opción más simple al principio, pero la descartamos porque mezcla dos cosas que no tienen nada que ver entre sí (los datos personales de alguien y cómo entra al sistema), y porque no había forma de tener un ADMIN o un CAJERO sin obligarlo a existir también como cliente.
- **Dos entidades separadas, `Cliente` y `Usuario`, relacionadas entre sí**.

## Qué decidimos

Separamos en `Cliente` (identidad y contacto) y `Usuario` (credenciales, rol, estado de seguridad), con una relación de 1 a 0 o 1 — o sea, un cliente puede tener un usuario asociado o no. El campo `cliente_id` en `Usuario` lo dejamos nullable justo para eso: para que un ADMIN o un CAJERO puedan existir en `Usuario` sin tener fila en `Cliente`. Y le pusimos `UNIQUE(cliente_id)` a esa columna para que un mismo cliente no pueda tener dos cuentas de acceso — en Postgres eso funciona bien porque el `UNIQUE` permite varios `NULL` sin que choquen entre sí, así que los usuarios sin cliente asociado no se estorban.

## Qué implica esto

**A favor:** el modelo refleja bien la diferencia real entre "quién es la persona" y "cómo entra al sistema", nos permitió meter roles de personal del banco sin inventarnos tablas nuevas, y si más adelante queremos aplicar reglas de seguridad distintas a cada tabla, ya está separado.

**En contra:** registrar un cliente nuevo (HU-01) implica insertar en dos tablas a la vez, así que tocó manejar bien la transacción (`@Transactional`) para que no se quede un cliente creado sin sus credenciales si algo falla a mitad de camino.
