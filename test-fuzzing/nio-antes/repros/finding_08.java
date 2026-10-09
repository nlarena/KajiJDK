// Repro de #08 - faltaba el chequeo de completitud de metodos abstractos (JLS 8.1.1.1).
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_08.java
//
// ANTES: una clase CONCRETA con metodos abstractos heredados sin implementar compilaba igual. El
// javac real la rechaza. Peor que un error: escondia errores reales, que reaparecian como
// AbstractMethodError en runtime.
//
// AHORA: **la rechaza**, y que este archivo NO compile es justamente la prueba:
//
//   error: `P` no es abstracta y no implementa `get` de `List`
//
// Este repro se lee al reves que los demas: si algun dia vuelve a compilar, el chequeo se
// perdio.
//
// ALCANCE, y es importante: el chequeo cubre los metodos de una INTERFAZ y los de una superclase
// de la MISMA unidad de compilacion, pero **no** los de una superclase que viene del classpath.
// Ese hueco esta abierto y documentado en #284, con su propio repro.
package java.util;

import java.util.List;

public class P<E> implements List<E> {
    public int size() { return 0; }
}
