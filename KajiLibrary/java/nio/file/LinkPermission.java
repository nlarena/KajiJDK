package java.nio.file;

import java.security.BasicPermission;

// El permiso para crear enlaces: `"hard"` para los duros, `"symbolic"` para los simbolicos.
//
// Solo esos dos nombres, y por eso el constructor los valida: `BasicPermission` acepta comodines
// como `"*"`, y aca un comodin daria un permiso mas amplio que ninguno de los dos que existen.
//
// KajiJDK no crea enlaces --no hay nativo-- asi que nadie lo consulta; el tipo esta porque la firma
// de las excepciones de la spec lo nombra y porque codigo que lo construye tiene que compilar.
public final class LinkPermission extends BasicPermission {

    private static final long serialVersionUID = -1441492453772213220L;

    private void comprobar(String name) {
        if (!name.equals("hard") && !name.equals("symbolic")) {
            throw new IllegalArgumentException("name: " + name);
        }
    }

    /**
     * @param name `"hard"` o `"symbolic"`
     * @throws IllegalArgumentException si es cualquier otra cosa
     */
    public LinkPermission(String name) {
        super(name);
        this.comprobar(name);
    }

    /**
     * Igual que el otro; `actions` tiene que estar vacio o ser `null`.
     *
     * <p>Este permiso no tiene acciones: el nombre ya dice todo. La sobrecarga existe porque la
     * maquinaria de politicas de seguridad construye permisos por reflexion con dos argumentos
     * siempre.
     */
    public LinkPermission(String name, String actions) {
        super(name);
        this.comprobar(name);
        if (actions != null && actions.length() > 0) {
            throw new IllegalArgumentException("actions: " + actions);
        }
    }
}
