package java.awt;

/**
 * Puente para instalar un despachador ajeno en una {@link EventQueue}.
 *
 * <p>No es una clase del JDK: es andamiaje nuestro, del mismo tipo que
 * {@code java.nio.channels.FabricaMapMode}. Existe porque el metodo de {@link EventQueue} que guarda
 * el despachador es privado --en el JDK se lo alcanza por {@code AWTAccessor}, la puerta de servicio
 * que los modulos de la plataforma se abren entre si-- y {@code jdk.swing.interop} vive en otro
 * paquete.
 *
 * <h2>Que es un despachador ajeno</h2>
 *
 * <p>Cuando AWT convive con otro juego de herramientas graficas --JavaFX incrustando Swing, que es
 * para lo que existe {@code jdk.swing.interop}-- no puede haber dos hilos de eventos peleando por
 * los mismos componentes. La salida es que uno de los dos ceda: AWT deja de atender su propia cola
 * y le pasa cada despacho al otro, que los corre en su hilo. Eso es un despachador ajeno.
 *
 * <p>Instalar uno cambia tres cosas en la cola: quien decide si el hilo actual es el de despacho,
 * quien corre cada evento, y de donde salen los bucles secundarios. Las tres estan en
 * {@link Despachador}.
 */
public final class AccesoDespachador {

    /** Quien se hace cargo de los eventos de una cola que cedio el despacho. */
    public interface Despachador {

        /**
         * Si el hilo actual es aquel en el que este despachador corre los eventos.
         *
         * @return cierto si lo es
         */
        boolean esHiloDeDespacho();

        /**
         * Corre eso en el hilo de despacho, cuando pueda.
         *
         * @param tarea que correr
         */
        void programarDespacho(Runnable tarea);

        /**
         * Un bucle secundario suyo, para que un dialogo modal bloquee sin congelar la interfaz.
         *
         * @return el bucle
         */
        SecondaryLoop bucleSecundario();
    }

    private AccesoDespachador() {
    }

    /**
     * Instala ese despachador en esa cola, o lo saca si es {@code null}.
     *
     * <p>Si la cola esta apilada bajo otras, el despachador va a la de mas arriba, que es la que
     * atiende: instalarlo en una cola tapada no tendria efecto.
     *
     * @param cola la cola que cede el despacho
     * @param despachador quien se hace cargo, o {@code null} para volver atras
     * @throws NullPointerException si la cola es {@code null}
     */
    public static void instalar(EventQueue cola, Despachador despachador) {
        if (cola == null) {
            throw new NullPointerException("cola");
        }
        cola.instalarDespachador(despachador);
    }
}
