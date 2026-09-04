package jdk.net;

import java.security.BasicPermission;

/**
 * El permiso que protege las opciones de socket de {@link ExtendedSocketOptions}.
 *
 * <p>Las opciones extendidas no son inocuas: varias tocan el comportamiento del nucleo, y
 * {@link ExtendedSocketOptions#SO_PEERCRED} devuelve la identidad de otro proceso. De ahi que
 * usarlas sea una accion con permiso propio y no simplemente una llamada mas.
 *
 * <p>Extiende {@link BasicPermission}, asi que el nombre admite comodines: {@code "*"} da todos,
 * {@code "setOption.*"} da los de una familia. No tiene acciones — el segundo constructor las
 * acepta y las ignora, y esta solo porque el mecanismo de permisos construye por reflexion con dos
 * argumentos.
 */
public final class NetworkPermission extends BasicPermission {

    private static final long serialVersionUID = -2004683231018171266L;

    /** Un permiso con ese nombre. */
    public NetworkPermission(String name) {
        super(name);
    }

    /** Igual; {@code actions} se ignora, y el JDK hace lo mismo. */
    public NetworkPermission(String name, String actions) {
        super(name, actions);
    }
}
