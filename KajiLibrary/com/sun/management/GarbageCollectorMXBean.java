package com.sun.management;

/**
 * Un recolector de basura, con el agregado de poder mirar la <strong>ultima</strong> recoleccion.
 *
 * <p>La interfaz estandar {@link java.lang.management.GarbageCollectorMXBean} solo da acumulados:
 * cuantas recolecciones hubo y cuanto tiempo sumaron. Sirve para una tendencia y no sirve para
 * diagnosticar, porque un promedio esconde justamente la pausa que interesa.
 *
 * <p>{@link #getLastGcInfo} es lo que falta: de la ultima recoleccion da cuando empezo, cuando
 * termino y como quedo cada region de memoria antes y despues. Con eso se puede decir si una pausa
 * concreta recupero algo o fue en vano.
 *
 * @since 1.5
 */
public interface GarbageCollectorMXBean extends java.lang.management.GarbageCollectorMXBean {

    /**
     * Los datos de la ultima recoleccion de este recolector.
     *
     * @return los datos, o {@code null} si todavia no hubo ninguna
     */
    GcInfo getLastGcInfo();
}
