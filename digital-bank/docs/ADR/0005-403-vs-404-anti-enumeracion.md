# ADR-0005: 403 en vez de 404 para clientes cuando la cuenta no existe

**Fecha:** 16 de septiembre de 2026
**Responsable:** Equipo Digital Bank Backend

## Contexto

Mientras armábamos la consulta de saldo (HU-06), el criterio original decía que si la cuenta no existe, el sistema debía responder siempre 404 ("Cuenta no encontrada"), sin importar quién la consultara. Pero al pensarlo con calma nos dimos cuenta de un problema: si cualquier cliente autenticado puede ver un 404 con solo mandar cualquier número de cuenta, en la práctica le estamos dando una herramienta para ir probando números y, por la respuesta, deducir cuáles cuentas existen de verdad en el sistema — sin necesitar tener permiso sobre ninguna de ellas.

## Alternativas que consideramos

- **404 igual para todos** (como decía el criterio original): es lo más simple y fiel al texto, pero deja abierta la posibilidad de que alguien "enumere" cuentas.
- **403 igual para todos, incluidos administradores**: cierra el problema de enumeración, pero le quita al admin o al cajero la posibilidad de distinguir entre "esta cuenta no existe" y "no tienes permiso", lo cual les complica el trabajo en el día a día del banco.
- **Respuesta distinta según el rol**: 403 para clientes, 404 para el personal autorizado del banco.

## Qué decidimos

Nos fuimos por la respuesta diferenciada: si la cuenta no existe y quien consulta es ADMIN o CAJERO, recibe 404 real. Si quien consulta es un CLIENTE, recibe siempre 403 — el mismo código que recibiría si la cuenta existiera pero fuera de otra persona. Así, un cliente nunca puede diferenciar entre "esa cuenta no existe" y "esa cuenta no es tuya". Esto nos tocó editarlo en el backlog, porque cambiaba directamente el criterio de aceptación 3 de HU-06 tal como estaba escrito al principio.

## Qué implica esto

**A favor:** cerramos la posibilidad de que un cliente enumere cuentas del banco, sin quitarle al personal autorizado la visibilidad que necesita para hacer su trabajo. Además calza con lo que piden los lineamientos de seguridad sobre "fallos seguros" y "mínimo privilegio".

**En contra:** es una regla que no salta a la vista con solo mirar el código HTTP — dos roles distintos reciben respuestas distintas ante la misma situación real (cuenta inexistente), así que tuvimos que dejarlo bien documentado en el criterio de aceptación y en las pruebas, para que no parezca un error o una inconsistencia del sistema.
