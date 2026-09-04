package com.sun.management;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataView;
import javax.management.openmbean.CompositeType;

/**
 * El aviso de que hubo una recoleccion de basura, con lo necesario para saber cual y como salio.
 *
 * <h2>Por que un aviso y no una consulta</h2>
 *
 * <p>Porque las recolecciones ocurren cuando quieren. Un monitor que preguntara periodicamente por
 * {@link GarbageCollectorMXBean#getLastGcInfo} se perderia todas las que pasaron entre dos
 * consultas, que en una carga alta son casi todas. Suscribirse al aviso es la unica forma de verlas
 * todas sin consultar sin parar.
 *
 * <h2>La causa y la accion, que es lo que se lee primero</h2>
 *
 * <p>{@link #getGcCause} dice <strong>por que</strong> arranco: porque se lleno el eden, porque
 * alguien llamo a {@code System.gc()}, porque el metaespacio se quedo corto. Es el campo que separa
 * una recoleccion normal de un sintoma. {@link #getGcAction} dice que hizo — si fue menor o mayor.
 *
 * <p>Un solo campo de esos vale mas que el tiempo total: mil recolecciones menores por eden lleno
 * son el funcionamiento normal, y tres mayores por {@code System.gc()} son un problema de codigo.
 *
 * <h2>Como llega</h2>
 *
 * <p>Dentro de una {@link javax.management.Notification} cuyo tipo es
 * {@link #GARBAGE_COLLECTION_NOTIFICATION}. Lo que viaja en los datos de usuario es un
 * {@link CompositeData}; {@link #from} lo vuelve a convertir en este objeto del lado del que
 * escucha.
 *
 * @since 1.7
 */
public class GarbageCollectionNotificationInfo implements CompositeDataView {

    /** El tipo de notificacion que lleva uno de estos. */
    public static final String GARBAGE_COLLECTION_NOTIFICATION =
            "com.sun.management.gc.notification";

    private final String gcName;
    private final String gcAction;
    private final String gcCause;
    private final GcInfo gcInfo;

    /**
     * Un aviso.
     *
     * @param gcName el nombre del recolector
     * @param gcAction que hizo
     * @param gcCause por que arranco
     * @param gcInfo los datos de la recoleccion
     * @throws NullPointerException si alguno es {@code null}
     */
    public GarbageCollectionNotificationInfo(final String gcName, final String gcAction,
            final String gcCause, final GcInfo gcInfo) {
        if (gcName == null) {
            throw new NullPointerException("gcName");
        }
        if (gcAction == null) {
            throw new NullPointerException("gcAction");
        }
        if (gcCause == null) {
            throw new NullPointerException("gcCause");
        }
        if (gcInfo == null) {
            throw new NullPointerException("gcInfo");
        }
        this.gcName = gcName;
        this.gcAction = gcAction;
        this.gcCause = gcCause;
        this.gcInfo = gcInfo;
    }

    /**
     * El nombre del recolector, el mismo que da su MXBean.
     *
     * @return el nombre
     */
    public String getGcName() {
        return gcName;
    }

    /**
     * Que hizo esta recoleccion, en texto libre.
     *
     * <p>Texto y no un enum porque cada recolector describe sus fases a su manera, y fijar un
     * conjunto cerrado habria dejado afuera a todos los recolectores futuros.
     *
     * @return la accion
     */
    public String getGcAction() {
        return gcAction;
    }

    /**
     * Por que arranco, en texto libre.
     *
     * @return la causa
     */
    public String getGcCause() {
        return gcCause;
    }

    /**
     * Los datos de la recoleccion.
     *
     * @return los datos
     */
    public GcInfo getGcInfo() {
        return gcInfo;
    }

    /**
     * Reconstruye el aviso desde su forma abierta.
     *
     * @param cd la forma abierta, o {@code null}
     * @return el aviso, o {@code null} si {@code cd} era {@code null}
     * @throws IllegalArgumentException si {@code cd} no tiene la forma de este aviso
     */
    public static GarbageCollectionNotificationInfo from(final CompositeData cd) {
        if (cd == null) {
            return null;
        }
        if (!cd.containsKey("gcName") || !cd.containsKey("gcAction")
                || !cd.containsKey("gcCause") || !cd.containsKey("gcInfo")) {
            throw new IllegalArgumentException(
                    "el CompositeData no tiene la forma de un GarbageCollectionNotificationInfo");
        }
        return new GarbageCollectionNotificationInfo((String) cd.get("gcName"),
                (String) cd.get("gcAction"), (String) cd.get("gcCause"),
                GcInfo.from((CompositeData) cd.get("gcInfo")));
    }

    /**
     * La forma abierta de este aviso.
     *
     * @param ct el tipo pedido
     * @return la forma abierta
     * @throws UnsupportedOperationException si no hay como armarla
     */
    public CompositeData toCompositeData(final CompositeType ct) {
        // Armar el valor abierto necesita construir el CompositeType anidado del GcInfo, que a su
        // vez necesita el TabularType de los dos mapas de MemoryUsage. Eso lo produce la VM cuando
        // emite el aviso; del lado del que escucha nunca hace falta, porque lo que llega ya viene
        // en forma abierta y el camino que se usa es `from`.
        throw new UnsupportedOperationException(
                "la forma abierta de este aviso la construye la VM que lo emite");
    }
}
