// Repro de #290 - `Arrays.compare(int[], int[])` se declaraba ambigua, y `Arrays.copyOf(int[],
// int)` elegia la sobrecarga equivocada en silencio.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_290.java
//
// ANTES tenia DOS caras, y la segunda era la peligrosa:
//
//   Arrays.compare(x, y)    error: la referencia a `compare` es ambigua
//   Arrays.copyOf(x, 3)     COMPILA, y elige `<T> T[] copyOf(T[], int)` en vez de la de `int[]`
//
// Lo que emitia para el segundo:
//
//   javac del JDK 25                    el nuestro, antes
//   ---------------------------------   ------------------------------------------
//   invokestatic copyOf:([II)[I         invokestatic copyOf:([Ljava/lang/Object;I)[Ljava/lang/Object;
//                                       checkcast "[Ljava/lang/Integer;"
//
// O sea: infirio `T = Integer` para un `int[]`, llamo a la sobrecarga de `Object[]` y le puso
// encima un cast a `Integer[]`. Compilaba, y reventaba en el primer `aastore` sobre lo que en
// realidad seguia siendo un `int[]`:
//
//   array_operations.rs:558: aastore: value header does not point at a known class
//
// CAUSA: un arreglo de primitivos se consideraba convertible a `T[]`. Dos lugares, los dos en
// `attribute.rs`, y el mismo error en los dos: el brazo `(Array(a), Array(b))` recursaba sobre
// los elementos sin mirar que uno fuera primitivo, y abajo la recursion caia en el brazo de
// **boxing** (`int` -> `Integer` -> `T`) o en la **indulgencia** con las variables de metodo
// (`lenient(to)`), que existe para que la inferencia las resuelva despues.
//
// Adentro de un arreglo ninguna de las dos corresponde: la covarianza del §4.10.3 vale solo entre
// tipos referencia, y una variable de tipo solo liga tipos referencia (§4.5.1). `int[]` es
// asignable a `int[]` y a `Object`, y a nada mas.
//
// AHORA: arreglado en `convertible` y en `assignable` — si alguno de los dos elementos es
// primitivo, tienen que ser el MISMO primitivo. `copyOf(int[], int)` emite `([II)[I`, identico al
// JDK, y `compare(int[], int[])` compila.
//
// De paso: `applicable_by_inference` tambien reducia `int[] -> a[]` boxeando. Se cerro tambien
// (`infer.rs`, marca `falso` en el BoundSet), aunque por si solo no alcanzaba — el filtro de
// inferencia no corre sobre clases del classpath, que es justo el caso de `java.util.Arrays`.
//
// Familia de #279 vista desde el otro lado: alli el argumento era `T[]` y no se elegia ninguno;
// aca era un `int[]` al que se le ofrecia un candidato `T[]` que no deberia ser aplicable.
//
// Queda como REGRESION. Los nueve controles siguen porque acotan que el arreglo no rompio las
// otras familias.
import java.util.Arrays;

public class finding_290 {

    // El caso que fallaba: se declaraba ambigua.
    public static int ambigua() {
        int[] x = { 1, 2 };
        int[] y = { 1, 2, 3 };
        return Arrays.compare(x, y);
    }

    // La forma de rango, que nunca fue ambigua. Se conserva como control.
    public static int rodeo() {
        int[] x = { 1, 2 };
        int[] y = { 1, 2, 3 };
        return Arrays.compare(x, 0, x.length, y, 0, y.length);
    }

    // Controles: las otras nueve familias con el mismo argumento compilan.
    public static int controles() {
        int[] x = { 1, 2 };
        int[] y = { 1, 2, 3 };
        int r = 0;
        r = r + Arrays.compareUnsigned(x, y);
        r = r + Arrays.mismatch(x, y);
        r = r + (Arrays.equals(x, y) ? 1 : 0);
        r = r + Arrays.binarySearch(x, 2);
        r = r + Arrays.hashCode(x);
        r = r + Arrays.toString(x).length();
        r = r + Arrays.copyOf(x, 1).length;
        Arrays.sort(x);
        Arrays.fill(x, 0);
        return r;
    }
}
