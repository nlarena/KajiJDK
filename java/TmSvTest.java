import java.util.Date;
import java.util.Iterator;
import java.util.ServiceLoader;
import java.util.Timer;
import java.util.TimerTask;

// Comportamiento de java.util.Timer y java.util.ServiceLoader.
//
// Nota sobre las esperas: se usa una **espera activa** sobre System.currentTimeMillis() y no
// Thread.sleep. En nuestro interprete verde el plazo de `sleep` se mide en PASOS de bytecode y no
// en milisegundos, asi que `Thread.sleep(200)` vuelve en microsegundos de reloj real y el hilo del
// Timer nunca ve llegar la hora de su tarea. La espera activa mide el mismo reloj que el Timer, y
// entonces la prueba dice lo mismo de los dos lados.
public class TmSvTest {

    private static int corridas = 0;
    private static int orden = 0;

    static class Cuenta extends TimerTask {
        public void run() {
            corridas = corridas + 1;
        }
    }

    // Anota su etiqueta en el orden en que corrio, para verificar que la cola respeta la hora.
    static class Marca extends TimerTask {
        private final int etiqueta;

        Marca(int etiqueta) {
            this.etiqueta = etiqueta;
        }

        public void run() {
            orden = orden * 10 + this.etiqueta;
        }
    }

    private static void esperar(long ms) {
        long fin = System.currentTimeMillis() + ms;
        while (System.currentTimeMillis() < fin) {
        }
    }

    public static int run() {
        int r = 0;

        // ---- una sola tarea, diferida ------------------------------------------------------------
        corridas = 0;
        Timer t = new Timer("prueba");
        t.schedule(new Cuenta(), 20);
        esperar(300);
        r = r + (corridas == 1 ? 1 : 0);
        esperar(150);
        r = r + (corridas == 1 ? 2 : 0);          // una sola vez, no se repite
        t.cancel();

        // ---- una tarea a fecha fija --------------------------------------------------------------
        corridas = 0;
        Timer conFecha = new Timer();
        conFecha.schedule(new Cuenta(), new Date(System.currentTimeMillis() + 20));
        esperar(300);
        r = r + (corridas == 1 ? 4 : 0);
        conFecha.cancel();

        // ---- repeticion ----------------------------------------------------------------------------
        corridas = 0;
        Timer rep = new Timer("rep");
        Cuenta repetida = new Cuenta();
        rep.schedule(repetida, 0, 20);
        esperar(300);
        r = r + (corridas >= 3 ? 8 : 0);
        // cancelar la tarea la detiene, sin tocar el timer
        boolean detuvo = repetida.cancel();
        r = r + (detuvo ? 16 : 0);
        int hasta = corridas;
        esperar(200);
        r = r + (corridas == hasta ? 32 : 0);
        // cancelar dos veces devuelve false la segunda: nada quedaba por prevenir
        r = r + (repetida.cancel() ? 0 : 64);
        // y ahora esta en la cola, cancelada, esperando a que purge la saque
        r = r + (rep.purge() >= 0 ? 128 : 0);
        rep.cancel();

        // ---- la cola respeta la hora ------------------------------------------------------------------
        orden = 0;
        Timer cola = new Timer("cola");
        cola.schedule(new Marca(3), 120);        // se programan al reves de como corren
        cola.schedule(new Marca(1), 20);
        cola.schedule(new Marca(2), 70);
        esperar(400);
        r = r + (orden == 123 ? 256 : 0);
        cola.cancel();

        // ---- errores de uso ---------------------------------------------------------------------------
        Timer usado = new Timer("usado");
        Cuenta unaVez = new Cuenta();
        usado.schedule(unaVez, 10000);
        boolean dosVeces = false;
        try {
            usado.schedule(unaVez, 10);           // la misma tarea, otra vez
        } catch (IllegalStateException e) {
            dosVeces = true;
        }
        r = r + (dosVeces ? 512 : 0);
        boolean negativo = false;
        try {
            usado.schedule(new Cuenta(), -1);
        } catch (IllegalArgumentException e) {
            negativo = true;
        }
        r = r + (negativo ? 1024 : 0);
        boolean periodoCero = false;
        try {
            usado.schedule(new Cuenta(), 0, 0);
        } catch (IllegalArgumentException e) {
            periodoCero = true;
        }
        r = r + (periodoCero ? 2048 : 0);
        usado.cancel();
        boolean yaCancelado = false;
        try {
            usado.schedule(new Cuenta(), 10);
        } catch (IllegalStateException e) {
            yaCancelado = true;
        }
        r = r + (yaCancelado ? 4096 : 0);

        // scheduledExecutionTime antes de correr no dice nada util, pero no rompe
        r = r + (new Cuenta().scheduledExecutionTime() == 0L ? 8192 : 0);

        // ---- ServiceLoader ------------------------------------------------------------------------------
        // Sin proveedores declarados en el classpath, un ServiceLoader es una coleccion vacia bien
        // formada. Eso es lo que se prueba: que se comporte como tal en vez de romper.
        ServiceLoader<Runnable> sl = ServiceLoader.load(Runnable.class);
        r = r + (sl != null ? 1 : 0);
        Iterator<Runnable> it = sl.iterator();
        r = r + (it.hasNext() ? 0 : 2);
        r = r + (sl.findFirst().isPresent() ? 0 : 4);
        r = r + (sl.stream().count() == 0L ? 8 : 0);
        sl.reload();
        r = r + (sl.iterator().hasNext() ? 0 : 16);
        r = r + (sl.toString().indexOf("java.util.ServiceLoader[") == 0 ? 32 : 0);

        ServiceLoader<Runnable> conCargador =
                ServiceLoader.load(Runnable.class, ClassLoader.getSystemClassLoader());
        r = r + (conCargador != null ? 64 : 0);
        ServiceLoader<Runnable> instalados = ServiceLoader.loadInstalled(Runnable.class);
        r = r + (instalados.iterator().hasNext() ? 0 : 128);

        // pedirle un elemento a un iterador vacio se niega
        boolean vacio = false;
        try {
            sl.iterator().next();
        } catch (java.util.NoSuchElementException e) {
            vacio = true;
        }
        r = r + (vacio ? 256 : 0);

        return r;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
