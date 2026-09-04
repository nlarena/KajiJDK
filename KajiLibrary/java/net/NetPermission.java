package java.net;

import java.security.BasicPermission;

// Los permisos "de red" que en realidad no son de red: son permisos para **reconfigurar la
// plataforma**.
//
// Los nombres que define el JDK son cosas como "setDefaultAuthenticator", "setCookieHandler" o
// "setProxySelector": todos ellos autorizan a instalar un callback global. Y ahi esta el motivo de
// que existan -- quien puede reemplazar el `Authenticator` de la VM ve las contrasenas de todo el
// mundo, y quien puede reemplazar el `ProxySelector` desvia todo el trafico. No hace falta ninguna
// red para que eso sea peligroso, y no hace falta ninguna para representarlo.
//
// Toda la logica --nombres jerarquicos, comodin `*`, sin acciones-- la pone `BasicPermission`. Esta
// clase existe para ser un **tipo distinto**: una politica que otorga "setDefaultAuthenticator" no
// deberia otorgar de paso una propiedad del sistema con el mismo nombre.
//
// Nada omitido.
//
// @deprecated El Security Manager quedo deprecado para remocion; estos permisos ya no se chequean.
@Deprecated
public final class NetPermission extends BasicPermission {

    private static final long serialVersionUID = -8343910153355041693L;

    /**
     * @throws IllegalArgumentException si el nombre es vacio
     * @throws NullPointerException si el nombre es null
     */
    public NetPermission(String name) {
        super(name);
    }

    /**
     * Como el otro constructor; {@code actions} se ignora porque esta clase no tiene acciones.
     *
     * <p>Existe para la deserializacion y para encadenar desde subclases, no porque el argumento
     * signifique algo.
     */
    public NetPermission(String name, String actions) {
        super(name, actions);
    }
}
