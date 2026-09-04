package jdk.dynalink.linker;

/**
 * Un enlazador que puede decidir por el tipo del receptor, antes de mirar nada mas.
 *
 * <h2>Para que sirve separar esa pregunta</h2>
 *
 * <p>Para no recorrer la cadena entera en cada enlace. Una cadena de enlazadores comunes se
 * prueba en orden hasta que uno no devuelva {@code null}; si todos son de este tipo, quien los
 * compone puede en cambio preguntar {@link #canLinkType} y armar una <strong>cache por clase</strong>
 * — la segunda vez que aparece un receptor de esa clase ya sabe a quien mandarlo.
 *
 * <p>Eso es exactamente lo que hace {@code CompositeTypeBasedGuardingDynamicLinker}, y es la
 * unica razon de que esta interfaz exista aparte de {@link GuardingDynamicLinker}.
 *
 * <h2>La respuesta es un permiso, no una promesa</h2>
 *
 * <p>Contestar {@code true} no obliga a enlazar: {@link #getGuardedInvocation} todavia puede
 * devolver {@code null} si la operacion concreta no le sirve. Al reves si es un compromiso —
 * contestar {@code false} hace que a este enlazador ni se le pregunte.
 *
 * @since 9
 */
public interface TypeBasedGuardingDynamicLinker extends GuardingDynamicLinker {

    /**
     * Si este enlazador quiere que se le ofrezcan los receptores de esa clase.
     *
     * @param type la clase del receptor
     * @return {@code true} si le interesa
     */
    boolean canLinkType(Class<?> type);
}
