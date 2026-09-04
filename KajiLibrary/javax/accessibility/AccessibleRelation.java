package javax.accessibility;

/**
 * Un vínculo entre dos objetos que el árbol de componentes no expresa.
 *
 * <p>La jerarquía dice quién está adentro de quién, y eso no alcanza. Que una etiqueta describa a un
 * campo, que un botón controle a un panel, que un texto siga a otro en el orden de lectura: nada de
 * eso se deduce de estar al lado. Estas relaciones lo dicen explícitamente.
 *
 * <p>La más usada es {@link #LABELED_BY}, y es la que arregla el problema clásico de un formulario:
 * sin ella, una ayuda técnica llega a un campo de texto vacío y no tiene forma de saber que la
 * palabra que está a la izquierda es su nombre.
 *
 * <p>Casi todas vienen de a pares —{@code LABEL_FOR} y {@code LABELED_BY}— porque el vínculo se
 * declara desde los dos lados y quien recorra el árbol puede entrar por cualquiera.
 */
public class AccessibleRelation extends AccessibleBundle {

    /** El nombre de la propiedad <b>childNodeOf</b>. */
    public static final String CHILD_NODE_OF = "childNodeOf";

    /** El nombre de la propiedad <b>childNodeOfProperty</b>. */
    public static final String CHILD_NODE_OF_PROPERTY = "childNodeOfProperty";

    /** El nombre de la propiedad <b>controlledBy</b>. */
    public static final String CONTROLLED_BY = "controlledBy";

    /** El nombre de la propiedad <b>controlledByProperty</b>. */
    public static final String CONTROLLED_BY_PROPERTY = "controlledByProperty";

    /** El nombre de la propiedad <b>controllerFor</b>. */
    public static final String CONTROLLER_FOR = "controllerFor";

    /** El nombre de la propiedad <b>controllerForProperty</b>. */
    public static final String CONTROLLER_FOR_PROPERTY = "controllerForProperty";

    /** El nombre de la propiedad <b>embeddedBy</b>. */
    public static final String EMBEDDED_BY = "embeddedBy";

    /** El nombre de la propiedad <b>embeddedByProperty</b>. */
    public static final String EMBEDDED_BY_PROPERTY = "embeddedByProperty";

    /** El nombre de la propiedad <b>embeds</b>. */
    public static final String EMBEDS = "embeds";

    /** El nombre de la propiedad <b>embedsProperty</b>. */
    public static final String EMBEDS_PROPERTY = "embedsProperty";

    /** El nombre de la propiedad <b>flowsFrom</b>. */
    public static final String FLOWS_FROM = "flowsFrom";

    /** El nombre de la propiedad <b>flowsFromProperty</b>. */
    public static final String FLOWS_FROM_PROPERTY = "flowsFromProperty";

    /** El nombre de la propiedad <b>flowsTo</b>. */
    public static final String FLOWS_TO = "flowsTo";

    /** El nombre de la propiedad <b>flowsToProperty</b>. */
    public static final String FLOWS_TO_PROPERTY = "flowsToProperty";

    /** El nombre de la propiedad <b>labeledBy</b>. */
    public static final String LABELED_BY = "labeledBy";

    /** El nombre de la propiedad <b>labeledByProperty</b>. */
    public static final String LABELED_BY_PROPERTY = "labeledByProperty";

    /** El nombre de la propiedad <b>labelFor</b>. */
    public static final String LABEL_FOR = "labelFor";

    /** El nombre de la propiedad <b>labelForProperty</b>. */
    public static final String LABEL_FOR_PROPERTY = "labelForProperty";

    /** El nombre de la propiedad <b>memberOf</b>. */
    public static final String MEMBER_OF = "memberOf";

    /** El nombre de la propiedad <b>memberOfProperty</b>. */
    public static final String MEMBER_OF_PROPERTY = "memberOfProperty";

    /** El nombre de la propiedad <b>parentWindowOf</b>. */
    public static final String PARENT_WINDOW_OF = "parentWindowOf";

    /** El nombre de la propiedad <b>parentWindowOfProperty</b>. */
    public static final String PARENT_WINDOW_OF_PROPERTY = "parentWindowOfProperty";

    /** El nombre de la propiedad <b>subwindowOf</b>. */
    public static final String SUBWINDOW_OF = "subwindowOf";

    /** El nombre de la propiedad <b>subwindowOfProperty</b>. */
    public static final String SUBWINDOW_OF_PROPERTY = "subwindowOfProperty";

    private Object[] target = new Object[0];

    /** Con la clave y sin destino. */
    public AccessibleRelation(String key) {
        this.key = key;
    }

    /** Con la clave y un destino. */
    public AccessibleRelation(String key, Object target) {
        this.key = key;
        this.target = new Object[1];
        this.target[0] = target;
    }

    /** Con la clave y varios destinos. */
    public AccessibleRelation(String key, Object[] target) {
        this.key = key;
        this.target = target;
    }

    /** Qué relación es. */
    public String getKey() {
        return this.key;
    }

    /** Con qué objetos se relaciona. */
    public Object[] getTarget() {
        Object[] copia;
        if (this.target == null) {
            copia = new Object[0];
        } else {
            copia = new Object[this.target.length];
            System.arraycopy(this.target, 0, copia, 0, this.target.length);
        }
        return copia;
    }

    /** Cambia el destino. */
    public void setTarget(Object target) {
        Object[] uno = new Object[1];
        uno[0] = target;
        this.target = uno;
    }

    /** Cambia los destinos. */
    public void setTarget(Object[] target) {
        if (target == null) {
            this.target = new Object[0];
        } else {
            this.target = target;
        }
    }
}
