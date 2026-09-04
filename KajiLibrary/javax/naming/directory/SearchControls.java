package javax.naming.directory;

import java.io.Serializable;

/**
 * KajiLibrary's javax.naming.directory.SearchControls -- cuanto buscar y que traer.
 *
 * <p>Seis opciones, y las tres primeras son las que deciden si una busqueda es viable o tumba el
 * directorio:
 *
 * <ul>
 *   <li>el <b>alcance</b>. {@link #OBJECT_SCOPE} mira una sola entrada, {@link #ONELEVEL_SCOPE} sus
 *       hijos directos, y {@link #SUBTREE_SCOPE} el arbol entero. El tercero sobre la raiz de un
 *       directorio grande es la forma clasica de hacer un pedido que tarda minutos;
 *   <li>el <b>limite de cantidad</b>, que corta despues de tantos resultados;
 *   <li>el <b>limite de tiempo</b>, en milisegundos.
 * </ul>
 *
 * <p>Los dos limites en 0 significan <b>sin limite</b>, que es el valor por omision de los dos. Vale
 * saberlo: los defaults de esta clase son los mas permisivos, no los mas seguros.
 *
 * <h2>Que atributos vuelven</h2>
 *
 * <p>{@link #setReturningAttributes} con null trae <b>todos</b>, y con un arreglo <b>vacio</b> no
 * trae ninguno. Los dos son utiles y confundirlos es facil: el arreglo vacio sirve para preguntar
 * "cuales entradas coinciden" sin traer sus datos, que sobre un directorio remoto es la diferencia
 * entre unos kilobytes y unos megabytes.
 *
 * <p>{@link #setReturningObjFlag} pide que ademas venga el <b>objeto</b> de cada entrada y no solo
 * sus atributos. Cuesta caro y por eso arranca apagado.
 */
public class SearchControls implements Serializable {

    private static final long serialVersionUID = -9138475345988518376L;

    /** Solo la entrada nombrada. */
    public static final int OBJECT_SCOPE = 0;

    /** Sus hijos directos, sin ella. */
    public static final int ONELEVEL_SCOPE = 1;

    /** Ella y todo su subarbol. */
    public static final int SUBTREE_SCOPE = 2;

    private int searchScope;

    private int timeLimit;

    private boolean derefLink;

    private boolean returnObj;

    private long countLimit;

    private String[] attributesToReturn;

    /**
     * Los valores por omision: un nivel, sin limites, todos los atributos, sin objetos.
     *
     * <p>Ver la nota de la clase: los limites por omision son ninguno.
     */
    public SearchControls() {
        this.searchScope = ONELEVEL_SCOPE;
        this.timeLimit = 0;
        this.countLimit = 0;
        this.derefLink = false;
        this.returnObj = false;
        this.attributesToReturn = null;
    }

    /**
     * Todo explicito.
     *
     * @param scope una de las tres constantes
     * @param countlim cuantos resultados como maximo; 0 es sin limite
     * @param timelim milisegundos; 0 es sin limite
     * @param attrs que atributos traer; null son todos y vacio es ninguno
     * @param retobj si ademas viene el objeto de cada entrada
     * @param deref si se siguen los enlaces
     */
    public SearchControls(int scope, long countlim, int timelim, String[] attrs, boolean retobj,
                          boolean deref) {
        this.searchScope = scope;
        this.timeLimit = timelim;
        this.derefLink = deref;
        this.returnObj = retobj;
        this.countLimit = countlim;
        this.attributesToReturn = attrs;
    }

    /** El alcance. */
    public int getSearchScope() {
        return this.searchScope;
    }

    /** El limite de tiempo en milisegundos; 0 es sin limite. */
    public int getTimeLimit() {
        return this.timeLimit;
    }

    /** Si se siguen los enlaces. */
    public boolean getDerefLinkFlag() {
        return this.derefLink;
    }

    /** Si ademas viene el objeto de cada entrada. */
    public boolean getReturningObjFlag() {
        return this.returnObj;
    }

    /** El limite de cantidad; 0 es sin limite. */
    public long getCountLimit() {
        return this.countLimit;
    }

    /** Que atributos traer. Ver la nota de la clase sobre null contra vacio. */
    public String[] getReturningAttributes() {
        return this.attributesToReturn;
    }

    /** Ver {@link #getSearchScope}. */
    public void setSearchScope(int scope) {
        this.searchScope = scope;
    }

    /** Ver {@link #getTimeLimit}. */
    public void setTimeLimit(int ms) {
        this.timeLimit = ms;
    }

    /** Ver {@link #getDerefLinkFlag}. */
    public void setDerefLinkFlag(boolean on) {
        this.derefLink = on;
    }

    /** Ver {@link #getReturningObjFlag}. */
    public void setReturningObjFlag(boolean on) {
        this.returnObj = on;
    }

    /** Ver {@link #getCountLimit}. */
    public void setCountLimit(long limit) {
        this.countLimit = limit;
    }

    /** Ver {@link #getReturningAttributes}. */
    public void setReturningAttributes(String[] attrs) {
        this.attributesToReturn = attrs;
    }
}
