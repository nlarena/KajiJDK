package javax.naming;

/**
 * El enlace simbolico de JNDI: una atadura cuyo contenido es **otro nombre**.
 *
 * <p>Es un `Reference` con una sola direccion, de tipo `"LinkAddress"`, cuyo contenido es el nombre
 * apuntado. Que sea un `Reference` y no un tipo aparte no es casualidad: asi cualquier proveedor
 * que ya sabe guardar referencias sabe guardar enlaces sin cambiar nada, y el enlace sobrevive a
 * ida y vuelta por la red igual que el resto.
 *
 * <p>Lo que lo hace especial esta del lado del que resuelve, no aca: `Context.lookup()` **sigue**
 * los enlaces --devuelve el objeto del otro lado-- y `Context.lookupLink()` no --devuelve este
 * objeto--. Ese par de metodos es toda la diferencia entre "seguime el enlace" y "mostrame el
 * enlace", y es la razon por la que `lookupLink` existe.
 *
 * <p>Un enlace apunta a un nombre relativo al **contexto inicial**, no al contexto donde esta
 * atado. Es lo contrario de lo que uno espera de un enlace de sistema de archivos y es del
 * contrato: un enlace no cambia de destino cuando se lo mira desde otro lado.
 */
public class LinkRef extends Reference {

    private static final long serialVersionUID = -5386290613498931298L;

    /** La clase que se declara en la referencia; el `getLinkName` verifica contra esto. */
    static final String linkClassName = LinkRef.class.getName();

    /** El tipo de direccion bajo el que va el nombre apuntado. */
    static final String linkAddrType = "LinkAddress";

    public LinkRef(Name linkName) {
        super(linkClassName, new StringRefAddr(linkAddrType, linkName.toString()));
    }

    public LinkRef(String linkName) {
        super(linkClassName, new StringRefAddr(linkAddrType, linkName));
    }

    /**
     * El nombre apuntado.
     *
     * <p>Verifica en vez de confiar porque los campos de `Reference` son `protected` y mutables:
     * nada impide que a un `LinkRef` le cambien la clase o le saquen la direccion, y ahi no hay un
     * nombre que devolver. Por eso tira `MalformedLinkException` --que es lo que significa "esto
     * dice ser un enlace y no lo es"-- en vez de `null` o una `NullPointerException`.
     */
    public String getLinkName() throws NamingException {
        if (className != null && className.equals(linkClassName)) {
            RefAddr addr = get(linkAddrType);
            if (addr instanceof StringRefAddr) {
                return (String) addr.getContent();
            }
        }
        throw new MalformedLinkException();
    }
}
