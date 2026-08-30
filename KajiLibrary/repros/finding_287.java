// Repro de #287 - una llamada estatica CALIFICADA hecha desde dentro de una interfaz que declara
// un homonimo se resuelve contra la interfaz, ignorando el calificador. Compila en silencio y
// recursa para siempre.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_287.java
//   bin\jvm.exe -v KajiLibrary\repros\finding_287.class
//
// Lo que emite cada compilador para el cuerpo de `of()`:
//
//   javac del JDK 25                          el nuestro
//   ---------------------------------------   ------------------------------------------
//   0: anewarray Object                       0: anewarray Object
//   4: iconst_0                               4: iconst_0
//   5: invokestatic Ayudante.of([Object;I)    5: invokestatic Integer.valueOf(I)   <-- boxea
//   8: areturn                                8: invokestatic of:(Object;Object;)  <-- LA PROPIA
//                                            11: areturn
//
// El calificador `Ayudante.` se ignora: el compilador elige `of(E, E)` de la interfaz misma,
// boxeando el `int` para que encaje. Como `of(E, E)` vuelve a llamar a `Ayudante.of(...)`, que
// vuelve a resolver a `of(E, E)`, el resultado en runtime es StackOverflowError.
//
// El caso ANALOGO entre dos clases resuelve bien: una clase con `f(Object, Object)` que llama a
// `Otra.f(Object[], int)` emite la llamada correcta. Lo que lo dispara es que el que llama sea
// una **interfaz** con estaticos homonimos.
//
// Es el modo de falla mas caro: no hay error de compilacion, el bytecode es valido, y el sintoma
// —un StackOverflowError— no menciona ni la interfaz ni el metodo que se quiso llamar.
//
// Como salio: `Set.of()` y `Map.of()` de la biblioteca. Sus cuerpos llaman a `FixedSet.of(...)` y
// `FixedMap.of(...)`, ayudantes package-private con firma propia; los dos terminaban llamandose a
// si mismos. `List.of` NO se vio afectado, y la razon es iluminadora: su cuerpo usa
// `new FixedList<E>(a)` — un constructor, no una llamada estatica.
//
// Rodeo aplicado en la biblioteca: renombrar los ayudantes para que no sean homonimos
// (`FixedSet.fromArray`, `FixedMap.fromPairs`). Es un nombre interno, asi que no toca el contrato.
public interface finding_287<E> {

    // El homonimo de la propia interfaz.
    static <E> finding_287<E> of(E a, E b) {
        return null;
    }

    // Deberia llamar a Ayudante_287.of(Object[], int). Resuelve a of(E, E) de arriba.
    static <E> finding_287<E> of() {
        return Ayudante_287.of(new Object[0], 0);
    }

    // Control: con el ayudante SIN homonimo, la llamada sale bien.
    static <E> finding_287<E> ok() {
        return Ayudante_287.desdeArreglo(new Object[0], 0);
    }
}

final class Ayudante_287<E> implements finding_287<E> {

    static <E> Ayudante_287<E> of(Object[] a, int n) {
        return new Ayudante_287<E>();
    }

    static <E> Ayudante_287<E> desdeArreglo(Object[] a, int n) {
        return new Ayudante_287<E>();
    }
}
