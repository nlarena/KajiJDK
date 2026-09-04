# FZ-013 — Un comodín en el escáner de strings dejó 252 de 1200 semillas sin compilar

**Estado:** arreglado · **Encontrado:** 2026-09-02 · **Lo encontró:** la corrida larga de K7

## El caso

Con **toda** la gramática prendida, la corrida larga bajó de 100% a **77% de semillas usables**.
Doscientas cincuenta y dos fallaron así:

```
generator emitted source javac rejects: Fz954.java:26: error: cannot find symbol
```

Y la línea 26 es:

```java
static int rec(int d, int a) {
    if (d <= 0) { return a; }
    return ((a * 31) + Fz954.rec(d - 1, Fz954.rec(2, Fz954.ssame(("true" + "0"), new String("kaji")))));
}
```

`ssame` se **usa** dos veces y no se **emite** ni una.

## La causa

`ssame` sale sólo si el programa compara strings por identidad, y eso lo decide
`JavaProgram::compares_strings`. Ese escáner sí visita `recursive_body` —eso ya estaba— pero su
`in_expr` terminaba en un **comodín**:

```rust
Expr::Call(_, args, _) => args.iter().any(in_expr),
_ => false,
```

`Expr::Recurse` cae ahí. Y también `RawBitsHigh`, `MatrixLoad`, `MatrixRowLength`. O sea que
cualquier `ssame` metido adentro de una llamada recursiva era invisible.

## Por qué es la misma historia de siempre

El comodín es el bug, no el brazo que falta. Cada vez que agregué un nodo nuevo a `Expr` —matrices,
NaN, recursión— este `match` siguió compilando sin decir nada, y el nodo nuevo quedó del lado de
«esto no contiene nada interesante» por omisión.

Un `match` **exhaustivo** habría roto la compilación el día que se agregó cada uno, que es
exactamente lo que uno quiere de un escáner: que una forma nueva no pueda entrar sin que alguien
decida si la visita o no. Es la misma familia que FZ-004 y FZ-005 —parece estar probando algo y no
lo está— con la diferencia de que acá el síntoma sí era visible, sólo que en un número que ninguna
campaña corta miraba: el 100% de usables.

## El arreglo

El comodín se fue. Los brazos que descienden ahora incluyen `Recurse`, `RawBitsHigh`,
`MatrixRowLength` y `MatrixLoad`, y las hojas de verdad —literales, `Var`, `Field`, `ThroughRef`,
`Virtual`, `ArrayLength`, `NanLit`— están enumeradas una por una.

## Lo que queda

Los otros escáneres del mismo archivo (`obj_use`, `classifies`, `uses_matrices`…) tienen la misma
forma y hay que revisarlos igual. Este apareció porque `recursion_share` y `string_share` estuvieron
prendidos a la vez por primera vez; los demás esperan a la combinación que los saque.
