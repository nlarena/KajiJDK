# FZ-006 — El reductor no podía reemplazar por una constante una hoja que **nombra** algo

| campo | valor |
|---|---|
| **estado** | arreglado (2026-08-29) |
| **severidad** | media |
| **encontrado por** | la etapa de `invokeinterface`: un test del reductor que dejó de llegar al piso |
| **afecta** | todo hallazgo minimizado — o sea, todos |

## Qué pasaba

`a_predicate_everything_satisfies_reaches_the_floor` corre el reductor con el predicado `|_| true`:
sin nada que preservar, tiene que aterrizar en el programa más chico que la gramática puede
expresar. Dejó de hacerlo, y quedaba esto:

```java
static int m0() {
    Fz11I q4 = new Fz11B(0);
    return q4.v();
}
```

16 candidatos probados, 16 aceptados, y ahí se plantó. Que probados == aceptados es la firma: no es
que rechazara candidatos, es que **se le acabaron** — ninguno de los que quedaban era más chico
según su propio orden.

## Por qué

El orden del reductor era `(nodos, masa de literales)`. Un candidato se acepta solo si es
**estrictamente** menor.

`Pass::ExprToConstant` ofrece reemplazar `q4.v()` por `0`. Pero `Expr::Virtual` es una **hoja**, igual
que `Expr::IntLit`: mismo conteo de nodos. Y `0` no aporta masa: misma masa de literales. Empate, y
un empate se rechaza.

Consecuencia, que es lo que hace que esto valga un ID: **mientras la referencia queda en pie, la
declaración que lee tampoco es borrable** — `DeleteStatement` produce un programa que no chequea. Una
sola llamada clava el objeto, su argumento de constructor y toda su jerarquía adentro del hallazgo,
para siempre. Y lo mismo vale para `Expr::Field`, `Expr::Var` y `Expr::ArrayLength`.

## Por qué apareció recién ahora

No es una regresión de la etapa de interfaces: es una debilidad que estaba desde que existe el
reductor. Agregar `invokeinterface` movió el flujo del generador de números aleatorios, la semilla 11
pasó a producir un programa con esta forma exacta, y el test lo destapó. Es el mismo patrón que
FZ-004 y FZ-005: **apareció midiendo, no razonando**.

## La primera hipótesis, que era falsa

Que el reductor no visitaba `Expr::Virtual`. Se descarta leyendo `visit_expr`: la hoja recibe `f`
como cualquier otro nodo, solo que no recurre. El candidato **se generaba**; lo que fallaba era la
puerta de peso que viene después. Imprimir el residuo y los contadores (`steps` / `candidates_tried`)
fue lo que separó las dos explicaciones.

## El arreglo

Un tercer componente en el orden: `(nodos, masa de literales, referencias)`, donde *referencias* es
cuántas hojas nombran algo en vez de ser una constante. Constante-por-referencia pasa a ser un paso
estrictamente decreciente, y el orden sigue **bien fundado** porque los tres componentes están
acotados por abajo.

El mensaje de fallo del test también quedó mejor: imprime el programa que sobrevivió y los dos
contadores. Un "no llegó al piso" pelado no distingue "el reductor está roto" de "hay un candidato
que no sabe construir", y esa distinción fue justamente lo que costó.

## Tests

`weight_is_a_well_founded_order` gana la mitad que faltaba: que existan candidatos de
`ExprToConstant` con **el mismo** conteo de nodos y **la misma** masa de literales — o sea, que el
tercer componente sea lo único que los hace un paso. Sin ese aserto, el componente se podría borrar y
el test seguiría verde.
