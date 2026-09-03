package java.util.logging;

/**
 * KajiLibrary's java.util.logging.MemoryHandler -- guarda los ultimos mensajes y los suelta cuando
 * pasa algo.
 *
 * <p>Resuelve la tension entre las dos cosas que uno quiere de una traza y que se contradicen: tener
 * el detalle fino cuando algo falla, y no pagar el costo de escribirlo cuando no falla nada. La
 * salida es no escribir: los registros se acumulan en un anillo en memoria, y **recien** cuando llega
 * uno de nivel {@link #getPushLevel} o mas se vuelca todo lo acumulado al manejador de destino. Lo
 * que queda en el archivo es el minuto anterior al fallo, que es exactamente el minuto que uno
 * querria haber estado registrando.
 *
 * <p>El buffer es circular y de tamano fijo, y eso tambien es el diseno: al llenarse **descarta el
 * mas viejo** en vez de crecer o de dejar de aceptar. Un programa que corre una semana sin fallar no
 * puede quedarse sin memoria por su propia traza, y de lo que se acumulo el lunes no le importa nada
 * a nadie.
 *
 * <p>El nivel propio y el de empuje son dos umbrales distintos y hay que no confundirlos: el propio
 * --{@link Handler#setLevel}, que por omision es {@link Level#ALL}-- decide **que entra al buffer**, y
 * el de empuje decide **que lo vacia**. Poner los dos iguales convierte esto en un manejador comun
 * con un paso de mas.
 *
 * <p>{@link #push} tambien se puede llamar a mano, que es lo que hace falta cuando la senal de que
 * algo anda mal no es un mensaje sino otra cosa -- un chequeo de salud que dio mal, una peticion que
 * tardo de mas.
 */
public class MemoryHandler extends Handler {

    private static final int TAMANO_POR_OMISION = 1000;

    private Handler target;
    private Level pushLevel;
    private LogRecord[] buffer;

    // El indice del mas viejo, y cuantos hay. Con estos dos alcanza: el mas nuevo esta en
    // `(inicio + cuantos - 1) % buffer.length`, y no hace falta distinguir "vacio" de "lleno" por la
    // posicion de dos punteros, que es el error clasico del anillo.
    private int inicio;
    private int cuantos;

    /**
     * El que sale de la configuracion.
     *
     * <p>El destino **no tiene valor por omision** y es el unico manejador del que eso es cierto:
     * `java.util.logging.MemoryHandler.target` tiene que estar. Es coherente con lo que este
     * manejador es -- no escribe a ningun lado por si mismo, asi que uno sin destino no es un
     * manejador con una configuracion pobre sino un agujero por donde la traza se pierde entera. Vale
     * mas fallar al construirlo.
     *
     * @throws RuntimeException si la configuracion no dice a que manejador volcar
     */
    public MemoryHandler() {
        LogManager m = LogManager.getLogManager();
        String cname = "java.util.logging.MemoryHandler";
        this.pushLevel = m.getLevelProperty(cname + ".push", Level.SEVERE);
        int tam = m.getIntProperty(cname + ".size", TAMANO_POR_OMISION);
        if (tam <= 0) {
            tam = TAMANO_POR_OMISION;
        }
        this.buffer = new LogRecord[tam];
        this.setLevel(m.getLevelProperty(cname + ".level", Level.ALL));
        this.setFilter(m.getFilterProperty(cname + ".filter", null));
        this.setFormatter(m.getFormatterProperty(cname + ".formatter", new SimpleFormatter()));
        String destino = m.getProperty(cname + ".target");
        if (destino == null) {
            throw new RuntimeException("The handler " + cname + " does not specify a target");
        }
        try {
            this.target = (Handler) LogManager.crear(destino.trim());
        } catch (Exception e) {
            throw new RuntimeException("MemoryHandler can't load handler target \"" + destino + "\"");
        }
    }

