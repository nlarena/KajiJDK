// Repro de #301 - `super.m()` sobre una superclase generica no sustituia sus argumentos de tipo.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_301.java
//   bin\run-headless.exe KajiLibrary\repros\finding_301.class lasDosVias
//
// ANTES el archivo NO compilaba:
//
//   error: tipo incompatible en `e`
//       Enumeration<Object> e = super.keys();
//                           ^
//
// Y lo desconcertante es que la MISMA llamada por `this` andaba. La diferencia estaba en el tipo
// que se le daba al receptor:
//
//   this   ->  Caja<Object>              con sus argumentos
//   super  ->  Hashtable                 CRUDO, sin ellos
//
// Con el crudo, el retorno `Enumeration<K>` de `keys()` sale con la `K` sin sustituir, y asignarlo
// a un `Enumeration<Object>` no tipa.
//
// CAUSA: el tipo de `super` salia de `table.super_class(clase)`, que devuelve el **simbolo** de la
// superclase y nada mas. El supertipo ya instanciado --`Hashtable<Object,Object>`-- esta en otro
// lado: en el `Resolved::Class` de la propia clase, que es donde `enter` lo dejo resuelto.
//
// AHORA sale de ahi, con el crudo como respaldo para cuando no hay `Resolved`.
//
// Aparecio escribiendo `java.util.Properties`, que extiende `Hashtable<Object,Object>` y recorre
// sus claves. El rodeo era escribir `this.keys()`, que en ese caso da lo mismo porque `keys` no
// esta sobreescrito -- pero que no es un rodeo valido en general: si la subclase SI sobreescribe el
// metodo, `this` y `super` llaman a cosas distintas.
//
// `lasDosVias` -> 11, `conCampo` -> 7, `sobreescrito` -> 12.
import java.util.Enumeration;
import java.util.Hashtable;

public class finding_301 extends Hashtable<Object, Object> {

    // El caso del finding: el mismo metodo por las dos vias.
    public static int lasDosVias() {
        finding_301 c = new finding_301();
        c.put("a", "b");
        Enumeration<Object> porThis = c.keys();
        Enumeration<Object> porSuper = c.desdeSuper();
        return (porThis != null ? 10 : 0) + (porSuper != null ? 1 : 0);
    }

    Enumeration<Object> desdeSuper() {
        return super.keys();
    }

    // Un metodo cuyo RETORNO es la variable de tipo, no un generico que la contiene.
    public static int conCampo() {
        finding_301 c = new finding_301();
        c.put("k", "v");
        Object v = c.desdeSuperGet("k");
        return v.equals("v") ? 7 : 0;
    }

    Object desdeSuperGet(Object k) {
        return super.get(k);
    }

    // Y el control que muestra por que el rodeo con `this` no era equivalente: aca `size` SI esta
    // sobreescrito, asi que `this.size()` y `super.size()` dan distinto a proposito.
    public int size() {
        return super.size() + 10;
    }

    public static int sobreescrito() {
        finding_301 c = new finding_301();
        c.put("a", "b");
        c.put("c", "d");
        return c.size();
    }
}
