package javax.management.relation;

/**
 * La interfaz de gestion de {@link RelationSupport} cuando se lo registra como MBean.
 *
 * <h2>Por que existe y que agrega</h2>
 *
 * <p>Una relacion puede vivir de dos maneras: administrada internamente por el servicio, o
 * registrada en el servidor de MBeans como un objeto mas. La segunda es la que permite verla y
 * manipularla desde una consola de administracion.
 *
 * <p>Lo que agrega sobre {@link Relation} son los dos metodos que reflejan <em>en cual de las dos
 * formas</em> esta viviendo. Son de uso interno del servicio; el codigo de usuario los lee pero no
 * los escribe.
 */
public interface RelationSupportMBean extends Relation {

    /**
     * Si el servicio de relaciones la esta administrando.
     *
     * <p>{@code false} significa que el objeto existe pero todavia no fue agregado al servicio, y
     * entonces casi ninguna operacion sirve: sin el servicio no hay a quien preguntarle por el tipo
     * ni con que verificar los MBeans referenciados.
     */
    Boolean isInRelationService();

    /** Lo llama el servicio al tomarla y al soltarla. */
    void setRelationServiceManagementFlag(Boolean flag) throws IllegalArgumentException;
}
