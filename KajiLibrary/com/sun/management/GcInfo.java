package com.sun.management;

import java.lang.management.MemoryUsage;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataView;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.TabularData;

/**
 * Los datos de una recoleccion de basura concreta: cuando fue y como quedo la memoria.
 *
 * <h2>Los dos mapas, que son el dato central</h2>
 *
 * <p>{@link #getMemoryUsageBeforeGc} y {@link #getMemoryUsageAfterGc} tienen una entrada por
 * <strong>region</strong> de memoria — eden, superviviente, vieja, metaespacio— y no un total. Eso
 * es lo que permite decir algo util: una recoleccion que vacio el eden y no movio la region vieja
 * fue barata y sana; una que redujo el eden y ademas hizo crecer la vieja acaba de promover objetos
 * que van a costar caros despues.
 *
 * <p>Un total antes y despues no distingue esos dos casos, y son opuestos.
 *
 * <h2>Por que implementa {@link CompositeData}</h2>
 *
 * <p>Para poder viajar por JMX sin que el cliente tenga esta clase. Un monitor remoto recibe un
 * valor compuesto generico, con los mismos items; si ademas tiene esta clase en su classpath, usa
 * {@link #from} y recupera los accesores tipados.
 *
 * <p>Por eso no hay constructor publico: un {@code GcInfo} lo produce la VM al recolectar, o se
 * reconstruye desde su forma abierta. Fabricar uno a mano seria inventar una recoleccion que no
 * ocurrio.
 *
 * @since 1.5
 */
public class GcInfo implements CompositeData, CompositeDataView {

    private final CompositeData cdata;
    private final long id;
    private final long startTime;
    private final long endTime;
    private final Map<String, MemoryUsage> usageBeforeGc;
    private final Map<String, MemoryUsage> usageAfterGc;

    private GcInfo(final CompositeData cd) {
        this.cdata = cd;
        this.id = ((Long) cd.get("id")).longValue();
        this.startTime = ((Long) cd.get("startTime")).longValue();
        this.endTime = ((Long) cd.get("endTime")).longValue();
        this.usageBeforeGc = leerMapa(cd, "memoryUsageBeforeGc");
        this.usageAfterGc = leerMapa(cd, "memoryUsageAfterGc");
    }

    /**
     * Un {@code Map<String, MemoryUsage>} sale de la forma abierta como una tabla de filas
     * {@code (key, value)}.
     *
     * <p>Es la unica forma que el sistema de tipos abiertos tiene de representar un mapa: no hay un
     * "MapType", asi que se codifica como una tabla indexada por la clave. Deshacer esa
     * codificacion es todo lo que hace este metodo.
     */
    private static Map<String, MemoryUsage> leerMapa(final CompositeData cd, final String item) {
        if (!cd.containsKey(item)) {
            return Collections.emptyMap();
        }
        final Object valor = cd.get(item);
        if (!(valor instanceof TabularData)) {
            return Collections.emptyMap();
        }
        final Map<String, MemoryUsage> out = new TreeMap<String, MemoryUsage>();
        for (final Object fila : ((TabularData) valor).values()) {
            final CompositeData f = (CompositeData) fila;
            out.put((String) f.get("key"), MemoryUsage.from((CompositeData) f.get("value")));
        }
        return Collections.unmodifiableMap(out);
    }

    /**
     * El numero de esta recoleccion, dentro de las de su recolector.
     *
     * <p>Es un contador por recolector, no global: el numero 7 del recolector joven y el 7 del
     * viejo no tienen nada que ver.
     *
     * @return el numero
     */
    public long getId() {
        return id;
    }

    /**
     * Cuando empezo, en milisegundos desde que arranco la VM.
     *
     * <p>Desde el arranque de la VM y no desde la epoca: lo que se quiere medir es una duracion
     * dentro de esta ejecucion, y un reloj de pared puede saltar hacia atras.
     *
     * @return los milisegundos desde el arranque
     */
    public long getStartTime() {
        return startTime;
    }

    /**
     * Cuando termino, en milisegundos desde que arranco la VM.
     *
     * @return los milisegundos desde el arranque
     */
    public long getEndTime() {
        return endTime;
    }

    /**
     * Cuanto duro, en milisegundos.
     *
     * <p>No es necesariamente la pausa que sufrio la aplicacion: un recolector concurrente trabaja
     * mientras los hilos siguen andando, y ahi esta duracion es mucho mayor que la pausa real.
     *
     * @return la duracion
     */
    public long getDuration() {
        return endTime - startTime;
    }

    /**
     * Como estaba cada region antes de recolectar.
     *
     * @return el mapa, de nombre de region a su uso
     */
    public Map<String, MemoryUsage> getMemoryUsageBeforeGc() {
        return usageBeforeGc;
    }

    /**
     * Como quedo cada region despues de recolectar.
     *
     * @return el mapa, de nombre de region a su uso
     */
    public Map<String, MemoryUsage> getMemoryUsageAfterGc() {
        return usageAfterGc;
    }

    /**
     * Reconstruye un {@code GcInfo} desde su forma abierta.
     *
     * @param cd la forma abierta, o {@code null}
     * @return el objeto, o {@code null} si {@code cd} era {@code null}
     * @throws IllegalArgumentException si {@code cd} no tiene la forma de un {@code GcInfo}
     */
    public static GcInfo from(final CompositeData cd) {
        if (cd == null) {
            return null;
        }
        if (!cd.containsKey("id") || !cd.containsKey("startTime") || !cd.containsKey("endTime")) {
            throw new IllegalArgumentException("el CompositeData no tiene la forma de un GcInfo");
        }
        return new GcInfo(cd);
    }

    // ---- CompositeData, delegado en el valor abierto del que salio ----
    //
    // Delegar y no reimplementar: los items son los que el productor puso, y una VM puede agregar
    // los suyos. Contestar desde los campos de arriba haria desaparecer todo lo que esta clase no
    // conoce, que es justamente lo que un monitor generico querria ver.

    /** {@inheritDoc} */
    public boolean containsKey(final String key) {
        return cdata.containsKey(key);
    }

    /** {@inheritDoc} */
    public boolean containsValue(final Object value) {
        return cdata.containsValue(value);
    }

    /** {@inheritDoc} */
    public Object get(final String key) {
        return cdata.get(key);
    }

    /** {@inheritDoc} */
    public Object[] getAll(final String[] keys) {
        return cdata.getAll(keys);
    }

    /** {@inheritDoc} */
    public CompositeType getCompositeType() {
        return cdata.getCompositeType();
    }

    /** {@inheritDoc} */
    public Collection<?> values() {
        return cdata.values();
    }

    /** {@inheritDoc} */
    public boolean equals(final Object obj) {
        return cdata.equals(obj);
    }

    /** {@inheritDoc} */
    public int hashCode() {
        return cdata.hashCode();
    }

    /** {@inheritDoc} */
    public String toString() {
        return cdata.toString();
    }

    /**
     * La forma abierta de este objeto.
     *
     * <p>Devuelve el valor del que salio, sin volver a armarlo: es el que tiene todos los items,
     * incluidos los que esta clase no interpreta.
     *
     * @param ct el tipo pedido, que se ignora por lo dicho arriba
     * @return la forma abierta
     */
    public CompositeData toCompositeData(final CompositeType ct) {
        return cdata;
    }
}
