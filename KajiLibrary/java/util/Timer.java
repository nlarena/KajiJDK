package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Date;
import java.util.TimerTask;

// Un planificador de tareas diferidas: un hilo de fondo que ejecuta TimerTask, una vez o cada
// tanto.
//
// El diseno son tres piezas y no se entiende ninguna sola:
//
//   1. Una **cola de prioridad** (`TimerQueue`) ordenada por hora de ejecucion. Es un monticulo
//      binario y no una lista ordenada porque lo que se hace todo el tiempo es "mirar el proximo"
//      y "reinsertar el que acaba de correr" -- O(1) y O(log n) contra O(n) de insertar en orden.
//   2. Un **hilo** (`TimerThread`) que duerme hasta la hora del primero y lo corre.
//   3. El **monitor de la cola**, que sincroniza a los dos: agregar una tarea despierta al hilo
//      por si la nueva va antes que la que estaba esperando.
//
// Un solo hilo para todas las tareas, y eso es contrato, no atajo: una tarea que tarda demora a
// las que vienen atras. Es la razon por la que `ScheduledThreadPoolExecutor` existe.
//
// Sobre las dos formas de repetir, que es lo que mas se confunde:
//
//   schedule(...)             **retraso fijo**: la proxima se cuenta desde que TERMINO la anterior.
//                             Si una corrida se atrasa, las siguientes se corren todas.
//   scheduleAtFixedRate(...)  **frecuencia fija**: la proxima se cuenta desde la hora TEORICA de la
//                             anterior. Si una se atrasa, las siguientes salen en rafaga para
//                             ponerse al dia.
//
// El signo de `TimerTask.period` es la codificacion: negativo para retraso fijo, positivo para
// frecuencia fija. Viene del JDK y por eso `scheduledExecutionTime` tiene que ramificar por el
// signo.
//
// **Divergencia deliberada**: el argumento `isDaemon` se guarda y no se usa. Nuestro `Thread` no
// tiene `setDaemon`, asi que el hilo del timer es siempre normal; hay que llamar a `cancel()` para
// que termine. En el JDK un timer demonio no impide que la VM salga.
public class Timer {

    // Para darle un nombre distinto a cada hilo cuando no lo dan.
    private static int serie = 0;

    private final TimerQueue queue = new TimerQueue();
    private final TimerThread thread;

    public Timer() {
        this(nombrePorDefecto(), false);
    }

    public Timer(boolean isDaemon) {
        this(nombrePorDefecto(), isDaemon);
    }

    public Timer(String name) {
        this(name, false);
    }

    public Timer(String name, boolean isDaemon) {
        this.thread = new TimerThread(this.queue);
        this.thread.setName(name);
        this.thread.start();
    }

    private static String nombrePorDefecto() {
        synchronized (Timer.class) {
            serie = serie + 1;
            return "Timer-" + serie;
        }
    }

    // ---- programar ------------------------------------------------------------------------------

