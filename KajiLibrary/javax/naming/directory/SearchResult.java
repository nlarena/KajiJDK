package javax.naming.directory;

import javax.naming.Binding;

/**
 * KajiLibrary's javax.naming.directory.SearchResult -- una entrada que coincidio con la busqueda.
 *
 * <p>Un {@link Binding} --nombre y objeto-- mas los atributos que se pidieron. Extiende
 * {@code Binding} y no lo copia porque un resultado de busqueda <b>es</b> una asociacion nombre-objeto;
 * lo que agrega el directorio son los atributos.
 *
 * <p>El nombre puede ser relativo o absoluto y eso importa al usarlo: con
 * {@link javax.naming.NameClassPair#isRelative} en false, el nombre es un URL completo y no se puede
 * pasar a {@code lookup} del mismo contexto. Pasa cuando la busqueda cruzo una referencia a otro
 * servidor.
 *
 * <p>El objeto viene solo si se pidio con {@link SearchControls#setReturningObjFlag}; si no, es null
 * y lo unico util son el nombre y los atributos.
 */
public class SearchResult extends Binding {

    private static final long serialVersionUID = -9158063327699723172L;

    /** Los atributos que se pidieron traer. */
    private Attributes attrs;

    /** Nombre relativo, objeto y atributos. */
    public SearchResult(String name, Object obj, Attributes attrs) {
        super(name, obj);
        this.attrs = attrs;
    }

    /** Idem, diciendo si el nombre es relativo. */
    public SearchResult(String name, Object obj, Attributes attrs, boolean isRelative) {
        super(name, obj, isRelative);
        this.attrs = attrs;
    }

    /** Idem, con el nombre de clase explicito. */
    public SearchResult(String name, String className, Object obj, Attributes attrs) {
        super(name, className, obj);
        this.attrs = attrs;
    }

    /** Todo explicito. */
    public SearchResult(String name, String className, Object obj, Attributes attrs,
                        boolean isRelative) {
        super(name, className, obj, isRelative);
        this.attrs = attrs;
    }

    /** Los atributos. */
    public Attributes getAttributes() {
        return this.attrs;
    }

    /** Ver {@link #getAttributes}. */
    public void setAttributes(Attributes attrs) {
        this.attrs = attrs;
    }

    /** Lo del {@link Binding} mas los atributos. */
    public String toString() {
        return super.toString() + ":" + getAttributes();
    }
}
