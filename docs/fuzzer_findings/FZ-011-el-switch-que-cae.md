# FZ-011 — Un `switch` cuyo último arm **cae** al `default` no se cuenta como salida

**Estado:** arreglado · **Encontrado:** 2026-08-30 · **Lo encontró:** la corrida larga de K7

## El caso

Una semilla de 2000, en la primera corrida larga:

```
unusable x1: generator emitted source javac rejects:
  kaji-fuzz-long\Fz276.java:111: error: unreachable statement
```

El programa:

```java
switch (…) {
case 99: { …; continue; }
case 44: {
    …
    for (int i26 = 0; i26 < 3; i26++) { … }   // no termina abruptamente
}
default: { …; continue; }
}
long v29 = …;                                  // ← inalcanzable, y javac tiene razón
```

## La causa

`Stmt::completes_abruptly` miraba **cada arm por su cuenta**: exigía que todos salieran. `case 44`
termina en un `for`, así que no sale — y el predicado concluía que el `switch` entero podía completar
normalmente, se emitía la sentencia de abajo, y `javac` la rechazaba.

Pero `case 44` no necesita salir: **cae** al `default`, que hace `continue`. En Java el fall-through
es parte del significado del `switch`, y el predicado lo estaba ignorando.

La regla correcta tiene dos formas de estar bien, no una:

- el arm **sale del bloque** — termina abruptamente y no por un `break` pelado, que deja el `switch`
  y sigue abajo;
- o el arm **cae al siguiente** — no termina abruptamente, así que el control sigue más abajo.

Con el `default` emitido último, todo lo que cae termina llegando ahí, así que alcanza con exigir
que el `default` salga y que ningún arm se vaya por un `break` pelado.

## Por qué tardó tanto en aparecer

Necesita tres cosas a la vez: un `switch` con `default`, un arm que **no** termine abruptamente, y
que todos los demás caminos salgan. Con `jump_share` y `switch_share` en sus valores por defecto eso
es raro — una de cada 2000, medido. Los censos de `switch` prueban las formas de a una; ésta es la
combinación.

Es el argumento entero a favor de las corridas largas: no encontró una divergencia de la VM, encontró
un hueco del generador que 300 semillas no alcanzan a producir.
