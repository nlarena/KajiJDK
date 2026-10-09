import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Finding #560: un `default` --y un valor de uso-- que NOMBRA UNA CONSTANTE.
 *
 * <p>Fallaba de dos maneras, y la segunda era peor: en un `default` el `AnnotationDefault` no se
 * emitia, y en un uso la anotacion entera se caia en silencio. Despues del primer arreglo a medias, el
 * entero constante de un uso salia con el tag `e` --el de una constante de enum-- donde el javac real
 * pone `I`, que es un `element_value` mal formado.
 *
 * <p>La causa de fondo: `collect_static_final_fields` le daba el `public static final` implicito (§9.3)
 * a una `interface` y no a un `@interface`, asi que las constantes de un tipo anotacion no entraban al
 * mapa de constantes. El `ConstantValue` del campo salia igual --se pliega del init literal directo--,
 * y eso era lo que despistaba.
 *
 * <p>Se corre por reflexion y mide **el uso**, no los `default`: `Method.getDefaultValue()` devuelve
 * `null` siempre en esta biblioteca --el comentario de `Method.java` lo dice: el `AnnotationDefault` no
 * se lee por reflexion-- asi que por ese camino no se distingue un default ausente de uno presente.
 * Los cuatro `default` se verifican con el `javap` del JDK sobre nuestro `.class` y con
 * `codegen::tests::an_annotation_default_naming_a_constant_is_emitted`.
 *
 * <p>Devuelve **-1** si las cuatro comprobaciones coinciden con el JDK 25, o el numero de la primera
 * que no. Con el javac anterior al arreglo da **1**: la anotacion del uso no estaba.
 */
public class Finding560 {

    @Retention(RetentionPolicy.RUNTIME)
    public @interface Marca {
        String K = "kay";
        int N = 7;
        long L = 8L;

        String porLiteral() default "kay";
        String porConstante() default K;
        int entero() default N;
        long largo() default L;
    }

    @Marca(porConstante = Marca.K, entero = Marca.N)
    static class Usa { }

    public static int run() throws Exception {
        // 1: la anotacion del uso tiene que **estar**. Antes del arreglo se caia entera, porque un
        // valor que nombraba una constante hacia que el emisor devolviera "no se pudo codificar".
        Marca u = Usa.class.getAnnotation(Marca.class);
        if (u == null) { return 1; }
        // 2: el valor `String` escrito como constante.
        if (!"kay".equals(u.porConstante())) { return 2; }
        // 3: el `int` escrito como constante. Es el que salia con el tag de una constante de ENUM,
        // o sea un `element_value` mal formado que la reflexion no puede leer como entero.
        if (u.entero() != 7) { return 3; }
        // 4: y los elementos que el uso NO escribe siguen tomando su `default`, que es la otra mitad
        // del #560 --- la que la ficha nombraba.
        if (!"kay".equals(u.porLiteral()) || u.largo() != 8L) { return 4; }
        return -1;
    }

    public static void main(String[] a) throws Exception {
        System.out.println(run());
    }
}
