package javax.naming;

/**
 * Falla al seguir un enlace, con el estado del enlace **aparte** del estado del contexto.
 *
 * <p>Un enlace (`LinkRef`) es un nombre atado a otro nombre: resolver `a/b` puede llevar a un
 * enlace que dice "en realidad esto es `x/y/z`", y la resolucion sigue por ahi. Cuando falla,
 * hay **dos** resoluciones en juego y las dos importan: la del nombre original y la del nombre
 * del enlace. Los cuatro campos heredados de `NamingException` cuentan la primera; los cuatro
 * `link*` de esta clase cuentan la segunda.
 *
 * <p>Sin esa separacion el diagnostico seria inutil: "no encontre `z`" no dice si `z` era parte
 * del nombre que se pidio o del nombre al que el enlace mandaba.
 *
 * <p>El resto de la jerarquia esta explicado en `NamingException`.
 */
public class LinkException extends NamingException {

    private static final long serialVersionUID = -7967662604076777712L;

    /** Hasta donde se resolvio **el nombre del enlace**. */
    protected Name linkResolvedName;

    /** El objeto al que se llego resolviendo el nombre del enlace. */
    protected Object linkResolvedObj;

    /** Lo que faltaba del nombre del enlace. */
    protected Name linkRemainingName;

    /** El porque, en texto, de la falla del enlace. Es el paralelo de `getExplanation()`. */
    protected String linkExplanation;

    public LinkException(String explanation) {
        super(explanation);
        linkResolvedName = null;
        linkResolvedObj = null;
        linkRemainingName = null;
        linkExplanation = null;
    }

    public LinkException() {
        super();
        linkResolvedName = null;
        linkResolvedObj = null;
        linkRemainingName = null;
        linkExplanation = null;
    }

    public Name getLinkResolvedName() {
        return this.linkResolvedName;
    }

    public Name getLinkRemainingName() {
        return this.linkRemainingName;
    }

    public Object getLinkResolvedObj() {
        return this.linkResolvedObj;
    }

    public String getLinkExplanation() {
        return this.linkExplanation;
    }

    public void setLinkExplanation(String msg) {
        this.linkExplanation = msg;
    }

    // Clonan por la misma razon que los de `NamingException`: el proveedor sigue usando su copia
    // del nombre despues de haber lanzado.

    public void setLinkResolvedName(Name name) {
        this.linkResolvedName = (name != null) ? (Name) name.clone() : null;
    }

    public void setLinkRemainingName(Name name) {
        this.linkRemainingName = (name != null) ? (Name) name.clone() : null;
    }

    public void setLinkResolvedObj(Object obj) {
        this.linkResolvedObj = obj;
    }

    @Override
    public String toString() {
        return super.toString() + "; Link Remaining Name: '" + this.linkRemainingName + "'";
    }

    @Override
    public String toString(boolean detail) {
        if (!detail || this.linkResolvedObj == null) {
            return this.toString();
        }
        return this.toString() + "; Link Resolved Object: " + this.linkResolvedObj;
    }
}
