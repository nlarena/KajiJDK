package javax.management;

import java.security.BasicPermission;

/**
 * El permiso de <b>quien firmo el codigo</b>, no el de quien lo llama.
 *
 * <p>Es la pieza rara del modelo: los otros permisos de JMX se consultan contra la pila de
 * llamadas, este se consulta contra el origen del <b>MBean que se esta registrando</b>. La pregunta
 * que responde no es "puede este hilo registrar" sino "confiamos en el codigo de esta clase como
 * para dejarla entrar al agente". De ahi el unico nombre util, `register`.
 *
 * <p>El argumento `actions` de la segunda forma tiene que ser `null` o vacio. Existe solo porque el
 * cargador de politicas construye todos los permisos con dos argumentos y no puede saber cuales los
 * usan.
 */
public class MBeanTrustPermission extends BasicPermission {

    private static final long serialVersionUID = -2952178077029017036L;

    /** @throws IllegalArgumentException si el nombre no es `register` ni `*` */
    public MBeanTrustPermission(String name) {
        this(name, null);
    }

    /**
     * @param actions tiene que ser `null` o la cadena vacia
     * @throws IllegalArgumentException si el nombre no sirve o si vienen acciones
     */
    public MBeanTrustPermission(String name, String actions) {
        super(name, actions);
        if (actions != null && actions.length() > 0) {
            throw new IllegalArgumentException("MBeanTrustPermission no lleva acciones: " + actions);
        }
        if (!name.equals("register") && !name.equals("*")) {
            throw new IllegalArgumentException("Nombre invalido: " + name);
        }
    }
}
