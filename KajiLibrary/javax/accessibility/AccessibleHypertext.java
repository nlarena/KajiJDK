package javax.accessibility;

/**
 * Texto accesible con enlaces adentro.
 *
 * <p>{@link #getLinkIndex} es la operación que importa: dado un punto del texto, dice qué enlace lo
 * contiene. Es lo que permite anunciar "esto es un enlace" mientras se recorre el texto, en vez de
 * tener que listar los enlaces por separado y perder dónde estaban.
 */
public interface AccessibleHypertext extends AccessibleText {

    /** Cuántos enlaces hay. */
    int getLinkCount();

    /**
     * El `linkIndex`-ésimo enlace.
     *
     * @return el enlace, o `null` si no hay tantos
     */
    AccessibleHyperlink getLink(int linkIndex);

    /**
     * Qué enlace contiene a ese carácter.
     *
     * @return el número de enlace, o -1 si ese carácter no está en ninguno
     */
    int getLinkIndex(int charIndex);
}
