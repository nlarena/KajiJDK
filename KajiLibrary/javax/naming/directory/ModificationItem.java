package javax.naming.directory;

import java.io.Serializable;

/**
 * KajiLibrary's javax.naming.directory.ModificationItem -- una modificacion sola, para aplicar en
 * lote.
 *
 * <p>Un par: que hacer --agregar, reemplazar o quitar-- y sobre que atributo. Existe para la version
 * de {@code modifyAttributes} que recibe un arreglo, y esa version es la que hay que usar cuando las
 * modificaciones tienen que aplicarse <b>juntas</b>: la especificacion pide que se apliquen todas o
 * ninguna.
 *
 * <p>La otra version --la que recibe un solo codigo y unos {@link Attributes}-- aplica la misma
 * operacion a todos, asi que no sirve para un lote que mezcla agregados y borrados.
 *
 * <p>Es inmutable: los dos campos se fijan al construir. Tiene sentido para algo que participa de una
 * operacion atomica -- si se pudiera cambiar despues de armar el arreglo, lo que se aplica no seria
 * lo que se reviso.
 */
public class ModificationItem implements Serializable {

    private static final long serialVersionUID = 7573258562534746850L;

    /** Una de las tres constantes de {@link DirContext}. */
    private final int mod_op;

    /** Sobre que atributo. */
    private final Attribute attr;

    /**
     * @param mod_op {@link DirContext#ADD_ATTRIBUTE}, {@link DirContext#REPLACE_ATTRIBUTE} o
     *     {@link DirContext#REMOVE_ATTRIBUTE}
     * @param attr el atributo; para quitar, sus valores dicen <b>cuales</b> quitar
     * @throws IllegalArgumentException si el codigo no es uno de los tres, o si el atributo es null
     */
    public ModificationItem(int mod_op, Attribute attr) {
        if (attr == null) {
            throw new IllegalArgumentException("Must specify non-null attribute for modification");
        }
        if (mod_op != DirContext.ADD_ATTRIBUTE
                && mod_op != DirContext.REPLACE_ATTRIBUTE
                && mod_op != DirContext.REMOVE_ATTRIBUTE) {
            throw new IllegalArgumentException("Invalid modification code " + mod_op);
        }
        this.mod_op = mod_op;
        this.attr = attr;
    }

    /** Que hacer. */
    public int getModificationOp() {
        return this.mod_op;
    }

    /** Sobre que atributo. */
    public Attribute getAttribute() {
        return this.attr;
    }

    /** La operacion en palabras y el atributo, para un registro. */
    public String toString() {
        switch (this.mod_op) {
            case DirContext.ADD_ATTRIBUTE:
                return "Add attribute: " + this.attr.toString();
            case DirContext.REPLACE_ATTRIBUTE:
                return "Replace attribute: " + this.attr.toString();
            default:
                return "Remove attribute: " + this.attr.toString();
        }
    }
}
