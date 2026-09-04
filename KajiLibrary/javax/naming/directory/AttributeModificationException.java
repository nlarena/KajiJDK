package javax.naming.directory;

import javax.naming.NamingException;

/**
 * KajiLibrary's javax.naming.directory.AttributeModificationException -- una modificacion violo el
 * esquema.
 *
 * <p>Es la unica de este paquete que lleva datos propios: los {@link ModificationItem} que no se
 * pudieron aplicar. Hace falta porque {@code modifyAttributes} recibe una <b>lista</b> de
 * modificaciones y la especificacion pide que se apliquen todas o ninguna -- sin saber cual fallo,
 * no habria como arreglar el pedido.
 *
 * <p>Las modificaciones se ponen despues de construir la excepcion y no en el constructor. Es
 * incomodo y tiene su motivo: la capa que detecta el error suele ser mas profunda que la que sabe
 * que items venian en el pedido.
 */
public class AttributeModificationException extends NamingException {

    private static final long serialVersionUID = 8060676069678710186L;

    /** Las que no se aplicaron, o null si no se dijo. */
    private ModificationItem[] unexecs = null;

    /** Sin detalle. */
    public AttributeModificationException() {
        super();
    }

    /** Con un mensaje que diga cual fue el problema. */
    public AttributeModificationException(String explanation) {
        super(explanation);
    }

    /**
     * Deja las modificaciones que no se llegaron a aplicar.
     *
     * <p>Se guarda el arreglo tal cual, sin copiar, que es lo que hace el JDK.
     */
    public void setUnexecutedModifications(ModificationItem[] e) {
        this.unexecs = e;
    }

    /** Ver {@link #setUnexecutedModifications}; null si nadie las puso. */
    public ModificationItem[] getUnexecutedModifications() {
        return this.unexecs;
    }

    /**
     * El mensaje de {@code NamingException} y, si hay, la <b>primera</b> modificacion no aplicada.
     *
     * <p>La primera y no todas: es la que suele explicar el fallo, y una lista larga en un
     * {@code toString} hace ilegible cualquier registro.
     */
    public String toString() {
        String head = super.toString();
        if (this.unexecs == null || this.unexecs.length == 0) {
            return head;
        }
        return head + "First unexecuted modification: " + this.unexecs[0].toString();
    }
}
