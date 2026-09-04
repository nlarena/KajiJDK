package javax.naming.spi;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

/**
 * KajiLibrary's javax.naming.spi.DirStateFactory -- una {@link StateFactory} que ademas produce
 * atributos.
 *
 * <p>La contracara de {@link DirObjectFactory}. Lo que devuelve no es un objeto sino un
 * {@link Result}: el valor a guardar <b>y</b> los atributos con los que hay que guardarlo.
 *
 * <p>Hace falta que sean dos cosas porque en un directorio las dos se escriben juntas y de forma
 * atomica. Devolver solo el objeto obligaria a un segundo {@code modifyAttributes}, y entre las dos
 * llamadas la entrada existiria sin su clase de objeto -- que es justamente lo que el esquema
 * prohibe.
 */
public interface DirStateFactory extends StateFactory {

    /**
     * Lo que hay que guardar y con que atributos.
     *
     * @param inAttrs los que ya se pensaba escribir, o null
     * @return null si esta fabrica no reconoce el objeto
     */
    Result getStateToBind(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment,
                          Attributes inAttrs) throws NamingException;

    /**
     * El par que devuelve {@link DirStateFactory#getStateToBind}.
     *
     * <p>Inmutable y sin logica: existe solo porque Java no tiene tuplas. Cualquiera de los dos
     * campos puede ser null, y eso significa "usa lo que ya tenias".
     */
    public static class Result {

        /** Lo que hay que guardar. */
        private final Object obj;

        /** Con que atributos. */
        private final Attributes attrs;

        /**
         * @param obj el valor a guardar, o null para dejar el original
         * @param outAttrs los atributos, o null para dejar los que habia
         */
        public Result(Object obj, Attributes outAttrs) {
            this.obj = obj;
            this.attrs = outAttrs;
        }

        /** El valor a guardar. */
        public Object getObject() {
            return this.obj;
        }

        /** Los atributos. */
        public Attributes getAttributes() {
            return this.attrs;
        }
    }
}
