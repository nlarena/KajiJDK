package javax.management.relation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.management.ObjectName;

/**
 * Un rol con su valor: el nombre, y los MBeans que hoy ocupan esa punta de la relacion.
 *
 * <p>Es la instancia de lo que {@link RoleInfo} describe. Aquella dice "un dueno, exactamente uno,
 * de clase {@code Persona}"; esta dice "el dueno es <em>este</em> MBean".
 *
 * <p>Es mutable —tiene setters— porque se usa como <strong>argumento</strong>: se arma uno, se lo
 * pasa a {@code setRole} y se lo puede reusar cambiandole el valor. El servicio de relaciones se
 * queda con una copia, asi que modificarlo despues no cambia la relacion.
 */
public class Role implements Serializable {

    private static final long serialVersionUID = -279985518429862552L;

    private String name;
    private List<ObjectName> objectNameList = new ArrayList<ObjectName>();

    /**
     * @throws IllegalArgumentException si falta el nombre o la lista
     */
    public Role(String roleName, List<ObjectName> roleValue) throws IllegalArgumentException {
        if (roleName == null || roleValue == null) {
            throw new IllegalArgumentException("hacen falta el nombre y el valor");
        }
        this.name = roleName;
        this.objectNameList = new ArrayList<ObjectName>(roleValue);
    }

    /** El nombre del rol. */
    public String getRoleName() {
        return this.name;
    }

    /** Los MBeans que lo ocupan. */
    public List<ObjectName> getRoleValue() {
        return this.objectNameList;
    }

    /**
     * @throws IllegalArgumentException si es {@code null}
     */
    public void setRoleName(String roleName) throws IllegalArgumentException {
        if (roleName == null) {
            throw new IllegalArgumentException("el nombre no puede ser null");
        }
        this.name = roleName;
    }

    /**
     * @throws IllegalArgumentException si es {@code null}
     */
    public void setRoleValue(List<ObjectName> roleValue) throws IllegalArgumentException {
        if (roleValue == null) {
            throw new IllegalArgumentException("el valor no puede ser null");
        }
        this.objectNameList = new ArrayList<ObjectName>(roleValue);
    }

    public String toString() {
        return "role name: " + this.name + "; role value: "
                + roleValueToString(this.objectNameList);
    }

    /**
     * Una copia.
     *
     * <p>Copia la lista tambien: una copia que compartiera el valor con el original haria que
     * cambiar uno cambiara el otro, que es lo contrario de lo que un {@code clone} promete.
     */
    public Object clone() {
        try {
            return new Role(this.name, this.objectNameList);
        } catch (IllegalArgumentException e) {
            // Imposible: los dos ya pasaron la validacion al construir este objeto.
            return null;
        }
    }

    /** Los nombres de MBean, uno por linea. Es como se los muestra en un log. */
    public static String roleValueToString(List<ObjectName> roleValue)
            throws IllegalArgumentException {
        if (roleValue == null) {
            throw new IllegalArgumentException("el valor no puede ser null");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < roleValue.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            sb.append(roleValue.get(i).toString());
        }
        return sb.toString();
    }
}
