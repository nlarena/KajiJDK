package com.sun.tools.attach;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import com.sun.tools.attach.spi.AttachProvider;

/**
 * Una VM en marcha, vista desde otro proceso.
 *
 * <h2>Que es "adjuntarse"</h2>
 *
 * <p>Es conseguir un canal con una VM que ya esta corriendo y que no se inicio para eso. Lo que
 * habilita es cargar un <strong>agente</strong> adentro suyo — codigo que corre con sus permisos,
 * ve sus clases y puede instrumentarlas. Es como funcionan los perfiladores y los depuradores que se
 * enganchan a un proceso vivo, y es tambien la razon de que haya un {@link AttachPermission}: quien
 * puede adjuntarse puede ejecutar cualquier cosa dentro del destino.
 *
 * <h2>Los tres cargadores, y en que se diferencian</h2>
 *
 * <ul>
 * <li>{@link #loadAgent} — un agente Java: un JAR con {@code Agent-Class} en el manifiesto;</li>
 * <li>{@link #loadAgentLibrary} — una biblioteca nativa, buscada por nombre en la ruta del sistema;</li>
 * <li>{@link #loadAgentPath} — una biblioteca nativa, por ruta absoluta.</li>
 * </ul>
 *
 * <p>Los dos ultimos se diferencian solo en como se encuentra el archivo, y son dos porque quien
 * carga por nombre quiere que el sistema resuelva la convencion de la plataforma
 * ({@code lib*.so}, {@code *.dll}) y quien carga por ruta ya sabe exactamente cual quiere.
 *
 * <h2>Como se resuelve todo esto</h2>
 *
 * <p>Nada de esto se implementa aca: los tres metodos estaticos delegan en los
 * {@link AttachProvider} instalados, que son quienes conocen el mecanismo del sistema operativo.
 * <strong>Sin proveedores instalados</strong> —el caso de esta VM— {@link #list} devuelve una lista
 * vacia y {@link #attach} tira {@link AttachNotSupportedException}. Es el comportamiento correcto y
 * el mismo que da un JDK al que le sacaron los proveedores: el mecanismo esta entero, lo que falta
 * es alguien que se registre en el.
 */
public abstract class VirtualMachine {

    private final AttachProvider provider;
    private final String id;

    /**
     * Para las implementaciones de un proveedor.
     *
     * @throws NullPointerException si el proveedor o el identificador son {@code null}
     */
    protected VirtualMachine(AttachProvider provider, String id) {
        if (provider == null) {
            throw new NullPointerException("provider");
        }
        if (id == null) {
            throw new NullPointerException("id");
        }
        this.provider = provider;
        this.id = id;
    }

    /**
     * Las VMs que ve cada proveedor instalado, juntas.
     *
     * <p>Una foto: entre listarlas y adjuntarse, una VM puede haber terminado.
     */
    public static List<VirtualMachineDescriptor> list() {
        List<VirtualMachineDescriptor> todas = new ArrayList<VirtualMachineDescriptor>();
        List<AttachProvider> proveedores = AttachProvider.providers();
        for (int i = 0; i < proveedores.size(); i++) {
            todas.addAll(proveedores.get(i).listVirtualMachines());
        }
        return todas;
    }

    /**
     * Se adjunta a la VM identificada por {@code id}, probando cada proveedor hasta que uno pueda.
     *
     * <p>Probar en orden y no elegir es lo correcto: un identificador solo significa algo dentro de
     * un proveedor, asi que no hay forma de saber de antemano cual lo entiende. Que uno diga
     * {@link AttachNotSupportedException} no es un error — es su manera de decir "este no es mio".
     *
     * @throws AttachNotSupportedException si ningun proveedor lo reconoce, o si no hay ninguno
     *     instalado
     * @throws NullPointerException si {@code id} es {@code null}
     */
    public static VirtualMachine attach(String id)
            throws AttachNotSupportedException, IOException {
        if (id == null) {
            throw new NullPointerException("id");
        }
        List<AttachProvider> proveedores = AttachProvider.providers();
        if (proveedores.isEmpty()) {
            throw new AttachNotSupportedException("no hay ningun proveedor instalado");
        }
        AttachNotSupportedException ultima = null;
        for (int i = 0; i < proveedores.size(); i++) {
            try {
                return proveedores.get(i).attachVirtualMachine(id);
            } catch (AttachNotSupportedException e) {
                // Se guarda la ultima y se sigue: que este proveedor no lo reconozca no dice nada
                // sobre los que faltan.
                ultima = e;
            }
        }
        throw ultima;
    }

