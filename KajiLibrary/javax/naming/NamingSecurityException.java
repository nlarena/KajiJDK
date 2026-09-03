package javax.naming;

/**
 * Raiz abstracta de las tres fallas de seguridad, para poder atraparlas juntas.

 * <p>Es `abstract` porque "problema de seguridad" no es una causa: es una **categoria**. Lanzarla
 * tal cual no le diria al que la atrapa si le faltan credenciales, si el mecanismo no se soporta o
 * si simplemente no tiene permiso, que son tres reacciones distintas.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public abstract class NamingSecurityException extends NamingException {

    private static final long serialVersionUID = 5855287647294685775L;

    public NamingSecurityException(String explanation) {
        super(explanation);
    }

    public NamingSecurityException() {
        super();
    }
}
