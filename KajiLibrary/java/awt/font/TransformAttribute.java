package java.awt.font;

import java.awt.geom.AffineTransform;
import java.io.Serializable;

/**
 * Una {@link AffineTransform} envuelta para poder usarse como valor de atributo de texto.
 *
 * <p>La envoltura no es burocracia. `AffineTransform` es **mutable**, y un atributo de texto se
 * guarda en mapas y se comparte entre tramos: si el valor fuera la transformación misma, cambiarla
 * desde afuera cambiaría el estilo de todo lo que la usara. Acá se copia al entrar y al salir, así
 * que lo que se guarda es un valor.
 *
 * <p>Una transformación identidad se guarda como `null` adentro. Es la forma de que
 * {@link #isIdentity} sea una comparación contra `null` y no un recorrido de seis coeficientes, que
 * es una pregunta que se hace por cada tramo de texto que se dibuja.
 */
public final class TransformAttribute implements Serializable {

    private static final long serialVersionUID = 3356247357827709530L;

    /** La transformación que no hace nada. */
    public static final TransformAttribute IDENTITY = new TransformAttribute(null);

    private final AffineTransform transform;

    /**
     * Con la transformación dada, que se copia.
     *
     * <p>Acepta `null` y lo toma como la identidad. La documentación del JDK dice que tira, pero su
     * implementación lo acepta, y es lo que hace lo que vale.
     */
    public TransformAttribute(AffineTransform transform) {
        if (transform != null && !transform.isIdentity()) {
            this.transform = new AffineTransform(transform);
        } else {
            this.transform = null;
        }
    }

    /** Una copia de la transformación; la identidad si no hay. */
    public AffineTransform getTransform() {
        AffineTransform at = this.transform;
        if (at == null) {
            return new AffineTransform();
        }
        return new AffineTransform(at);
    }

    /** Si no hace nada. */
    public boolean isIdentity() {
        return this.transform == null;
    }

    public int hashCode() {
        if (this.transform == null) {
            return 0;
        }
        return this.transform.hashCode();
    }

    /** Igualdad por la transformación que envuelve. */
    public boolean equals(Object rhs) {
        if (rhs == null) {
            return false;
        }
        if (rhs == this) {
            return true;
        }
        if (rhs.getClass() != this.getClass()) {
            return false;
        }
        TransformAttribute that = (TransformAttribute) rhs;
        if (this.transform == null) {
            return that.transform == null;
        }
        return this.transform.equals(that.transform);
    }
}