    // Una vez, dentro de `delay` milisegundos.
    public void schedule(TimerTask task, long delay) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        this.sched(task, System.currentTimeMillis() + delay, 0);
    }

    // Una vez, a la hora dada.
    public void schedule(TimerTask task, Date time) {
        this.sched(task, time.getTime(), 0);
    }

    // Repetida con **retraso fijo**: el periodo se cuenta desde el fin de la corrida anterior.
    public void schedule(TimerTask task, long delay, long period) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        this.sched(task, System.currentTimeMillis() + delay, -period);
    }

    public void schedule(TimerTask task, Date firstTime, long period) {
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        this.sched(task, firstTime.getTime(), -period);
    }

    // Repetida con **frecuencia fija**: el periodo se cuenta desde la hora teorica de la corrida
    // anterior, asi que una demora se recupera despues.
    public void scheduleAtFixedRate(TimerTask task, long delay, long period) {
        if (delay < 0) {
            throw new IllegalArgumentException("Negative delay.");
        }
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        this.sched(task, System.currentTimeMillis() + delay, period);
    }

    public void scheduleAtFixedRate(TimerTask task, Date firstTime, long period) {
        if (period <= 0) {
            throw new IllegalArgumentException("Non-positive period.");
        }
        this.sched(task, firstTime.getTime(), period);
    }

    // El comun de las seis. Los dos monitores se toman en este orden --primero la cola, despues la
    // tarea-- y siempre en el mismo, que es lo unico que evita el abrazo mortal contra
    // `TimerTask.cancel`.
    private void sched(TimerTask task, long time, long period) {
        if (time < 0) {
            throw new IllegalArgumentException("Illegal execution time.");
        }
        synchronized (this.queue) {
            if (!this.thread.newTasksMayBeScheduled) {
                throw new IllegalStateException("Timer already cancelled.");
            }
            synchronized (task.lock) {
                if (task.state != TimerTask.VIRGIN) {
                    throw new IllegalStateException(
                            "Task already scheduled or cancelled");
                }
                task.nextExecutionTime = time;
                task.period = period;
                task.state = TimerTask.SCHEDULED;
            }
            this.queue.add(task);
            // Si la que acaba de entrar es la primera, el hilo estaba durmiendo hasta otra hora.
            if (this.queue.getMin() == task) {
                this.queue.notify();
            }
        }
    }

    // ---- terminar --------------------------------------------------------------------------------

    // Descarta lo pendiente y deja que el hilo termine. Una tarea que ya esta corriendo no se
    // interrumpe: cancelar no es matar.
    public void cancel() {
        synchronized (this.queue) {
            this.thread.newTasksMayBeScheduled = false;
            this.queue.clear();
            this.queue.notify();
        }
    }

    // Saca de la cola las tareas ya canceladas y devuelve cuantas saco.
    //
    // Existe por una razon concreta: una tarea cancelada se queda en la cola hasta que le toque el
    // turno, asi que un programa que cancela muchas y agrega muchas mas acumula basura viva. Este
    // metodo es la valvula, y no se llama solo.
    public int purge() {
        int quitadas = 0;
        synchronized (this.queue) {
            int i = this.queue.size();
            while (i > 0) {
                TimerTask t = this.queue.get(i);
                boolean cancelada;
                synchronized (t.lock) {
                    cancelada = t.state == TimerTask.CANCELLED;
                }
                if (cancelada) {
                    this.queue.quitar(i);
                    quitadas = quitadas + 1;
                }
                i = i - 1;
            }
            if (quitadas > 0) {
                this.queue.reordenar();
            }
        }
        return quitadas;
    }
}

// El monticulo binario de tareas, ordenado por `nextExecutionTime`. Package-private.
//
// Se indexa desde 1 y no desde 0 a proposito: con base 1 los hijos de `i` son `2i` y `2i+1` y el
// padre es `i/2`, sin sumas ni restas. Es la convencion clasica y la razon por la que el slot 0
// queda sin usar.
//
// **No** sincroniza nada: quien la usa toma su monitor por afuera. Asi el Timer puede hacer
// varias operaciones bajo un solo candado.
final class TimerQueue {

    private TimerTask[] queue = new TimerTask[128];
    private int size = 0;

    int size() {
        return this.size;
    }

    void add(TimerTask task) {
        if (this.size + 1 == this.queue.length) {
            TimerTask[] mas = new TimerTask[2 * this.queue.length];
            System.arraycopy(this.queue, 0, mas, 0, this.queue.length);
            this.queue = mas;
        }
        this.size = this.size + 1;
        this.queue[this.size] = task;
        this.subir(this.size);
    }

    // La tarea que va primero. Sin sacarla.
    TimerTask getMin() {
        return this.queue[1];
    }

    TimerTask get(int i) {
        return this.queue[i];
    }

    void removeMin() {
        this.queue[1] = this.queue[this.size];
        this.queue[this.size] = null;
        this.size = this.size - 1;
        this.bajar(1);
    }

    // Saca la posicion `i`. Deja el monticulo **sin** reordenar: `purge` hace muchas seguidas y
    // reordena una sola vez al final.
    void quitar(int i) {
        this.queue[i] = this.queue[this.size];
        this.queue[this.size] = null;
        this.size = this.size - 1;
    }

    // Cambia la hora del primero y lo reubica. Es lo que hace una tarea repetida al volver a la
    // cola sin salir de ella.
    void rescheduleMin(long newTime) {
        this.queue[1].nextExecutionTime = newTime;
        this.bajar(1);
    }

    boolean isEmpty() {
        return this.size == 0;
    }

