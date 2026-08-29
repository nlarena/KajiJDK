// Repro de #261 - un array NO era un tipo referencia para la resolucion, y una llamada sin
// resolver se emitia como NADA.
//
// Tres agujeros encadenados, del mas profundo al mas visible:
//
//   1. `types::is_subtype` no le daba supertipos a un array. JLS 4.10.3: los supertipos directos
//      de `T[]` son `Object`, `Cloneable` y `java.io.Serializable`. Sin eso, `char[] <: Object`
//      era falso.
//   2. `attribute::is_reference` no incluia `RType::Array`, asi que `assignable` y `convertible`
//      ni siquiera llegaban a consultar el subtipado: caian al `_ => false`.
//   3. Y cuando la resolucion fallaba sobre un tipo EXTERNO, la pasada 2 callaba a proposito
//      (no modelamos toda firma del JDK) y el codegen, sin binding, hacia `return` en silencio:
//      los argumentos ya estaban empujados, asi que el metodo seguia con la pila corrida.
//      `s.length(1, 2)` compilaba a `iconst_1; iconst_2; ireturn` -- sin receptor, sin llamada
//      y sin un solo diagnostico.
//
// El sintoma real: `System.arraycopy(Object, int, Object, int, int)` NO resolvia, y once fuentes
// de KajiLibrary (StringBuilder, ArrayList, los Buffered*, ...) se compilaban mudas y rotas.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_261.java
//   bin\run-headless.exe KajiLibrary\repros\finding_261.class run     -> 0
public class finding_261 {

    static int recibeObject(Object o) {
        return 1;
    }

    public static int run() {
        char[] c = new char[2];
        Object[] o = new Object[2];
        int[] i = new int[2];

        /* 1. Un array pasa a un parametro `Object` (JLS 5.3 sobre 4.10.3). Es la firma de
              System.arraycopy, y por esto la biblioteca entera no podia copiar un array. */
        if (finding_261.recibeObject(c) + finding_261.recibeObject(o)
                + finding_261.recibeObject(i) != 3) {
            return 1;
        }

        /* 2. Y en posicion de asignacion, a las tres raices de un array. */
        Object comoObjeto = c;
        if (comoObjeto == null) {
            return 2;
        }
        java.lang.Cloneable comoCloneable = c;
        if (comoCloneable == null) {
            return 3;
        }
        java.io.Serializable comoSerializable = c;
        if (comoSerializable == null) {
            return 4;
        }

        /* 3. La copia en si: el destino declara `Object` y recibe un `char[]`. */
        char[] destino = new char[4];
        destino[0] = 'k';
        System.arraycopy(c, 0, destino, 2, 2);
        if (destino[0] != 'k') {
            return 5;
        }

        /* 4. `unArray.clone()` es el UNICO miembro de un array cuyo tipo no sale de `Object`:
              devuelve el propio tipo array, no `Object` (JLS 10.7). Es lo que hace tipar el
              `values()` de cualquier enum, que es `return $VALUES.clone();`. */
        char[] copia = c.clone();
        if (copia == null || copia.length != 2) {
            return 6;
        }
        return 0;
    }
}
