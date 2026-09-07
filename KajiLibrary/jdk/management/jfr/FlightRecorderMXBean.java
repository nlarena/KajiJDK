package jdk.management.jfr;

import java.io.IOException;
import java.lang.management.PlatformManagedObject;
import java.util.List;
import java.util.Map;

/**
 * JFR manejado por JMX: grabar en una VM desde otro proceso.
 *
 * <h2>Por que existe si ya esta {@code jdk.jfr}</h2>
 *
 * <p>Porque {@code jdk.jfr} solo se puede usar desde adentro de la VM que se quiere observar. Una
 * consola de monitoreo esta afuera, y lo unico que la cruza es JMX.
 *
 * <p>Es tambien lo que hace posible grabar en un proceso que no fue escrito para eso: no hay que
 * agregarle codigo, alcanza con conectarse.
 *
 * <h2>Por que todo se maneja con un {@code long}</h2>
 *
 * <p>{@link #newRecording} devuelve un numero y todos los demas metodos lo reciben. Es asi porque a
 * traves de JMX no puede viajar un objeto {@link jdk.jfr.Recording}: lo unico que cruza son tipos
 * abiertos. El numero es el identificador de la grabacion del otro lado, y esta interfaz es un
 * control remoto sobre ella.
 *
 * <p>Lo mismo con {@link #openStream}, que devuelve otro numero: los datos se traen a pedazos con
 * {@link #readStream}, hasta que devuelva {@code null}. Una grabacion puede pesar cientos de megas
 * y no se puede mandar de una.
 *
 * <h2>Las dos formas de llevarse los datos</h2>
 *
 * <p>{@link #copyTo} le pide a la VM remota que escriba el archivo <strong>en su propio
 * disco</strong>; el flujo lo trae por la red. La primera es mucho mas rapida y deja el archivo
 * alla, que sirve cuando alguien va a buscarlo despues.
 *
 * @since 9
 */
public interface FlightRecorderMXBean extends PlatformManagedObject {

    /** El nombre del MBean en el servidor de la plataforma. */
    String MXBEAN_NAME = "jdk.management.jfr:type=FlightRecorder";

    /**
     * Crea una grabacion y devuelve su identificador.
     *
     * @return el identificador
     * @throws IllegalStateException si JFR no esta disponible
     */
    long newRecording() throws IllegalStateException;

    /**
     * Una grabacion con lo que haya en los buffers en este momento.
     *
     * @return el identificador de la instantanea
     */
    long takeSnapshot();

    /**
     * Copia una grabacion.
     *
     * @param recordingId el identificador de la original
     * @param stop si la copia queda detenida
     * @return el identificador de la copia
     * @throws IllegalArgumentException si no existe esa grabacion
     */
    long cloneRecording(long recordingId, boolean stop) throws IllegalArgumentException;

    /**
     * Arranca una grabacion.
     *
     * @param recordingId el identificador
     * @throws IllegalStateException si ya arranco o se cerro
     */
    void startRecording(long recordingId) throws IllegalStateException;

    /**
     * Detiene una grabacion.
     *
     * @param recordingId el identificador
     * @return si estaba grabando
     * @throws IllegalArgumentException si no existe esa grabacion
     * @throws IllegalStateException si no se puede detener
     */
    boolean stopRecording(long recordingId) throws IllegalArgumentException, IllegalStateException;

    /**
     * Cierra una grabacion y suelta sus datos.
     *
     * @param recordingId el identificador
     * @throws IOException si no se pudo cerrar
     */
    void closeRecording(long recordingId) throws IOException;

    /**
     * Abre un flujo para traerse los datos de una grabacion.
     *
     * @param recordingId el identificador de la grabacion
     * @param streamOptions opciones del flujo, como el intervalo de tiempo
     * @return el identificador del flujo
     * @throws IOException si no se pudo abrir
     */
    long openStream(long recordingId, Map<String, String> streamOptions) throws IOException;

    /**
     * Cierra un flujo.
     *
     * @param streamId el identificador del flujo
     * @throws IOException si no se pudo cerrar
     */
    void closeStream(long streamId) throws IOException;

    /**
     * El proximo pedazo de un flujo.
     *
     * @param streamId el identificador del flujo
     * @return los bytes, o {@code null} cuando no queda nada
     * @throws IOException si no se pudo leer
     */
    byte[] readStream(long streamId) throws IOException;

    /**
     * Las opciones de una grabacion: nombre, duracion, destino, limites.
     *
     * @param recordingId el identificador
     * @return las opciones
     * @throws IllegalArgumentException si no existe esa grabacion
     */
    Map<String, String> getRecordingOptions(long recordingId) throws IllegalArgumentException;

    /**
     * Los ajustes de eventos de una grabacion.
     *
     * <p>Distinto de {@link #getRecordingOptions}: los ajustes dicen que grabar, las opciones dicen
     * como.
     *
     * @param recordingId el identificador
     * @return los ajustes
     * @throws IllegalArgumentException si no existe esa grabacion
     */
    Map<String, String> getRecordingSettings(long recordingId) throws IllegalArgumentException;

    /**
     * Fija los ajustes a partir del texto de un archivo {@code .jfc}.
     *
     * @param recordingId el identificador
     * @param contents el contenido del archivo
     * @throws IllegalArgumentException si no existe esa grabacion o el contenido no sirve
     */
    void setConfiguration(long recordingId, String contents) throws IllegalArgumentException;

    /**
     * Fija los ajustes a partir de una configuracion instalada, por nombre.
     *
     * @param recordingId el identificador
     * @param name el nombre, por ejemplo {@code "default"}
     * @throws IllegalArgumentException si no existe esa grabacion o esa configuracion
     */
    void setPredefinedConfiguration(long recordingId, String name) throws IllegalArgumentException;

    /**
     * Fija los ajustes de eventos.
     *
     * @param recordingId el identificador
     * @param settings los ajustes
     * @throws IllegalArgumentException si no existe esa grabacion
     */
    void setRecordingSettings(long recordingId, Map<String, String> settings)
            throws IllegalArgumentException;

    /**
     * Fija las opciones de la grabacion.
     *
     * @param recordingId el identificador
     * @param options las opciones
     * @throws IllegalArgumentException si no existe esa grabacion
     */
    void setRecordingOptions(long recordingId, Map<String, String> options)
            throws IllegalArgumentException;

    /**
     * Las grabaciones que hay en la VM remota.
     *
     * @return las grabaciones
     */
    List<RecordingInfo> getRecordings();

    /**
     * Las configuraciones instaladas en la VM remota.
     *
     * @return las configuraciones
     */
    List<ConfigurationInfo> getConfigurations();

    /**
     * Los tipos de evento que la VM remota conoce.
     *
     * @return los tipos
     */
    List<EventTypeInfo> getEventTypes();

    /**
     * Le pide a la VM remota que escriba la grabacion en su propio disco.
     *
     * @param recordingId el identificador
     * @param outputFile la ruta, interpretada en la maquina remota
     * @throws IOException si no se pudo escribir
     */
    void copyTo(long recordingId, String outputFile) throws IOException;
}
