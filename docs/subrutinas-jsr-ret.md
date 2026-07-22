# Subrutinas: por qué **no** implementamos `jsr` / `ret` / `jsr_w`

> Nota de diseño (A6). Los tres únicos opcodes del JVMS que la VM decide **no ejecutar**.
> No son deuda técnica ni un pendiente que se nos quedó: son una exclusión deliberada, y
> este documento existe para que dentro de seis meses no se relean como un agujero.

## Qué son

Las **subrutinas** son el mecanismo con el que las JVM viejas compilaban `finally` sin
duplicar código:

- **`jsr` (0xa8)** salta a la subrutina y empuja en la pila de operandos un valor de tipo
  `returnAddress`: la dirección de la instrucción *siguiente* al `jsr`.
- La subrutina guarda esa dirección en un local (`astore`).
- **`ret` (0xa9)** salta a la dirección guardada en ese local.
- **`jsr_w` (0xc9)** es lo mismo con offset de 4 bytes. Existe además la forma
  `wide ret`, que nuestro desensamblador renderiza como `ret_w`.

El caso de uso era exactamente uno: un bloque `finally` tiene que ejecutarse por la
salida normal **y** por cada camino de excepción. Con subrutinas se emitía el bloque una
sola vez y cada camino hacía `jsr` hacia él.

## Por qué el mundo las abandonó

No por gusto: **son casi imposibles de verificar**.

Una subrutina puede invocarse desde varios sitios donde los locales tienen **tipos
distintos**. El verificador de bytecode asigna un tipo por local y por instrucción, pero
una subrutina polimórfica rompe esa premisa: el local 3 puede ser un `int` cuando se
llega desde un `jsr` y una referencia cuando se llega desde otro. Verificarlas bien
exige un análisis de flujo *consciente de subrutinas* — saber qué locales toca cada una,
fusionar sólo esos y preservar el resto del llamador, y emparejar cada `ret` con su
`jsr`. Es, con diferencia, la parte más difícil del verificador del JVMS.

Cuando Java 6 introdujo el `StackMapTable` y el verificador **por type-checking**
(JVMS §4.10.1) —una pasada, sin punto fijo, un frame declarado por punto de mezcla— las
subrutinas quedaron directamente fuera del modelo. La especificación lo resolvió
prohibiéndolas:

> **JVMS §4.9.1** — `jsr` y `jsr_w` no pueden aparecer en el `code` de un class file de
> versión **50.0 o superior**.

La versión 50.0 es **Java 6**. Desde entonces `javac` compila `finally` **duplicando** el
bloque en cada camino de salida, que es más bytecode pero trivialmente verificable.

**Consecuencia práctica: ningún `.class` producido después de 2006 puede contenerlos
legalmente.** Ejecutarlos sólo sirve para correr bytecode anterior a esa fecha.

## La decisión: leer sí, ejecutar no

El proyecto ya tiene la división correcta, y conviene verla explícita porque es
asimétrica **a propósito**:

| Capa | ¿Los soporta? | Por qué |
|---|---|---|
| Desensamblador (`opcode.rs`) | ✅ **Sí** — `jsr`, `ret`, `jsr_w`, `ret_w` | Un `javap` tiene que renderizar **cualquier** class file legal, incluidos los v49 y anteriores. El hito A0 se mide contra `javap -v` sobre `java.base`: si no los conociera, sería incorrecto |
| Verificador (`structural_check`) | ⛔ **Los rechaza** | `verifier.rs` los corta en el gate estructural §4.9, antes del type-checking |
| Intérprete | ⛔ **No los despacha** | Caen en el `todo!()`; y `wide ret` tiene un `panic!` explícito en el brazo de `0xc4` |

Es decir: **sabemos leer bytecode legacy, y nos negamos a ejecutarlo.** Esa es la
postura, y es coherente con lo que la propia especificación decidió.

## Qué costaría implementarlos

Vale la pena dejarlo escrito, porque el costo está donde no se lo espera.

**Ejecutarlos sería lo más fácil que queda del set** — unas 10 líneas entre los tres:
`jsr` empuja la dirección de retorno y salta; `ret` salta a la dirección del local. Es
más simple que `multianewarray`.

El costo real es otro:

1. **`Value` necesitaría una variante `ReturnAddress(usize)`**, y eso toca **todos** los
   `match` exhaustivos sobre `Value` del proyecto. Además es un tipo con reglas propias:
   no se puede guardar en un campo ni en un array, no se puede operar aritméticamente, y
   el verificador debe impedir que se lo confunda con una referencia.
2. **Habría que verificar subrutinas**, que es el problema descrito arriba. Nuestro
   verificador ya corre por dos caminos (chequeo contra `StackMapTable` **e** inferencia
   por punto fijo como fallback); el soporte de subrutinas caería sobre el segundo, que
   es justamente el más delicado.

Gastar eso para ejecutar bytecode que ningún compilador emite desde hace veinte años es
un mal negocio. **Rechazarlos no es una limitación: es la misma decisión que tomó el
JVMS.**

## Qué haría cambiar la decisión

Una sola cosa: querer ejecutar **class files anteriores a Java 6**. Si en algún momento
el objetivo incluye correr artefactos legacy —un `.jar` viejo, bytecode de un compilador
de los 90— entonces sí hay que pagar los dos costos de arriba.

Mientras el alcance sea *bytecode moderno* (propio o ajeno), esto queda cerrado.

## Cómo contarlo

Con esto, la cobertura del set de opcodes se lee así:

```
202  opcodes del JVMS
  3  excluidos por diseño   (jsr 0xa8 · ret 0xa9 · jsr_w 0xc9)  ← este documento
───
199  alcanzables
199  implementados                                             ← completo
```

El número honesto es **199/199**, no 199/202. Contar los tres legacy como faltantes
mezcla una decisión con una tarea, y hace parecer incompleto algo que está cerrado.

## Estado

- ✅ Decisión tomada y aplicada: `structural_check` los rechaza; el intérprete no los
  despacha.
- ✅ El desensamblador los soporta completo (requisito de A0).
- ⏭️ Se revisita **sólo** si el alcance pasa a incluir bytecode anterior a Java 6.
