# ADR-0004: Flyway para el esquema y CHECK constraints en vez de ENUM nativo

**Fecha:** 14 de septiembre de 2026
**Responsable:** Equipo Digital Bank Backend

## Contexto

Necesitábamos que el esquema de la base de datos fuera cambiando de forma ordenada y que todo el equipo trabajara sobre la misma versión sin sorpresas. Y aparte de eso, teníamos varios campos que solo pueden tomar un conjunto cerrado de valores (`rol`, `estado`, `tipo_cuenta`, el `tipo` de una transacción, etc.), así que tocaba decidir cómo restringirlos.

## Alternativas que consideramos — para el esquema

- **`ddl-auto=update` de Hibernate**: que Hibernate genere y actualice el esquema solo, a partir de las entidades. La descartamos porque no deja ningún historial de qué cambió y cuándo, y cada uno del equipo podría terminar con un esquema ligeramente distinto sin darse cuenta.
- **Flyway**: migraciones en SQL, numeradas y versionadas, que se aplican siempre en el mismo orden y quedan registradas.

## Alternativas que consideramos — para los valores fijos

- **`ENUM` nativo de Postgres**: es más "correcto" a nivel de tipo de dato, pero agregar un valor nuevo después requiere un `ALTER TYPE`, que es una operación más pesada y con restricciones.
- **`VARCHAR` + `CHECK`**: el campo es texto por fuera, pero la base de datos rechaza cualquier valor que no esté en la lista permitida.

## Qué decidimos

Usamos Flyway con `ddl-auto=validate`, o sea que el esquema real de la base de datos siempre manda, y las entidades de Java tienen que coincidir exactamente con él (si no coinciden, la aplicación ni siquiera arranca — nos pasó un par de veces mientras ajustábamos entidades y nos tocó revisar bien). Los campos con valores fijos los dejamos como `VARCHAR` con su `CHECK` correspondiente (por ejemplo `CHECK (rol IN ('CLIENTE','ADMIN','CAJERO'))`), y del lado de Java los manejamos como `enum` para que la validación de tipo pase primero por ahí antes de llegar a la base de datos.

## Qué implica esto

**A favor:** cada cambio de esquema queda en su propio archivo SQL versionado y documentado; si un día toca agregar un rol nuevo o un estado nuevo, es solo una migración más, sin tocar ningún tipo de dato existente; y la validación de los valores permitidos queda en dos partes (aplicación y base de datos), no solo confiando en una.

**En contra:** toca tener cuidado de nunca editar una migración que ya se aplicó (siempre una nueva), y hay que acordarse de mantener sincronizado el `enum` de Java con el `CHECK` de SQL — si alguien agrega un valor en un lado y se le olvida el otro, puede quedar inconsistente.
