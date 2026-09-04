package jdk.dynalink.linker.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardingDynamicLinker;
import jdk.dynalink.linker.LinkRequest;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.TypeBasedGuardingDynamicLinker;

/**
 * Compone enlazadores que deciden por el tipo del receptor, con una cache por clase.
 *
 * <h2>Que gana sobre {@link CompositeGuardingDynamicLinker}</h2>
 *
 * <p>Que no recorre la lista. La primera vez que aparece un receptor de cierta clase pregunta a
 * todos cual la acepta y se guarda la respuesta; a partir de ahi va derecho a los que sirven. En
 * una cadena de veinte enlazadores donde uno solo entiende {@code String}, esto es la diferencia
 * entre veinte preguntas por enlace y una.
 *
 * <p>La cache es un {@link ClassValue}, no un mapa: queda colgada de la propia clase y desaparece
 * cuando la clase se descarga. Con un mapa comun, las clases cargadas dinamicamente —que en un
 * lenguaje de scripting son muchas— no se podrian liberar nunca.
 *
 * @since 9
 */
public class CompositeTypeBasedGuardingDynamicLinker implements TypeBasedGuardingDynamicLinker {

    private final ClaseAEnlazadores claseAEnlazadores;

    /** La cache: para cada clase, cuales de los enlazadores la aceptan. */
    private static final class ClaseAEnlazadores
            extends ClassValue<List<TypeBasedGuardingDynamicLinker>> {

        private static final List<TypeBasedGuardingDynamicLinker> NINGUNO =
                Collections.emptyList();

        private final TypeBasedGuardingDynamicLinker[] enlazadores;
        /** Listas de un solo elemento, precalculadas: el caso de lejos mas frecuente. */
        private final List<List<TypeBasedGuardingDynamicLinker>> solos;

        ClaseAEnlazadores(final TypeBasedGuardingDynamicLinker[] enlazadores) {
            this.enlazadores = enlazadores;
            final List<List<TypeBasedGuardingDynamicLinker>> s =
                    new ArrayList<List<TypeBasedGuardingDynamicLinker>>(enlazadores.length);
            for (int i = 0; i < enlazadores.length; i++) {
                s.add(Collections.singletonList(enlazadores[i]));
            }
            this.solos = s;
        }

        protected List<TypeBasedGuardingDynamicLinker> computeValue(final Class<?> clazz) {
            List<TypeBasedGuardingDynamicLinker> lista = NINGUNO;
            for (int i = 0; i < enlazadores.length; i++) {
                if (!enlazadores[i].canLinkType(clazz)) {
                    continue;
                }
                if (lista == NINGUNO) {
                    lista = solos.get(i);
                } else {
                    if (lista.size() == 1) {
                        // Recien aca deja de servir la lista precalculada, que es inmutable.
                        lista = new ArrayList<TypeBasedGuardingDynamicLinker>(lista);
                    }
                    lista.add(enlazadores[i]);
                }
            }
            return lista;
        }
    }

    /**
     * Compone los enlazadores en el orden en que vienen.
     *
     * @param linkers los enlazadores
     */
    public CompositeTypeBasedGuardingDynamicLinker(
            final Iterable<? extends TypeBasedGuardingDynamicLinker> linkers) {
        final List<TypeBasedGuardingDynamicLinker> l =
                new ArrayList<TypeBasedGuardingDynamicLinker>();
        for (final TypeBasedGuardingDynamicLinker linker : linkers) {
            l.add(Objects.requireNonNull(linker));
        }
        this.claseAEnlazadores = new ClaseAEnlazadores(
                l.toArray(new TypeBasedGuardingDynamicLinker[l.size()]));
    }

    /** Si alguno de los compuestos acepta esa clase. */
    public boolean canLinkType(final Class<?> type) {
        return !claseAEnlazadores.get(type).isEmpty();
    }

    /**
     * Lo que conteste el primero de los enlazadores que aceptan la clase del receptor.
     *
     * <p>Sin receptor no hay clase por la cual decidir, y entonces devuelve {@code null} sin
     * preguntarle a nadie: un enlazador basado en tipos no tiene nada que decir de una invocacion
     * sin argumentos.
     */
    public GuardedInvocation getGuardedInvocation(final LinkRequest linkRequest,
            final LinkerServices linkerServices) throws Exception {
        final Object receptor = linkRequest.getReceiver();
        if (receptor == null) {
            return null;
        }
        for (final TypeBasedGuardingDynamicLinker linker
                : claseAEnlazadores.get(receptor.getClass())) {
            final GuardedInvocation invocation =
                    linker.getGuardedInvocation(linkRequest, linkerServices);
            if (invocation != null) {
                return invocation;
            }
        }
        return null;
    }

    /**
     * Agrupa los tramos consecutivos de enlazadores basados en tipo, dejando el resto igual.
     *
     * <p>El orden importa y por eso solo agrupa <strong>consecutivos</strong>: si entre dos
     * enlazadores por tipo hay uno comun, juntar los dos primeros los adelantaria por encima de
     * el, y la cadena dejaria de significar lo que su autor escribio.
     *
     * @param linkers los enlazadores, en orden
     * @return la lista optimizada, del mismo largo o mas corta
     */
    public static List<GuardingDynamicLinker> optimize(
            final Iterable<? extends GuardingDynamicLinker> linkers) {
        final List<GuardingDynamicLinker> salida = new ArrayList<GuardingDynamicLinker>();
        final List<TypeBasedGuardingDynamicLinker> tramo =
                new ArrayList<TypeBasedGuardingDynamicLinker>();
        for (final GuardingDynamicLinker linker : linkers) {
            Objects.requireNonNull(linker);
            if (linker instanceof TypeBasedGuardingDynamicLinker) {
                tramo.add((TypeBasedGuardingDynamicLinker) linker);
            } else {
                cerrarTramo(salida, tramo);
                salida.add(linker);
            }
        }
        cerrarTramo(salida, tramo);
        return salida;
    }

    private static void cerrarTramo(final List<GuardingDynamicLinker> salida,
            final List<TypeBasedGuardingDynamicLinker> tramo) {
        if (tramo.isEmpty()) {
            return;
        }
        if (tramo.size() == 1) {
            // Uno solo no se envuelve: la cache no ahorraria nada y agregaria una indireccion.
            salida.addAll(tramo);
        } else {
            salida.add(new CompositeTypeBasedGuardingDynamicLinker(tramo));
        }
        tramo.clear();
    }
}
