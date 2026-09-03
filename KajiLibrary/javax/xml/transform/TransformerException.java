package javax.xml.transform;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.transform.TransformerException -- algo salio mal, y **donde**.
 *
 * <p>Es la excepcion base de todo el paquete. Lo que la distingue de una `Exception` cualquiera es
 * el {@link SourceLocator}: un error de transformacion sin la linea de la hoja de estilo es casi
 * inutil, porque el mensaje describe una regla y el usuario necesita el lugar.
 *
 * <p>**El detalle historico que hay que respetar y que sorprende a todo el mundo:** esta clase tiene
 * su propio campo de causa --`containedException`-- y **no** usa el de {@link Throwable}. Nacio en
 * TrAX antes de que Java 1.4 le pusiera causa encadenada a `Throwable`, y cuando la plataforma la
 * incorporo ya habia codigo que llamaba a {@code getException()}. La compatibilidad se resolvio
 * dejando el campo propio y **redefiniendo** {@link #getCause} y {@link #initCause} para que operen
 * sobre el, de modo que las dos vias --la vieja y la de la plataforma-- ven lo mismo. Copiar la
 * causa a los dos lados hubiera sido peor: dos campos que se pueden desincronizar.
 *
 * <p>Una consecuencia concreta de eso, que aca se respeta al pie: {@link #initCause} sobre una
 * excepcion construida con causa tira {@link IllegalStateException}, igual que en `Throwable`, pero
 * mirando el campo propio. Y {@code initCause(null)} sobre una sin causa **es valido** y la deja
 * sin causa -- no es un error, y `Throwable` se comporta igual.
 *
 * <p>Otro detalle que no es cosmetico: los constructores que reciben una causa y un mensaje vacio o
 * nulo usan {@code causa.toString()} como mensaje. Una excepcion envuelta sin mensaje propio es un
 * mensaje en blanco en el log, que es la peor forma de perder un error.
 */
public class TransformerException extends Exception {

    private static final long serialVersionUID = 975798773772956428L;

    /** Donde paso, si se sabe. */
    private SourceLocator locator;

    /** La causa. Ver la nota del encabezado sobre por que no es la de `Throwable`. */
    private Throwable containedException;

    /**
     * Con un mensaje y nada mas.
     *
     * @param message la descripcion del error
     */
    public TransformerException(String message) {
        super(message);
        this.containedException = null;
        this.locator = null;
    }

    /**
     * Envolviendo otra excepcion; el mensaje sale de ella.
     *
     * @param e la causa
     */
    public TransformerException(Throwable e) {
        super(e.toString());
        this.containedException = e;
        this.locator = null;
    }

    /**
     * Con mensaje y causa.
     *
     * <p>Si el mensaje es nulo o vacio se usa {@code e.toString()}: ver la nota del encabezado.
     *
     * @param message la descripcion del error
     * @param e la causa
     */
    public TransformerException(String message, Throwable e) {
        super((message == null || message.length() == 0) ? e.toString() : message);
        this.containedException = e;
        this.locator = null;
    }

    /**
     * Con mensaje y ubicacion.
     *
     * @param message la descripcion del error
     * @param locator donde paso
     */
    public TransformerException(String message, SourceLocator locator) {
        super(message);
        this.containedException = null;
        this.locator = locator;
    }

    /**
     * Con mensaje, ubicacion y causa.
     *
     * @param message la descripcion del error
     * @param locator donde paso
     * @param e la causa
     */
    public TransformerException(String message, SourceLocator locator, Throwable e) {
        super(message);
        this.containedException = e;
        this.locator = locator;
    }

    // ---- ubicacion --------------------------------------------------------------------------

    /** Donde paso, o null si no se sabe. */
    public SourceLocator getLocator() {
        return locator;
    }

    /**
     * Fija la ubicacion.
     *
     * <p>Existe porque quien detecta el error no siempre es quien sabe donde esta: una capa interna
     * lanza, y la de afuera --que si tiene el contexto-- completa la ubicacion antes de propagar.
     *
     * @param location donde paso, o null para borrarla
     */
    public void setLocator(SourceLocator location) {
        this.locator = location;
    }

    /**
     * La ubicacion como texto, o null si no hay {@link SourceLocator}.
     *
     * <p>Ojo con los dos "vacios", que son distintos y significan cosas distintas: **null** es "no
     * hay ubicacion"; la **cadena vacia** es "hay ubicacion pero no dice nada" --un locator con URI
     * nula y linea y columna en cero--. Los componentes en cero se omiten porque no hay linea cero:
     * el 0 es el centinela de "no se", y escribirlo seria informar lo que no se sabe.
     */
    public String getLocationAsString() {
        if (locator == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        agregarUbicacion(buf);
        return buf.toString();
    }

    /**
     * El mensaje seguido de la ubicacion.
     *
     * <p>Lo que corresponde poner en un log: el mensaje solo describe la regla violada, y sin el
     * lugar no alcanza para arreglarla. A diferencia de {@link #getLocationAsString}, esto nunca
     * devuelve null -- si no hay ni mensaje ni ubicacion, devuelve la cadena vacia.
     */
    public String getMessageAndLocation() {
        StringBuilder buf = new StringBuilder();
        String message = super.getMessage();
        if (message != null) {
            buf.append(message);
        }
        if (locator != null) {
            agregarUbicacion(buf);
        }
        return buf.toString();
    }

    /** El armado comun de las dos de arriba, para que no se puedan separar los formatos. */
    private void agregarUbicacion(StringBuilder buf) {
        String systemID = locator.getSystemId();
        int line = locator.getLineNumber();
        int column = locator.getColumnNumber();
        if (systemID != null) {
            buf.append("; SystemID: ");
            buf.append(systemID);
        }
        if (line != 0) {
            buf.append("; Line#: ");
            buf.append(line);
        }
        if (column != 0) {
            buf.append("; Column#: ");
            buf.append(column);
        }
    }

    // ---- causa ------------------------------------------------------------------------------

    /** La causa, por el nombre viejo de TrAX. Equivale a {@link #getCause}. */
    public Throwable getException() {
        return containedException;
    }

    /**
     * La causa, por el nombre de la plataforma.
     *
     * <p>La comparacion con {@code this} es la convencion de `Throwable` para "sin causa" y se
     * respeta aca por si alguien construye la excepcion consigo misma adentro.
     */
    public Throwable getCause() {
        return (containedException == this) ? null : containedException;
    }

    /**
     * Fija la causa, una sola vez.
     *
     * <p>Opera sobre el campo propio, no sobre el de `Throwable`: ver la nota del encabezado.
     *
     * @param cause la causa, o null
     * @return esta misma excepcion
     * @throws IllegalStateException si ya tenia causa
     * @throws IllegalArgumentException si la causa es ella misma
     */
    public synchronized Throwable initCause(Throwable cause) {
        if (this.containedException != null) {
            throw new IllegalStateException("Can't overwrite cause");
        }
        if (cause == this) {
            throw new IllegalArgumentException("Self-causation not permitted");
        }
        this.containedException = cause;
        return this;
    }

    // ---- impresion --------------------------------------------------------------------------

    /**
     * Imprime la ubicacion, la traza, y despues la cadena de causas.
     *
     * <p>La ubicacion va **primero**, antes de la traza: es el dato que el usuario necesita y una
     * traza de pila lo enterraria.
     */
    public void printStackTrace() {
        printStackTrace(new PrintWriter(System.err, true));
    }

    /**
     * Idem, sobre un flujo de bytes.
     *
     * @param s a donde escribir; null significa el error estandar
     */
    public void printStackTrace(PrintStream s) {
        printStackTrace(new PrintWriter(s == null ? System.err : s, true));
    }

    /**
     * Idem, sobre un escritor de caracteres. Esta es la forma real; las otras dos delegan aca.
     *
     * <p>El bucle sobre las causas tiene un tope de 10 y un corte por igualdad. No es paranoia
     * gratuita: la cadena la arma quien lanza, y una excepcion que se contiene a si misma --o dos
     * que se contienen mutuamente-- convertiria un intento de loguear un error en un cuelgue. Un
     * metodo de diagnostico nunca puede ser peor que el problema que esta diagnosticando; por eso
     * ademas todo va dentro de un `catch (Throwable)` que se traga lo que salga.
     *
     * @param s a donde escribir; null significa el error estandar
     */
    public void printStackTrace(PrintWriter s) {
        PrintWriter w = (s == null) ? new PrintWriter(System.err, true) : s;
        try {
            String locInfo = getLocationAsString();
            if (locInfo != null) {
                w.println(locInfo);
            }
            super.printStackTrace(w);
        } catch (Throwable ignorada) {
            // Ni el reporte de un error puede lanzar.
        }
        Throwable exception = getException();
        int i = 0;
        while (i < 10 && exception != null) {
            w.println("---------");
            try {
                exception.printStackTrace(w);
            } catch (Throwable ignorada) {
                w.println("Could not print stack trace...");
            }
            if (exception instanceof TransformerException) {
                Throwable previa = exception;
                exception = ((TransformerException) exception).getException();
                if (previa == exception) {
                    break;
                }
            } else {
                exception = null;
            }
            i = i + 1;
        }
        w.flush();
    }
}
