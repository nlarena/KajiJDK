package java.awt;

import java.security.BasicPermission;

/**
 * Un permiso del AWT: "mostrar una ventana sin el cartel de advertencia", "leer el portapapeles",
 * "mover el mouse por codigo".
 *
 * <p>No agrega ni un metodo a {@code BasicPermission}: toda la logica --el comodin {@code "*"}, la
 * comparacion por nombre, la coleccion de permisos-- ya esta ahi. Existe solo para que el nombre de
 * la clase distinga la familia, que es como se escriben las politicas de seguridad.
 *
 * <p>El constructor con acciones ignora el segundo parametro. Esta en la API porque toda subclase
 * de {@code Permission} tiene que tenerlo para que el cargador de politicas pueda instanciarla por
 * reflexion; un AWTPermission no tiene acciones y {@code getActions()} devuelve la cadena vacia.
 */
public final class AWTPermission extends BasicPermission {

    private static final long serialVersionUID = 8890392402588814465L;

    public AWTPermission(String name) {
        super(name);
    }

    public AWTPermission(String name, String actions) {
        super(name, actions);
    }
}
