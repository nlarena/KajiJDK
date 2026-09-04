package javax.swing.undo;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Una edicion que deshace <strong>comparando fotos del estado</strong>, no revirtiendo la accion.
 *
 * <h2>Como se usa, y por que en dos tiempos</h2>
 *
 * <p>El constructor saca la foto de antes; {@link #end} saca la de despues. Entre las dos llamadas
 * va el cambio real, que esta clase nunca ve — de ahi que sirva para operaciones cuya inversa nadie
 * quiere escribir.
 *
 * <p>Deshacer es poner la foto vieja y rehacer es poner la nueva. Las dos operaciones son la misma
 * llamada con distinta tabla, que es lo que hace a esta clase tan corta.
 *
 * <h2>{@link #removeRedundantState}, que es lo que la hace practica</h2>
 *
 * <p>Sin esa poda, cada edicion guardaria el estado <em>entero</em> del objeto dos veces, aunque
 * hubiera cambiado un solo campo. El metodo saca de las dos tablas las claves cuyo valor no cambio,
 * asi que lo que queda es la diferencia. Es la razon de que el enfoque por fotos no sea
 * inmediatamente inviable en memoria.
 */
public class StateEdit extends AbstractUndoableEdit {

    private static final long serialVersionUID = 5297308062724130866L;

    /** Identificador de version del JDK; se conserva por fidelidad de la superficie. */
    protected static final String RCSID = "$Id: StateEdit.java,v 1.6 1997/10/01 20:05:51 sandipc Exp $";

    /** El objeto cuyo estado se fotografia. */
    protected StateEditable object;

    /** La foto de antes del cambio. */
    protected Hashtable<Object, Object> preState;

    /** La foto de despues, que llena {@link #end}. */
    protected Hashtable<Object, Object> postState;

    /** El nombre para mostrar. */
    protected String undoRedoName;

    /** Saca la foto de antes, sin nombre. */
    public StateEdit(StateEditable anObject) {
        super();
        init(anObject, null);
    }

    /** Saca la foto de antes, con un nombre para mostrar. */
    public StateEdit(StateEditable anObject, String name) {
        super();
        init(anObject, name);
    }

    /**
     * Guarda el objeto y le pide la foto de antes.
     *
     * <p>{@code protected} y separada del constructor porque los dos constructores hacen lo mismo:
     * es el lugar unico donde una subclase puede meterse.
     */
    protected void init(StateEditable anObject, String name) {
        this.object = anObject;
        this.preState = new Hashtable<Object, Object>(11);
        this.object.storeState(this.preState);
        this.postState = null;
        this.undoRedoName = name;
    }

    /** Saca la foto de despues y poda lo que no cambio. */
    public void end() {
        this.postState = new Hashtable<Object, Object>(11);
        this.object.storeState(this.postState);
        removeRedundantState();
    }

    /** Le pone al objeto la foto de antes. */
    public void undo() {
        super.undo();
        this.object.restoreState(this.preState);
    }

    /** Le pone al objeto la foto de despues. */
    public void redo() {
        super.redo();
        this.object.restoreState(this.postState);
    }

    public String getPresentationName() {
        return this.undoRedoName;
    }

    /**
     * Saca de las dos fotos las claves cuyo valor no cambio.
     *
     * <p>Se recorre la foto vieja y se compara contra la nueva; lo que coincide sale de las dos. Una
     * clave que solo esta en una de las dos <strong>si</strong> es un cambio —aparecio o
     * desaparecio— y por eso no se toca.
     */
    protected void removeRedundantState() {
        java.util.Vector<Object> aSacar = new java.util.Vector<Object>();
        Enumeration<Object> claves = this.preState.keys();
        while (claves.hasMoreElements()) {
            Object clave = claves.nextElement();
            if (this.postState.containsKey(clave)) {
                Object antes = this.preState.get(clave);
                Object despues = this.postState.get(clave);
                if (antes.equals(despues)) {
                    aSacar.addElement(clave);
                }
            }
        }
        for (int i = 0; i < aSacar.size(); i++) {
            Object clave = aSacar.elementAt(i);
            this.preState.remove(clave);
            this.postState.remove(clave);
        }
    }
}
