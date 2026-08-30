// Repro de #296 - `Thread.sleep(ms)` cuenta PASOS de bytecode, no milisegundos.
//
//   bin\javac.exe --emit -cp KajiLibrary KajiLibrary\repros\finding_296.java
//   bin\run-headless.exe KajiLibrary\repros\finding_296.class duermeDeVerdad
//
// **Este repro da distinto de los dos lados a proposito**, y es lo que documenta:
//
//   duermeDeVerdad()   java real -> 1     el nuestro -> 0
//
// El resto de los metodos da lo mismo en los dos, y son los que acotan el hallazgo.
//
// El mecanismo, que es lo unico que importa acá: en `bytecode_interpreter.rs::thread_sleep` el
// plazo se guarda como
//
//     sleep_until = Some(self.shared.steps + ms)
//
// o sea que `Thread.sleep(120)` significa "120 **pasos** de interprete", no 120 milisegundos.
// Sobre un planificador cooperativo es un reloj virtual coherente y determinista, y para eso
// funciona bien. El problema es que **no es el mismo reloj** que lee
// `System.currentTimeMillis()`, que si devuelve la hora real: los dos relojes existen y no estan
// relacionados.
//
// Que se rompe con eso: todo lo que mezcle los dos. El caso concreto que lo destapo fue
// `java.util.Timer`, que guarda la hora de cada tarea con `currentTimeMillis` y espera con
// `wait(millis)`; su hilo despierta a los pocos microsegundos de reloj real, ve que todavia no es
// la hora, y vuelve a dormir -- mientras el hilo principal, que "durmio" 200 ms, ya termino. La
// tarea nunca corre.
//
// La vuelta que si funciona, y es la que usan `TmSvTest` y este repro, es **esperar activamente**
// sobre `currentTimeMillis`: mide el mismo reloj que el Timer, y entonces la prueba dice lo mismo
// de los dos lados.
//
// Arreglarlo es una decision de diseno y no un parche: o el reloj virtual pasa a ser tambien el
// que devuelve `currentTimeMillis` --y entonces el tiempo del programa deja de ser el del
// mundo--, o el planificador espera de verdad cuando todos los hilos estan dormidos. Queda
// anotado sin elegir.
//
// `duermeDeVerdad` -> 1 en java real, 0 en el nuestro.
// `relojAvanza` -> 1, `esperaActivaFunciona` -> 1, `sleepNoRompe` -> 1.

public class finding_296 {

    // El unico que diverge: mide con el reloj real cuanto duro un sleep de 120 ms.
    public static int duermeDeVerdad() {
        long antes = System.currentTimeMillis();
        try {
            Thread.sleep(120);
        } catch (InterruptedException e) {
            return -1;
        }
        return System.currentTimeMillis() - antes >= 100 ? 1 : 0;
    }

    // Control: el reloj de pared si avanza por su cuenta.
    public static int relojAvanza() {
        long antes = System.currentTimeMillis();
        int vueltas = 0;
        while (vueltas < 100000000 && System.currentTimeMillis() == antes) {
            vueltas = vueltas + 1;
        }
        return System.currentTimeMillis() > antes ? 1 : 0;
    }

    // Control: la espera activa si consume tiempo real, y es la que hay que usar.
    public static int esperaActivaFunciona() {
        long antes = System.currentTimeMillis();
        long fin = antes + 120;
        while (System.currentTimeMillis() < fin) {
        }
        return System.currentTimeMillis() - antes >= 100 ? 1 : 0;
    }

    // Control: `sleep` no rompe nada, solo vuelve antes. Sigue siendo un punto de cesion valido.
    public static int sleepNoRompe() {
        try {
            Thread.sleep(50);
            Thread.sleep(0);
        } catch (InterruptedException e) {
            return 0;
        }
        return 1;
    }
}