    void clear() {
        int i = 1;
        while (i <= this.size) {
            this.queue[i] = null;
            i = i + 1;
        }
        this.size = 0;
    }

    // Rehace el monticulo entero, de abajo hacia arriba. Es O(n) -- mas barato que n inserciones.
    void reordenar() {
        int i = this.size / 2;
        while (i >= 1) {
            this.bajar(i);
            i = i - 1;
        }
    }

    private void subir(int k) {
        while (k > 1) {
            int padre = k / 2;
            if (this.queue[padre].nextExecutionTime <= this.queue[k].nextExecutionTime) {
                return;
            }
            this.intercambiar(k, padre);
            k = padre;
        }
    }

    private void bajar(int k) {
        while (2 * k <= this.size) {
            int hijo = 2 * k;
            if (hijo < this.size
                    && this.queue[hijo + 1].nextExecutionTime
                            < this.queue[hijo].nextExecutionTime) {
                hijo = hijo + 1;
            }
            if (this.queue[k].nextExecutionTime <= this.queue[hijo].nextExecutionTime) {
                return;
            }
            this.intercambiar(k, hijo);
            k = hijo;
        }
    }

    private void intercambiar(int a, int b) {
        TimerTask t = this.queue[a];
        this.queue[a] = this.queue[b];
        this.queue[b] = t;
    }
}

// El hilo que corre las tareas. Package-private.
//
// El bucle tiene una forma que conviene leer despacio, porque cada detalle esta por algo:
//
//   - Espera sobre el monitor de la **cola**, no sobre el suyo: asi `schedule` puede despertarlo
//     agregando una tarea que va antes que la que estaba esperando.
//   - La tarea se corre **fuera** del `synchronized`. Correrla adentro bloquearia a cualquiera que
//     quisiera programar otra mientras dura, y una tarea puede tardar lo que quiera.
//   - Una tarea repetida se reprograma **antes** de correr, no despues. Si se hiciera despues, una
//     tarea que lanza excepcion nunca volveria a la cola.
final class TimerThread extends Thread {

    // Se pone en false al cancelar. Lo lee el bucle bajo el monitor de la cola.
    boolean newTasksMayBeScheduled = true;

    private final TimerQueue queue;

    TimerThread(TimerQueue queue) {
        this.queue = queue;
    }

    public void run() {
        this.bucle();
        // Al salir, la cola queda vacia y el hilo muerto. Un Timer cancelado no se reanima.
        synchronized (this.queue) {
            this.newTasksMayBeScheduled = false;
            this.queue.clear();
        }
    }

    private void bucle() {
        while (true) {
            TimerTask task;
            boolean corresponde;
            synchronized (this.queue) {
                // Esperar mientras no haya nada y todavia se puedan agregar tareas.
                while (this.queue.isEmpty() && this.newTasksMayBeScheduled) {
                    this.esperar(0);
                }
                if (this.queue.isEmpty()) {
                    return; // cancelado y sin nada pendiente
                }
                task = this.queue.getMin();
                long ahora;
                long cuando;
                synchronized (task.lock) {
                    if (task.state == TimerTask.CANCELLED) {
                        this.queue.removeMin();
                        continue;
                    }
                    ahora = System.currentTimeMillis();
                    cuando = task.nextExecutionTime;
                    corresponde = cuando <= ahora;
                    if (corresponde) {
                        if (task.period == 0) {
                            this.queue.removeMin();
                            task.state = TimerTask.EXECUTED;
                        } else if (task.period < 0) {
                            // Retraso fijo: se cuenta desde AHORA.
                            this.queue.rescheduleMin(ahora - task.period);
                        } else {
                            // Frecuencia fija: se cuenta desde la hora teorica.
                            this.queue.rescheduleMin(cuando + task.period);
                        }
                    }
                }
                if (!corresponde) {
                    this.esperar(cuando - ahora);
                }
            }
            if (corresponde) {
                task.run();
            }
        }
    }

    // Espera sobre el monitor de la cola, tragandose la interrupcion.
    //
    // Se la traga porque el bucle vuelve a mirar la cola de todas formas: una interrupcion espuria
    // no puede hacer que una tarea se corra antes de tiempo ni que se saltee.
    private void esperar(long millis) {
        try {
            this.queue.wait(millis);
        } catch (InterruptedException e) {
            // vuelve al bucle
        }
    }
}
