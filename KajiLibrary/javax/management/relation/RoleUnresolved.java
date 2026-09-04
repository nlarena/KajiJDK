package javax.management.relation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.management.ObjectName;

/**
 * Un rol que no se pudo leer o escribir, con el motivo.
 *
 * <h2>Por que existe este tipo</h2>
 *
 * <p>Porque una operacion sobre varios roles no es todo o nada. Leer cinco roles puede dar tres
 * valores y dos problemas, y las dos cosas son informacion util: cortar en el primer problema
 * perderia los tres que si estaban.
 *
 * <p>Este objeto es la mitad "problema" de esa respuesta; la otra son los {@link Role} que si se
 * resolvieron. Las dos viajan juntas en un {@link RoleResult}.
 *
 * <p>El valor <strong>se conserva</strong> aunque haya fallado: en una escritura rechazada, es lo
 * que permite ver que se intentaba poner y no solo que no se pudo.
 */
public class RoleUnresolved implements Serializable {

    private static final long serialVersionUID = -48350262537070138L;

    private String roleName;
    private List<ObjectName> roleValue;
    private int problemType;

    /**
     * @param pbType uno de los codigos de {@link RoleStatus}
     * @throws IllegalArgumentException si falta el nombre o el codigo no es de {@link RoleStatus}
     */
    public RoleUnresolved(String name, List<ObjectName> value, int pbType)
            throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("falta el nombre del rol");
        }
        setProblemType(pbType);
        this.roleName = name;
        this.roleValue = value == null ? null : new ArrayList<ObjectName>(value);
    }

    /** El nombre del rol. */
    public String getRoleName() {
        return this.roleName;
    }

    /** Lo que se intentaba poner, o lo que habia; {@code null} si no aplica. */
    public List<ObjectName> getRoleValue() {
        return this.roleValue;
    }

    /** El codigo de {@link RoleStatus} que explica el problema. */
    public int getProblemType() {
        return this.problemType;
    }

    /**
     * @throws IllegalArgumentException si es {@code null}
     */
    public void setRoleName(String name) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("el nombre no puede ser null");
        }
        this.roleName = name;
    }

    /** Fija el valor; {@code null} lo saca. */
    public void setRoleValue(List<ObjectName> value) {
        this.roleValue = value == null ? null : new ArrayList<ObjectName>(value);
    }

    /**
     * @throws IllegalArgumentException si no es un codigo de {@link RoleStatus} — aceptar cualquier
     *     entero dejaria pasar un problema que despues nadie puede interpretar
     */
    public void setProblemType(int pbType) throws IllegalArgumentException {
        if (!RoleStatus.isRoleStatus(pbType)) {
            throw new IllegalArgumentException(
                    "no es un codigo de RoleStatus: " + String.valueOf(pbType));
        }
        this.problemType = pbType;
    }

    /** Una copia, con su lista propia. */
    public Object clone() {
        try {
            return new RoleUnresolved(this.roleName, this.roleValue, this.problemType);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("role name: ").append(this.roleName);
        if (this.roleValue != null) {
            sb.append("; value: ").append(Role.roleValueToString(this.roleValue));
        }
        sb.append("; problem type: ").append(String.valueOf(this.problemType));
        return sb.toString();
    }
}
