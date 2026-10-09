// Repro de #295 - una clase anidada en una INTERFAZ se trataba como interna de instancia, y a sus
// constructores se les inyectaba el parametro de cabecera `Outer this$0`.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_295.java
//   bin\run-headless.exe KajiLibrary\repros\finding_295.class envuelve
//
// ANTES el sintoma era este, y no se parece a la causa:
//
//   error: no se encontro un constructor `FailedException(Throwable)` aplicable
//     metodo FailedException.FailedException(StructuredTaskScope, Throwable) no es aplicable
//       (las listas de argumentos difieren en longitud)
//
// El constructor que el fuente escribio toma UN argumento; el que el compilador buscaba tomaba
// dos, porque le habia antepuesto la instancia envolvente. Y hasta el arreglo de #293 ni siquiera
// se veia: sin nadie aplicable, se emitia `invokespecial <init>:()V` y el argumento quedaba en la
// pila.
//
// Un tipo miembro de una interfaz es implicitamente `static` (§9.5) aunque no lo diga: no hay
// instancia envolvente que capturar, porque una interfaz no tiene instancias. La regla se estaba
// aplicando solo mirando el `static` de la anidada y su propio `kind`, sin mirar que era la
// **envolvente**.
//
// AHORA: `desugar.rs` filtra tambien por el kind de la envolvente.
//
// El matiz que costo una vuelta -- y que esta escrito en el codigo porque es facil volver a
// pisarlo: la regla vale para los tipos **miembro**, no para una clase **anonima** creada en un
// metodo `default`. Esa tiene dueña interfaz igual, pero se crea en contexto de instancia, donde
// `this` existe, y **si** captura. Ponerle el filtro dejaba a los `$1` de `Spliterator` y
// `PrimitiveIterator` sin su parametro de cabecera.
//
// `envuelve` -> 7, `conDefault` -> 9, `anidadaEnClase` -> 5.

public class finding_295 {

    // El caso del finding: una clase anidada en una interfaz, con un constructor de un argumento.
    public static int envuelve() {
        Caja.Falla f = new Caja.Falla(7);
        return f.valor();
    }

    // La clase anonima dentro de un metodo `default`, que SI captura y tiene que seguir andando.
    public static int conDefault() {
        Caja c = new CajaReal();
        return c.masUno().valor();
    }

    // Control: una anidada de instancia en una CLASE sigue capturando su `this$0`.
    //
    // El `new` va adentro de la envolvente y no aca afuera porque la forma calificada
    // (`a.new Adentro()`) no compila -- el tipo no resuelve en esa posicion. Es anterior a esta
    // tanda: el javac congelado falla igual.
    public static int anidadaEnClase() {
        Afuera a = new Afuera(5);
        return a.desdeAdentro();
    }
}

interface Caja {

    int base();

    // Anidada en una interfaz: implicitamente estatica, sin `this$0`.
    final class Falla {

        private final int v;

        Falla(int v) {
            this.v = v;
        }

        int valor() {
            return this.v;
        }
    }

    // Una anonima creada en contexto de instancia: usa `this` de la interfaz, asi que captura.
    default Falla masUno() {
        return new Falla(this.base() + 1);
    }
}

class CajaReal implements Caja {

    public int base() {
        return 8;
    }
}

class Afuera {

    private final int n;

    Afuera(int n) {
        this.n = n;
    }

    // Se crea la interna desde la envolvente, que es donde el `this` implicito esta disponible.
    int desdeAdentro() {
        Adentro d = new Adentro();
        return d.leeDeAfuera();
    }

    class Adentro {

        int leeDeAfuera() {
            return Afuera.this.n;
        }
    }
}
