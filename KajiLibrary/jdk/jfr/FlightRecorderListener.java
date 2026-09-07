package jdk.jfr;

/**
 * Avisos sobre el grabador y sobre los cambios de estado de las grabaciones.
 *
 * <h2>Para que sirve enterarse de que el grabador arranco</h2>
 *
 * <p>Porque JFR se inicializa perezosamente: puede no existir cuando la aplicacion arranca y
 * aparecer despues, si alguien lo prende desde afuera con {@code jcmd}. Codigo que quiera
 * configurar algo en cuanto exista no puede preguntar una vez y rendirse — tiene que registrarse y
 * esperar.
 *
 * <p>{@link #recorderInitialized} es ese aviso. Si el grabador <strong>ya</strong> estaba
 * inicializado al registrarse, el aviso llega igual y enseguida, asi que no hay carrera que
 * manejar.
 *
 * <h2>Los dos metodos son {@code default}</h2>
 *
 * <p>Casi nadie quiere los dos. Dejarlos con cuerpo vacio evita el metodo vacio de compromiso que
 * habria que escribir en cada implementacion.
 *
 * @since 9
 */
public interface FlightRecorderListener {

    /**
     * El grabador se inicializo.
     *
     * @param recorder el grabador
     */
    default void recorderInitialized(FlightRecorder recorder) {
    }

    /**
     * Una grabacion cambio de estado.
     *
     * @param recording la grabacion, ya con su estado nuevo
     */
    default void recordingStateChanged(Recording recording) {
    }
}
