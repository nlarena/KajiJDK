// La familia `java.util.concurrent.atomic`, que el finding #309 tenia mal compilada **entera**.
//
// Los ocho metodos afectados son los `getAndIncrement`/`getAndDecrement` de `AtomicInteger`,
// `AtomicLong`, `AtomicIntegerArray` y `AtomicLongArray`: los cuatro estan escritos como
// `return value++;` / `return array[i]++;`, que es exactamente la forma que no emitia nada. Toda
// llamada terminaba en "operand stack underflow".
//
// Vale la pena decir POR QUE no se habia notado. La biblioteca tiene dos redes: el punto fijo (los
// `.class` del arbol no cambian al recompilar) y las pruebas de comportamiento. El punto fijo **no
// podia** ver esto: los dos lados de la comparacion los emitia el mismo compilador roto, asi que
// coincidian perfectamente en estar mal. Es la tercera vez que el proyecto se lleva esta leccion --
// las otras dos fueron un cuerpo vacio y un miembro heredado de Object -- y las tres veces el unico
// que lo encontro fue correr el codigo.
//
// Con todo en verde devuelve -1; si no, el indice de la primera comprobacion que fallo.
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;

public class AtomTest {

    static int cuantas = 0;
    static int primerFallo = -1;

    static void ok(boolean b) {
        if (!b && primerFallo < 0) {
            primerFallo = cuantas;
        }
        cuantas = cuantas + 1;
    }

    static void enteros() {
        AtomicInteger a = new AtomicInteger(10);
        // Lo que devuelve es el valor **previo**, y el campo queda incrementado. Las dos mitades
        // importan: emitir el incremento sin dejar el valor, o al reves, son dos bugs distintos.
        ok(a.getAndIncrement() == 10);
        ok(a.get() == 11);
        ok(a.getAndDecrement() == 11);
        ok(a.get() == 10);

        // Y las formas que devuelven el **nuevo**, para fijar la diferencia.
        ok(a.incrementAndGet() == 11);
        ok(a.decrementAndGet() == 10);
        ok(a.getAndAdd(5) == 10);
        ok(a.get() == 15);
        ok(a.addAndGet(-5) == 10);

        // Desde un negativo, por si el signo del delta se colara en algun lado.
        AtomicInteger b = new AtomicInteger(-1);
        ok(b.getAndIncrement() == -1);
        ok(b.get() == 0);
    }

    static void largos() {
        AtomicLong a = new AtomicLong(10L);
        ok(a.getAndIncrement() == 10L);
        ok(a.get() == 11L);
        ok(a.getAndDecrement() == 11L);
        ok(a.get() == 10L);
        ok(a.incrementAndGet() == 11L);
        ok(a.decrementAndGet() == 10L);

        // Un valor que no entra en 32 bits: si el `long` se tratara como categoria 1 en algun dup,
        // aca se parte.
        AtomicLong g = new AtomicLong(4294967296L);
        ok(g.getAndIncrement() == 4294967296L);
        ok(g.get() == 4294967297L);
    }

    static void arregloInt() {
        AtomicIntegerArray a = new AtomicIntegerArray(3);
        a.set(1, 10);
        ok(a.getAndIncrement(1) == 10);
        ok(a.get(1) == 11);
        ok(a.getAndDecrement(1) == 11);
        ok(a.get(1) == 10);
        // Los vecinos no se tocan: el juego de pila usa (arrayref, indice) duplicados, y un indice
        // mal duplicado escribiria en otra celda.
        ok(a.get(0) == 0);
        ok(a.get(2) == 0);
    }

    static void arregloLong() {
        AtomicLongArray a = new AtomicLongArray(3);
        a.set(2, 4294967296L);
        ok(a.getAndIncrement(2) == 4294967296L);
        ok(a.get(2) == 4294967297L);
        ok(a.getAndDecrement(2) == 4294967297L);
        ok(a.get(2) == 4294967296L);
        ok(a.get(0) == 0L);
        ok(a.get(1) == 0L);
    }

    public static int run() {
        enteros();
        largos();
        arregloInt();
        arregloLong();
        return primerFallo;
    }

    // Para el arnes de regresion, que corre la clase con el `java` real y compara la salida.
    public static void main(String[] args) {
        System.out.println(run());
    }
}
