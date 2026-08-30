// Repro de #291 - `java.lang.Iterable` no tenia `forEach`.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_291.java
//   bin\run-headless.exe KajiLibrary\repros\finding_291.class suma
//
// ANTES: no compilaba nada de esto.
//
//   error: no se encuentra el metodo: forEach
//     simbolo:   metodo forEach(Consumer<...>)
//     ubicacion: clase Collection<E>
//
// Era el unico miembro publico de `Iterable` que faltaba, y como `Collection extends Iterable`, el
// efecto no era local: NINGUNA coleccion de la biblioteca tenia `forEach`. Ni ArrayList, ni
// HashSet, ni ArrayDeque -- ninguna de las quince.
//
// De paso: `LinkedBlockingDeque` y `LinkedTransferQueue` ya declaraban `public void
// forEach(Consumer<? super E>)` creyendo que sobreescribian algo. No sobreescribian nada.
//
// AHORA: `default void forEach(Consumer<? super T>)` en `Iterable`, recorriendo el iterador. Va
// como default y no como abstracto porque declararlo abstracto obligaria a escribirlo en cada
// implementor, y el cuerpo seria este mismo.
//
// `suma` -> 6, `sumaConjunto` -> 6, `sumaCola` -> 6, `orden` -> 123.
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class finding_291 {

    // El caso que fallaba: forEach sobre una List.
    public static int suma() {
        List<Integer> l = new ArrayList<Integer>();
        l.add(Integer.valueOf(1));
        l.add(Integer.valueOf(2));
        l.add(Integer.valueOf(3));
        Acumulador a = new Acumulador();
        l.forEach(a);
        return a.total;
    }

    // Un Set, para dejar dicho que el default se hereda por Collection y no por List.
    public static int sumaConjunto() {
        Set<Integer> s = new TreeSet<Integer>();
        s.add(Integer.valueOf(1));
        s.add(Integer.valueOf(2));
        s.add(Integer.valueOf(3));
        Acumulador a = new Acumulador();
        s.forEach(a);
        return a.total;
    }

    // Y un Deque, que llega a Iterable por otra rama de la jerarquia.
    public static int sumaCola() {
        Deque<Integer> d = new ArrayDeque<Integer>();
        d.addLast(Integer.valueOf(1));
        d.addLast(Integer.valueOf(2));
        d.addLast(Integer.valueOf(3));
        Acumulador a = new Acumulador();
        d.forEach(a);
        return a.total;
    }

    // El contrato dice "en el orden en que los da el iterador", asi que sobre una List el orden
    // tiene que ser el de la lista. 1, 2, 3 -> 123.
    public static int orden() {
        List<Integer> l = new ArrayList<Integer>();
        l.add(Integer.valueOf(1));
        l.add(Integer.valueOf(2));
        l.add(Integer.valueOf(3));
        Acumulador a = new Acumulador();
        l.forEach(a);
        return a.digitos;
    }
}

class Acumulador implements java.util.function.Consumer<Integer> {

    int total;
    int digitos;

    public void accept(Integer x) {
        this.total = this.total + x.intValue();
        this.digitos = this.digitos * 10 + x.intValue();
    }
}
