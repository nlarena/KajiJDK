package jdk.jfr;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * El tipo de un evento: su nombre, sus campos, sus ajustes y sus anotaciones.
 *
 * <h2>De donde sale</h2>
 *
 * <p>De leer una clase que hereda de {@link Event}: sus campos de instancia son los campos del
 * evento, sus anotaciones son las anotaciones del evento, y los metodos marcados con
 * {@link SettingDefinition} son sus ajustes propios.
 *
 * <p>Es la traduccion de "clase Java" a "tipo de evento", y es lo que se guarda en la cabecera de
 * la grabacion para que quien la lea despues entienda los bytes que siguen.
 *
 * <h2>Los cuatro campos que aparecen sin haberlos declarado</h2>
 *
 * <p>{@link #getFields} arranca siempre con {@code startTime}, {@code duration},
 * {@code eventThread} y {@code stackTrace}, en ese orden, y despues siguen los de la clase. No
 * salen de la clase: los pone JFR, y son los que hacen que dos eventos de tipos distintos se puedan
 * poner en la misma linea de tiempo.
 *
 * <p>{@code stackTrace} es el unico cuyo tipo no es una clase Java: es
 * {@code jdk.types.StackTrace}, un tipo del formato de la grabacion.
 *
 * <h2>Que campos declarados cuentan</h2>
 *
 * <p>Los de <strong>instancia</strong> que no sean {@code static} ni {@code transient} y que sean
 * de un tipo que JFR sepa grabar: los ocho primitivos, {@code String}, {@code Thread} y
 * {@code Class}.
 *
 * <p>Un campo de cualquier otro tipo —un arreglo, una coleccion, un {@code Object}— se
 * <strong>ignora en silencio</strong>. Es lo que hace el JDK y sorprende: la clase compila, el
 * campo existe en Java, y no aparece en la grabacion sin que nadie avise.
 *
 * <h2>Los tres ajustes que todo evento tiene</h2>
 *
 * <p>{@code threshold}, {@code stackTrace} y {@code enabled}, en ese orden, y despues los propios.
 * Sus valores por omision salen de las anotaciones correspondientes si estan.
 *
 * <p><strong>{@code period} no esta</strong>, ni siquiera en un evento anotado con {@link Period}.
 * Es lo que contesta el JDK y es facil de asumir al reves — la anotacion existe, el ajuste no
 * aparece en esta lista.
 *
 * <h2>Estado en esta VM</h2>
 *
 * <p>Casi todo lo de aca es real y sale de la reflexion, y esta comprobado contra el JDK 25: el
 * diferencial da <strong>1 discrepancia sobre 14</strong>.
 *
 * <p>{@link #isEnabled} no puede ser real y contesta {@code false}, que es lo correcto sin
 * grabador.
 *
 * <p>La discrepancia que queda es #505: {@code Field.getAnnotations()} devuelve vacio en esta VM, y
 * por eso un campo declarado pierde su {@link Label}, su {@link Description} y su tipo de
 * contenido. Los cuatro campos implicitos, que esta clase arma a mano, si los tienen. Se arregla
 * del lado de la VM, con el nativo de atributos de campo que {@code Method} ya tiene.
 *
 * @since 9
 */
public final class EventType {

    private static final Cache CACHE = new Cache();

    /**
     * La cache, con nombre en vez de anonima por #482: el generador de bytecode no emite una clase
     * anonima que este en el inicializador de un campo.
     */
    private static final class Cache extends ClassValue<EventType> {
        protected EventType computeValue(final Class<?> type) {
            return new EventType(type);
        }
    }

    private final Class<?> clase;
    private final String nombre;
    private final List<ValueDescriptor> campos;
    private final Map<String, ValueDescriptor> porNombre;
    private final List<AnnotationElement> anotaciones;
    private final List<SettingDescriptor> ajustes;

    private EventType(final Class<?> clase) {
        this.clase = clase;

        final Name n = clase.getAnnotation(Name.class);
        this.nombre = n != null ? n.value() : clase.getName();

        final List<ValueDescriptor> cs = new ArrayList<ValueDescriptor>(implicitos());
        for (final Field f : clase.getDeclaredFields()) {
            final int mod = f.getModifiers();
            if (Modifier.isStatic(mod) || Modifier.isTransient(mod)) {
                continue;
            }
            if (!tipoPermitido(f.getType())) {
                // JFR ignora en silencio los campos de tipo que no sabe grabar. Se reproduce en vez
                // de fallar: la clase compila igual del lado de Java, y un evento con un campo de
                // mas que no se graba es lo que el JDK entrega.
                continue;
            }
            cs.add(new ValueDescriptor(f.getType(), f.getName(),
                    deAnotaciones(f.getAnnotations())));
        }
        final Map<String, ValueDescriptor> mapa = new LinkedHashMap<String, ValueDescriptor>();
        for (final ValueDescriptor v : cs) {
            mapa.put(v.getName(), v);
        }
        this.campos = Collections.unmodifiableList(cs);
        this.porNombre = Collections.unmodifiableMap(mapa);

        this.anotaciones = Collections.unmodifiableList(deAnotaciones(clase.getAnnotations()));
        this.ajustes = Collections.unmodifiableList(armarAjustes(clase));
    }

    private static List<AnnotationElement> deAnotaciones(final Annotation[] as) {
        final List<AnnotationElement> out = new ArrayList<AnnotationElement>();
        for (final Annotation a : as) {
            if (a.annotationType().getName().startsWith("java.lang.annotation.")) {
                continue;
            }
            final Map<String, Object> vals = new LinkedHashMap<String, Object>();
            for (final Method m : a.annotationType().getDeclaredMethods()) {
                try {
                    vals.put(m.getName(), m.invoke(a));
                } catch (final ReflectiveOperationException e) {
                    continue;
                }
            }
            out.add(new AnnotationElement(a.annotationType(), vals));
        }
        return out;
    }

    private static List<SettingDescriptor> armarAjustes(final Class<?> clase) {
        final List<SettingDescriptor> out = new ArrayList<SettingDescriptor>();

        // El orden es el del JDK y no el alfabetico ni el de las anotaciones: threshold, stackTrace,
        // enabled. Comprobado contra el JDK 25.
        final Threshold th = clase.getAnnotation(Threshold.class);
        out.add(new SettingDescriptor(Threshold.NAME, "jdk.settings.Threshold", "Threshold",
                "Record event with duration above or equal to threshold",
                th == null ? "0 ns" : th.value(),
                Collections.<AnnotationElement>emptyList()));

        final StackTrace st = clase.getAnnotation(StackTrace.class);
        out.add(new SettingDescriptor(StackTrace.NAME, "jdk.settings.StackTrace", "Stack Trace",
                "Record stack traces", String.valueOf(st == null || st.value()),
                Collections.<AnnotationElement>emptyList()));

        final Enabled en = clase.getAnnotation(Enabled.class);
        out.add(new SettingDescriptor(Enabled.NAME, "jdk.settings.Enabled", "Enabled",
                "Record event", String.valueOf(en == null || en.value()),
                Collections.<AnnotationElement>emptyList()));

        // `period` NO esta, ni siquiera en un evento con @Period. Es lo que contesta el JDK, y
        // sorprende: la anotacion existe y el ajuste no aparece en la lista del tipo.
        for (final Method m : clase.getDeclaredMethods()) {
            if (m.getAnnotation(SettingDefinition.class) == null) {
                continue;
            }
            final Name nm = m.getAnnotation(Name.class);
            final Label lb = m.getAnnotation(Label.class);
            final Description ds = m.getAnnotation(Description.class);
            final Class<?> control = m.getParameterTypes().length == 1
                    ? m.getParameterTypes()[0] : SettingControl.class;
            out.add(new SettingDescriptor(nm != null ? nm.value() : m.getName(),
                    control.getName(), lb == null ? null : lb.value(),
                    ds == null ? null : ds.value(), valorInicial(control),
                    deAnotaciones(m.getAnnotations())));
        }
        return out;
    }

    /**
     * El valor por omision de un ajuste propio sale de <strong>instanciar su control</strong> y
     * preguntarle.
     *
     * <p>Es lo que hace el JDK, y es la unica forma: el valor por omision no esta declarado en
     * ningun lado, lo decide el control en su constructor.
     */
    private static String valorInicial(final Class<?> control) {
        try {
            final Object o = control.getDeclaredConstructor().newInstance();
            return o instanceof SettingControl ? ((SettingControl) o).getValue() : null;
        } catch (final ReflectiveOperationException e) {
            // Un control sin constructor accesible no invalida al tipo de evento entero: el ajuste
            // queda sin valor por omision, que es distinto de no existir.
            return null;
        }
    }

    /**
     * Los cuatro campos que todo evento tiene, esten o no declarados en la clase.
     *
     * <p>Van primero y en este orden. No salen de la clase: los pone JFR, y son los que hacen que
     * dos eventos de tipos distintos se puedan poner en la misma linea de tiempo.
     */
    private static List<ValueDescriptor> implicitos() {
        final List<ValueDescriptor> out = new ArrayList<ValueDescriptor>(4);
        out.add(new ValueDescriptor(long.class, "startTime",
                Arrays.asList(new AnnotationElement(Label.class, "Start Time"),
                        new AnnotationElement(Timestamp.class, Timestamp.TICKS))));
        out.add(new ValueDescriptor(long.class, "duration",
                Arrays.asList(new AnnotationElement(Label.class, "Duration"),
                        new AnnotationElement(Timespan.class, Timespan.TICKS))));
        out.add(new ValueDescriptor(Thread.class, "eventThread",
                Arrays.asList(new AnnotationElement(Label.class, "Event Thread"),
                        new AnnotationElement(Description.class,
                                "Thread in which event was committed in"))));
        // El unico cuyo tipo no es una clase Java: es un tipo del formato de la grabacion.
        out.add(new ValueDescriptor("jdk.types.StackTrace", "stackTrace",
                Arrays.asList(new AnnotationElement(Label.class, "Stack Trace"),
                        new AnnotationElement(Description.class,
                                "Stack Trace starting from the method the event was committed in"))));
        return out;
    }

    /**
     * Los tipos que JFR sabe grabar en un campo de evento.
     *
     * <p>Los ocho primitivos mas {@code String}, {@code Thread} y {@code Class}. Nada mas: ni
     * arreglos, ni colecciones, ni {@code Object}. Un campo de otro tipo se ignora en silencio.
     */
    private static boolean tipoPermitido(final Class<?> t) {
        return t.isPrimitive() && t != void.class
                || t == String.class || t == Thread.class || t == Class.class;
    }

    /**
     * El tipo de evento de esa clase.
     *
     * <p>Siempre la misma instancia para la misma clase: la lectura por reflexion se hace una vez.
     *
     * @param eventClass la clase del evento
     * @return su tipo
     * @throws NullPointerException si es {@code null}
     */
    public static EventType getEventType(final Class<? extends Event> eventClass) {
        return CACHE.get(Objects.requireNonNull(eventClass, "eventClass"));
    }

    /**
     * Los campos del evento, en el orden en que la clase los declara.
     *
     * @return los campos
     */
    public List<ValueDescriptor> getFields() {
        return campos;
    }

    /**
     * El campo con ese nombre.
     *
     * @param name el nombre
     * @return el campo, o {@code null} si no existe
     */
    public ValueDescriptor getField(final String name) {
        return porNombre.get(Objects.requireNonNull(name, "name"));
    }

    /**
     * El nombre del evento en la grabacion.
     *
     * @return el nombre
     */
    public String getName() {
        return nombre;
    }

    /**
     * El nombre legible del evento.
     *
     * @return la etiqueta, o {@code null} si no tiene
     */
    public String getLabel() {
        final Label l = clase.getAnnotation(Label.class);
        return l == null ? null : l.value();
    }

    /**
     * El identificador numerico del tipo.
     *
     * @return el identificador
     */
    public long getId() {
        return Tipos.id(clase);
    }

    /**
     * Las anotaciones del evento.
     *
     * @return las anotaciones
     */
    public List<AnnotationElement> getAnnotationElements() {
        return anotaciones;
    }

    /**
     * Si alguien esta grabando este tipo de evento ahora mismo.
     *
     * @return {@code false} en esta biblioteca, porque no hay grabador
     */
    public boolean isEnabled() {
        return false;
    }

    /**
     * La explicacion del evento.
     *
     * @return la descripcion, o {@code null} si no tiene
     */
    public String getDescription() {
        final Description d = clase.getAnnotation(Description.class);
        return d == null ? null : d.value();
    }

    /**
     * La anotacion de ese tipo que lleva el evento, si la lleva.
     *
     * @param <A> el tipo de la anotacion
     * @param annotationType el tipo de la anotacion
     * @return la anotacion, o {@code null}
     */
    public <A extends Annotation> A getAnnotation(final Class<A> annotationType) {
        return clase.getAnnotation(Objects.requireNonNull(annotationType, "annotationType"));
    }

    /**
     * Los ajustes del evento: los cuatro de siempre mas los propios.
     *
     * @return los ajustes
     */
    public List<SettingDescriptor> getSettingDescriptors() {
        return ajustes;
    }

    /**
     * Las categorias del evento, de su {@link Category}.
     *
     * <p>Sin la anotacion devuelve {@code ["Uncategorized"]}, que es lo que el JDK usa para que un
     * evento sin categoria caiga en algun lado del arbol en vez de no aparecer.
     *
     * @return las categorias, de la mas general a la mas especifica
     */
    public List<String> getCategoryNames() {
        final Category c = clase.getAnnotation(Category.class);
        if (c == null || c.value().length == 0) {
            return Collections.singletonList("Uncategorized");
        }
        return Collections.unmodifiableList(Arrays.asList(c.value()));
    }
}
