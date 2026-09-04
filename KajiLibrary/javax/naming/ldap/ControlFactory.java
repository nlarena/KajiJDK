package javax.naming.ldap;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingException;

/**
 * Convierte un {@link Control} crudo —OID y bytes— en el tipo que lo sabe interpretar.
 *
 * <h2>Por que hace falta el paso</h2>
 *
 * <p>Porque el proveedor LDAP recibe del servidor un OID y un arreglo de bytes, y no tiene por que
 * saber que significan: un control puede estar definido por cualquiera. Lo que hace es armar un
 * {@link BasicControl} y preguntarle a las fabricas registradas si alguna lo reconoce.
 *
 * <p>La primera que devuelva algo distinto de {@code null} gana; si ninguna reconoce el control,
 * queda el crudo — que sigue siendo utilizable, solo que sin accesores con sentido.
 *
 * <p>Es el mismo patron que {@link ExtendedRequest#createExtendedResponse} resuelve del otro lado:
 * quien definio la extension es el unico que sabe interpretarla.
 */
public abstract class ControlFactory {

    /** Para las implementaciones. */
    protected ControlFactory() {
    }

    /**
     * Interpreta el control, o devuelve {@code null} si no lo reconoce.
     *
     * <p>Devolver {@code null} es la respuesta normal: una fabrica reconoce uno o dos OIDs y no
     * opina sobre el resto.
     */
    public abstract Control getControlInstance(Control ctl) throws NamingException;

    /**
     * Prueba con todas las fabricas registradas.
     *
     * @param env el entorno, de donde sale {@link LdapContext#CONTROL_FACTORIES}
     * @return el control interpretado, o el mismo que entro si nadie lo reconocio
     */
    public static Control getControlInstance(Control ctl, Context ctx, Hashtable<?, ?> env)
            throws NamingException {
        Object prop = env == null ? null : env.get(LdapContext.CONTROL_FACTORIES);
        if (prop == null) {
            return ctl;
        }
        java.util.StringTokenizer st = new java.util.StringTokenizer(prop.toString(), ":");
        while (st.hasMoreTokens()) {
            String nombre = st.nextToken();
            try {
                Class<?> c = Class.forName(nombre, true, ClassLoader.getSystemClassLoader());
                ControlFactory f = (ControlFactory) c.getDeclaredConstructor().newInstance();
                Control r = f.getControlInstance(ctl);
                if (r != null) {
                    return r;
                }
            } catch (NamingException e) {
                throw e;
            } catch (Exception e) {
                // Una fabrica que no carga no invalida a las que siguen: el orden de la lista es
                // una preferencia, no una dependencia.
                continue;
            }
        }
        return ctl;
    }
}