    /**
     * @throws IllegalArgumentException si `size` no es positivo -- un anillo de cero no guarda nada y
     *         uno negativo no significa nada
     * @throws NullPointerException si `target` o `pushLevel` son `null`
     */
    public MemoryHandler(Handler target, int size, Level pushLevel) {
        if (size <= 0) {
            throw new IllegalArgumentException("size: " + size);
        }
        if (target == null) {
            throw new NullPointerException("target");
        }
        if (pushLevel == null) {
            throw new NullPointerException("pushLevel");
        }
        LogManager m = LogManager.getLogManager();
        String cname = "java.util.logging.MemoryHandler";
        this.setLevel(m.getLevelProperty(cname + ".level", Level.ALL));
        this.setFilter(m.getFilterProperty(cname + ".filter", null));
        this.setFormatter(m.getFormatterProperty(cname + ".formatter", new SimpleFormatter()));
        this.target = target;
        this.pushLevel = pushLevel;
        this.buffer = new LogRecord[size];
    }

    /**
     * Guarda el registro en el anillo, y lo vuelca todo si este alcanza el nivel de empuje.
     *
     * <p>Lo importante es lo que **no** hace: no le pasa el registro al destino. Al destino le llega
     * despues, cuando el volcado, y por eso un manejador de memoria no cuesta E/S mientras no pasa
     * nada.
     */
    public synchronized void publish(LogRecord record) {
        if (!this.isLoggable(record)) {
            return;
        }
        int i = (this.inicio + this.cuantos) % this.buffer.length;
        this.buffer[i] = record;
        if (this.cuantos < this.buffer.length) {
            this.cuantos = this.cuantos + 1;
        } else {
            // Estaba lleno: la escritura piso al mas viejo, asi que el mas viejo es el siguiente.
            this.inicio = (this.inicio + 1) % this.buffer.length;
        }
        if (record.getLevel().intValue() < this.pushLevel.intValue()) {
            return;
        }
        this.push();
    }

    /**
     * Vuelca lo acumulado al destino, del mas viejo al mas nuevo, y vacia el anillo.
     *
     * <p>Vaciarlo es parte del contrato y no un detalle: sin eso, dos fallos seguidos escribirian dos
     * veces los mismos registros y la traza mentiria sobre cuantas veces paso cada cosa.
     *
     * <p>Cada registro pasa por el {@link Handler#isLoggable} **del destino**, que es donde se aplica
     * el nivel del destino. El de este manejador ya se aplico al entrar.
     */
    public synchronized void push() {
        int i = 0;
        while (i < this.cuantos) {
            this.target.publish(this.buffer[(this.inicio + i) % this.buffer.length]);
            i = i + 1;
        }
        this.inicio = 0;
        this.cuantos = 0;
    }

    /** Vacia el **destino**; lo que esta en el anillo no se escribio todavia y no hay que vaciarlo. */
    public void flush() {
        this.target.flush();
    }

    /**
     * Cierra el destino y se apaga.
     *
     * <p>Y **descarta** lo que quede en el anillo, que es lo que dice el contrato. Suena a perdida y
     * es coherente: lo del anillo son mensajes que no llegaron a interesarle a nadie, y volcarlos al
     * cerrar convertiria cada final de programa en un volcado completo de traza fina.
     */
    public void close() throws SecurityException {
        this.target.close();
        this.setLevel(Level.OFF);
    }

    /** Al que se le vuelca lo acumulado. */
    public synchronized void setPushLevel(Level newLevel) throws SecurityException {
        if (newLevel == null) {
            throw new NullPointerException("newLevel");
        }
        this.pushLevel = newLevel;
    }

    public synchronized Level getPushLevel() {
        return this.pushLevel;
    }

    /**
     * Si el registro entra al **anillo**.
     *
     * <p>Que es distinto de si se va a ver: puede entrar y despues quedar descartado al cerrar sin
     * que haya habido ningun empuje.
     */
    public boolean isLoggable(LogRecord record) {
        return super.isLoggable(record);
    }
}
