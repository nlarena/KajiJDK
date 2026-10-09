// Repro de #302 - el hash de identidad cambiaba cuando el recolector movia el objeto.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_302.java
//   bin\run-headless.exe KajiLibrary\repros\finding_302.class hashEstable
//
// ANTES:
//
//   hashEstable()        -> 0     (el hash cambio)
//   hashCodeEstable()    -> 0     (idem por Object.hashCode)
//
// AHORA los dos dan 1, que es lo que da `java` real.
//
// EL MECANISMO, y es de una linea:
//
//   ("java/lang/Object", "hashCode", "()I") => Some(Value::Int(reference(&args[0]) as i32))
//
// El offset del objeto en el heap **es** su identidad, y por eso servia como respuesta. Lo que no
// es, es estable: el recolector joven es copiador, y mueve los objetos que sobreviven. Despues de
// una colecta menor el mismo objeto vive en otro offset, y su hash cambio.
//
// Eso rompe el contrato de `Object.hashCode()`, que pide que el valor sea el mismo mientras el
// objeto viva -- y de ese contrato cuelgan `HashMap` y `HashSet`. Una clave puesta antes de una
// colecta puede no encontrarse despues.
//
// COMO APARECIO, que es lo que lo hace valioso: escribiendo `EnumMap(Map)`. La prueba de
// comportamiento daba un resultado **no monotono** -- una version con menos codigo antes fallaba y
// una con mas andaba -- y eso no lo explica ningun bug de logica. El sintoma concreto era que
// `HashMap.get(Color.AZUL)` devolvia null adentro del constructor, con la misma clave con la que se
// habia hecho el `put` tres lineas arriba. Lo unico que habia cambiado en el medio eran unas
// asignaciones.
//
// AHORA: el hash se calcula **a demanda** la primera vez que se lo pide y se **guarda** en la
// palabra de marca del encabezado, en los bits 1..31; el bit 0 se lo queda el marcado del GC. El
// `evacuate_block` copia el encabezado con el resto del objeto, asi que el hash viaja con el.
//
// Es el mismo arreglo que hace HotSpot y por las mismas dos razones: la mayoria de los objetos no
// piden su hash nunca (calcularlo al crear seria trabajo tirado), y el encabezado es lo unico que
// se mueve junto con el objeto.
//
// El precio: `set_mark` y `clear_all_marks` ya no pueden escribir la palabra entera. Escribian `1`
// y `0`; ahora tocan solo el bit 0. Sin eso, cada marcado borraria el hash de todo objeto vivo --
// que es exactamente el bug que se estaba arreglando, con otro disparador.
//
// `hashEstable` -> 1, `hashCodeEstable` -> 1, `mapaSobrevive` -> 1, `identidadSigueSiendoUnica` -> 1.
import java.util.HashMap;
import java.util.Map;

public class finding_302 {

    private static void ensuciar() {
        for (int i = 0; i < 200000; i++) {
            Object basura = new Object();
        }
    }

    // El caso del finding: el hash de identidad, antes y despues de mover el objeto.
    public static int hashEstable() {
        Object o = new Object();
        int a = System.identityHashCode(o);
        ensuciar();
        int b = System.identityHashCode(o);
        return a == b ? 1 : 0;
    }

    // Lo mismo por la via de Object.hashCode(), que es la que usan las colecciones.
    public static int hashCodeEstable() {
        Object o = new Object();
        int a = o.hashCode();
        ensuciar();
        int b = o.hashCode();
        return a == b ? 1 : 0;
    }

    // La consecuencia practica: una clave con hash de identidad se sigue encontrando.
    public static int mapaSobrevive() {
        Map<Object, String> m = new HashMap<Object, String>();
        Object clave = new Object();
        m.put(clave, "valor");
        ensuciar();
        return m.get(clave) == null ? 0 : 1;
    }

    // Y el control del otro lado: que el hash sea estable no lo vuelve constante. Dos objetos
    // distintos siguen teniendo, casi siempre, hashes distintos -- si todos dieran lo mismo, el
    // hash seria "estable" y completamente inutil.
    public static int identidadSigueSiendoUnica() {
        int distintos = 0;
        int[] hashes = new int[100];
        for (int i = 0; i < 100; i++) {
            hashes[i] = new Object().hashCode();
        }
        for (int i = 0; i < 100; i++) {
            boolean repetido = false;
            for (int j = 0; j < i; j++) {
                if (hashes[j] == hashes[i]) {
                    repetido = true;
                }
            }
            if (!repetido) {
                distintos = distintos + 1;
            }
        }
        // Se pide holgadamente que la mayoria sean distintos, no que lo sean todos: dos hashes
        // iguales entre cien objetos es una colision legitima y no un bug.
        return distintos > 90 ? 1 : 0;
    }
}
