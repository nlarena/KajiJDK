package javax.management.relation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * La implementacion de {@link RelationType} que trae el JDK: los roles se declaran y se guardan.
 *
 * <h2>Las dos formas de usarla</h2>
 *
 * <p>Directamente, pasandole los roles al constructor; o extendiendola y llamando a
 * {@link #addRoleInfo} desde el constructor de la subclase — que es el motivo de que ese metodo sea
 * {@code protected} y de que exista el constructor de un solo argumento.
 *
 * <p>La segunda forma sirve para un tipo cuyos roles dependan de algo que se calcula.
 *
 * <h2>Por que se congela al registrarla</h2>
 *
 * <p>Una vez que el servicio de relaciones acepto el tipo, agregarle roles lo volveria inconsistente
 * con las relaciones que ya se crearon contra el: tendrian un rol menos del que su tipo declara, sin
 * que nadie las haya tocado. Por eso {@link #addRoleInfo} falla despues del registro.
 */
public class RelationTypeSupport implements RelationType {

    private static final long serialVersionUID = 4611072955724144607L;

    private final String typeName;
    private final Map<String, RoleInfo> roleName2InfoMap = new TreeMap<String, RoleInfo>();
    private boolean isInRelationService = false;

    /**
     * Con sus roles.
     *
     * @throws IllegalArgumentException si falta el nombre o los roles
     * @throws InvalidRelationTypeException si dos roles se llaman igual, o si alguno es {@code null}
     */
    public RelationTypeSupport(String relationTypeName, RoleInfo[] roleInfoArray)
            throws IllegalArgumentException, InvalidRelationTypeException {
        if (relationTypeName == null) {
            throw new IllegalArgumentException("falta el nombre del tipo");
        }
        checkRoleInfos(roleInfoArray);
        this.typeName = relationTypeName;
        for (int i = 0; i < roleInfoArray.length; i++) {
            this.roleName2InfoMap.put(roleInfoArray[i].getName(), new RoleInfo(roleInfoArray[i]));
        }
    }

    /**
     * Para las subclases, que agregan los roles con {@link #addRoleInfo}.
     *
     * @throws IllegalArgumentException si falta el nombre
     */
    protected RelationTypeSupport(String relationTypeName) {
        if (relationTypeName == null) {
            throw new IllegalArgumentException("falta el nombre del tipo");
        }
        this.typeName = relationTypeName;
    }

    /** El nombre del tipo. */
    public String getRelationTypeName() {
        return this.typeName;
    }

    /** Los roles que declara. */
    public List<RoleInfo> getRoleInfos() {
        return new ArrayList<RoleInfo>(this.roleName2InfoMap.values());
    }

    /**
     * La descripcion de ese rol.
     *
     * @throws RoleInfoNotFoundException si no lo declara
     */
    public RoleInfo getRoleInfo(String roleInfoName)
            throws IllegalArgumentException, RoleInfoNotFoundException {
        if (roleInfoName == null) {
            throw new IllegalArgumentException("falta el nombre del rol");
        }
        RoleInfo info = this.roleName2InfoMap.get(roleInfoName);
        if (info == null) {
            throw new RoleInfoNotFoundException(
                    "el tipo " + this.typeName + " no declara el rol " + roleInfoName);
        }
        return info;
    }

    /**
     * Agrega un rol; solo antes de registrar el tipo.
     *
     * @throws IllegalStateException si el tipo ya esta en el servicio de relaciones — ver la nota de
     *     la clase
     * @throws InvalidRelationTypeException si ya hay un rol con ese nombre
     */
    protected void addRoleInfo(RoleInfo roleInfo)
            throws IllegalArgumentException, InvalidRelationTypeException {
        if (roleInfo == null) {
            throw new IllegalArgumentException("el rol no puede ser null");
        }
        if (this.isInRelationService) {
            throw new IllegalStateException(
                    "el tipo ya esta registrado: no se le pueden agregar roles");
        }
        if (this.roleName2InfoMap.containsKey(roleInfo.getName())) {
            throw new InvalidRelationTypeException(
                    "ya hay un rol llamado " + roleInfo.getName());
        }
        this.roleName2InfoMap.put(roleInfo.getName(), new RoleInfo(roleInfo));
    }

    /** Lo llama el servicio de relaciones al registrar y al sacar el tipo. */
    void setRelationServiceFlag(boolean flag) {
        this.isInRelationService = flag;
    }

    /**
     * Valida un arreglo de roles antes de aceptarlo.
     *
     * @throws InvalidRelationTypeException si esta vacio, si hay un {@code null} o si dos se llaman
     *     igual — dos roles homonimos harian ambiguo todo acceso por nombre
     */
    static void checkRoleInfos(RoleInfo[] roleInfoArray)
            throws IllegalArgumentException, InvalidRelationTypeException {
        if (roleInfoArray == null) {
            throw new IllegalArgumentException("falta el arreglo de roles");
        }
        if (roleInfoArray.length == 0) {
            throw new InvalidRelationTypeException("un tipo de relacion necesita al menos un rol");
        }
        java.util.Set<String> vistos = new java.util.HashSet<String>();
        for (int i = 0; i < roleInfoArray.length; i++) {
            RoleInfo r = roleInfoArray[i];
            if (r == null) {
                throw new InvalidRelationTypeException("hay un rol null en el arreglo");
            }
            if (!vistos.add(r.getName())) {
                throw new InvalidRelationTypeException("hay dos roles llamados " + r.getName());
            }
        }
    }
}
