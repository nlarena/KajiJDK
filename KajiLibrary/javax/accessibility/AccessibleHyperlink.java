package javax.accessibility;

/**
 * Un enlace dentro de un texto.
 *
 * <p>Es una {@link AccessibleAction} porque un enlace es, sobre todo, algo que se puede **hacer**.
 * Lo que agrega es dónde está: el tramo de texto que ocupa, para que quien lea pueda anunciarlo en
 * su lugar y no al final.
 *
 * <p>{@link #isValid} existe porque el documento puede cambiar debajo: un enlace que se obtuvo antes
 * de una edición puede estar apuntando a un tramo que ya no existe.
 */
public abstract class AccessibleHyperlink implements AccessibleAction {

    /** Para las subclases. */
    protected AccessibleHyperlink() {
    }

    /** Si el enlace sigue apuntando a un tramo que existe. */
    public abstract boolean isValid();

    /** Cuántas acciones tiene; para un enlace, normalmente una. */
    public abstract int getAccessibleActionCount();

    /**
     * Sigue el enlace.
     *
     * @return `true` si se pudo
     */
    public abstract boolean doAccessibleAction(int i);

    /** El texto del enlace. */
    public abstract String getAccessibleActionDescription(int i);

    /** A dónde apunta: normalmente una `URL`. */
    public abstract Object getAccessibleActionObject(int i);

    /** Lo que se muestra como enlace: un texto o una imagen. */
    public abstract Object getAccessibleActionAnchor(int i);

    /** Dónde empieza el tramo del enlace. */
    public abstract int getStartIndex();

    /** Dónde termina. */
    public abstract int getEndIndex();
}
