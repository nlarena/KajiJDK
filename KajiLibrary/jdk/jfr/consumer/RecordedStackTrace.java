package jdk.jfr.consumer;

import java.util.Collections;
import java.util.List;

import jdk.jfr.ValueDescriptor;

/**
 * Una pila de llamadas grabada.
 *
 * <h2>Por que puede estar truncada</h2>
 *
 * <p>Porque las pilas profundas cuestan: caminarlas lleva tiempo y guardarlas lleva espacio, y JFR
 * tiene un tope configurable. {@link #isTruncated} dice si se llego a ese tope.
 *
 * <p>Ignorarlo lleva a una conclusion falsa clasica: agrupar por el marco mas profundo y creer que
 * ahi esta el costo, cuando en las pilas truncadas ese marco es simplemente donde JFR dejo de
 * mirar.
 *
 * <p>Los marcos vienen del mas reciente al mas antiguo, igual que en un volcado de pila.
 *
 * @since 9
 */
public final class RecordedStackTrace extends RecordedObject {

    RecordedStackTrace(List<ValueDescriptor> descriptores, Object[] valores) {
        super(descriptores, valores);
    }

    /**
     * Los marcos, del mas reciente al mas antiguo.
     *
     * @return los marcos
     */
    public List<RecordedFrame> getFrames() {
        final List<RecordedFrame> v = getValue("frames");
        return v == null ? Collections.<RecordedFrame>emptyList() : v;
    }

    /**
     * Si la pila se corto por llegar al tope de profundidad.
     *
     * @return si esta truncada
     */
    public boolean isTruncated() {
        return getBoolean("truncated");
    }
}
