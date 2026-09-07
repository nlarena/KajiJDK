package pq;

/**
 * El control: la misma jerarquia pero con el constructor escrito.
 *
 * <p>Compila en los dos lados. Sirve para separar "heredar de una clase sin constructor vacio no
 * anda" de "el constructor por omision no se valida", que es lo que pasa.
 */
public abstract class Control extends Base {

    public Control(Object carga) {
        super(carga);
    }

    public abstract int f();
}