    /**
     * Se adjunta a la VM que describe {@code vmd}, con el proveedor que la vio.
     *
     * <p>Aca no se prueba con todos, y no es una inconsistencia con {@link #attach(String)}: un
     * descriptor <em>ya dice</em> de que proveedor salio, asi que no hay nada que adivinar.
     */
    public static VirtualMachine attach(VirtualMachineDescriptor vmd)
            throws AttachNotSupportedException, IOException {
        if (vmd == null) {
            throw new NullPointerException("vmd");
        }
        return vmd.provider().attachVirtualMachine(vmd);
    }

    /**
     * Suelta la VM destino.
     *
     * <p>Lo que el agente ya cargo sigue adentro: soltar cierra el canal, no deshace lo hecho.
     */
    public abstract void detach() throws IOException;

    /** El proveedor que consiguio este canal. */
    public final AttachProvider provider() {
        return this.provider;
    }

    /** Como nombra su proveedor a esta VM. */
    public final String id() {
        return this.id;
    }

    /** Carga una biblioteca de agente nativa, por nombre, con opciones. */
    public abstract void loadAgentLibrary(String agentLibrary, String options)
            throws AgentLoadException, AgentInitializationException, IOException;

    /** Igual, sin opciones. */
    public void loadAgentLibrary(String agentLibrary)
            throws AgentLoadException, AgentInitializationException, IOException {
        loadAgentLibrary(agentLibrary, null);
    }

    /** Carga una biblioteca de agente nativa, por ruta absoluta, con opciones. */
    public abstract void loadAgentPath(String agentPath, String options)
            throws AgentLoadException, AgentInitializationException, IOException;

    /** Igual, sin opciones. */
    public void loadAgentPath(String agentPath)
            throws AgentLoadException, AgentInitializationException, IOException {
        loadAgentPath(agentPath, null);
    }

    /** Carga un agente Java: un JAR con {@code Agent-Class} en su manifiesto. */
    public abstract void loadAgent(String agent, String options)
            throws AgentLoadException, AgentInitializationException, IOException;

    /** Igual, sin opciones. */
    public void loadAgent(String agent)
            throws AgentLoadException, AgentInitializationException, IOException {
        loadAgent(agent, null);
    }

    /**
     * Las propiedades del sistema de la VM destino.
     *
     * <p>Son las suyas, no las de este proceso: es la forma barata de averiguar con que version de
     * Java corre, en que directorio, y con que classpath.
     */
    public abstract Properties getSystemProperties() throws IOException;

    /** Las propiedades que dejaron los agentes ya cargados en la VM destino. */
    public abstract Properties getAgentProperties() throws IOException;

    /** Arranca el agente de administracion de la VM destino con esa configuracion. */
    public abstract void startManagementAgent(Properties agentProperties) throws IOException;

    /**
     * Arranca el agente de administracion local y devuelve su direccion JMX.
     *
     * <p>Local quiere decir que solo se puede conectar algo de la misma maquina. Es lo que permite
     * a una herramienta como un monitor conectarse a un proceso que arranco sin ninguna opcion de
     * administracion.
     */
    public abstract String startLocalManagementAgent() throws IOException;

    /** Sobre el proveedor y el identificador, igual que {@link VirtualMachineDescriptor}. */
    public int hashCode() {
        return this.provider.hashCode() * 127 + this.id.hashCode();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof VirtualMachine) {
            VirtualMachine otra = (VirtualMachine) obj;
            return otra.provider() == this.provider && otra.id().equals(this.id);
        }
        return false;
    }

    public String toString() {
        return this.provider.toString() + ": " + this.id;
    }
}
