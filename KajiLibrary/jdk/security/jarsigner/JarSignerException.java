package jdk.security.jarsigner;

/**
 * Lo que lanza {@link JarSigner#sign} cuando la firma no se pudo completar.
 *
 * <p>Es la excepcion envolvente: la causa --una clave que no sirve, un algoritmo que no esta, un
 * error de E/S sobre el zip-- viaja adentro. Que sea sin comprobar es a proposito: `sign` puede
 * fallar por muchas razones distintas y ninguna se maneja distinto de las otras, asi que obligarlas
 * a declararse una por una no le daria informacion a nadie.
 */
public class JarSignerException extends RuntimeException {

    private static final long serialVersionUID = -4732217075689309530L;

    /**
     * Una excepcion con ese detalle y esa causa.
     *
     * @param msg el detalle
     * @param cause lo que fallo de verdad
     */
    public JarSignerException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
