import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.IndexedPropertyDescriptor;
import java.beans.Introspector;
import java.beans.PropertyChangeListener;
import java.beans.PropertyDescriptor;
import java.beans.VetoableChangeListener;

// Prueba diferencial de java.beans.Introspector.
//
// La gracia es que este MISMO archivo corre en las dos VMs: en la nuestra usa nuestro java.beans,
// y en la JVM real usa el java.beans del JDK (el classpath no puede aportar clases java.*). Si las
// dos devuelven -1, nuestras reglas de introspeccion son las del JDK, no una interpretacion.
//
// Cada regla dudosa esta afirmada aparte y prende su bit, asi un fallo dice CUAL regla se rompio.

class BeanDePrueba {
    private String nombre;
    private boolean activo;
    private Boolean envuelto;
    private int[] datos = new int[] { 7, 8, 9 };
    private int soloLectura = 5;

    public String getNombre() { return nombre; }
    public void setNombre(String n) { nombre = n; }

    public boolean isActivo() { return activo; }
    public void setActivo(boolean a) { activo = a; }

    // `is` con envoltorio Boolean: NO es getter.
    public Boolean isEnvuelto() { return envuelto; }
    public void setEnvuelto(Boolean b) { envuelto = b; }

    // arreglo + indexada
    public int[] getDatos() { return datos; }
    public void setDatos(int[] d) { datos = d; }
    public int getDatos(int i) { return datos[i]; }
    public void setDatos(int i, int v) { datos[i] = v; }

    public int getSoloLectura() { return soloLectura; }
    public void setSoloEscritura(String s) { }

    // sigla
    public String getURL() { return null; }
    public void setURL(String u) { }

    // dos mayusculas al principio
    public int getXCoord() { return 1; }

    // getter que devuelve void: no es propiedad
    public void getNada() { }
    // setter que devuelve algo: no es setter
    public String setRaro(String s) { return s; }
    // get con parametro no-int: no es propiedad
    public String getPorClave(String k) { return k; }
    // tipos que no cierran: gana el getter
    public String getDesparejo() { return null; }
    public void setDesparejo(int x) { }
    // solo indexada, sin accesores de arreglo
    public String getSoloIdx(int i) { return null; }
    public void setSoloIdx(int i, String v) { }
    // estatico: no cuenta
    public static String getEstatico() { return null; }

    public void addPropertyChangeListener(PropertyChangeListener l) { }
    public void removePropertyChangeListener(PropertyChangeListener l) { }
    public void addVetoableChangeListener(VetoableChangeListener l) { }
    public void removeVetoableChangeListener(VetoableChangeListener l) { }
}

public class KajiBeansIntrospeccionTest {

    static PropertyDescriptor buscar(PropertyDescriptor[] pds, String nombre) {
        PropertyDescriptor r = null;
        for (int i = 0; i < pds.length; i++) {
            if (r == null && pds[i].getName().equals(nombre)) { r = pds[i]; }
        }
        return r;
    }

    static String nom(java.lang.reflect.Method m) {
        return m == null ? "null" : m.getName();
    }

    static String tipo(Class<?> c) {
        return c == null ? "null" : c.getName();
    }

    public static int run() throws Exception {
        int fallas = 0;

        BeanInfo bi = Introspector.getBeanInfo(BeanDePrueba.class, Object.class);
        PropertyDescriptor[] pds = bi.getPropertyDescriptors();

        // --- 1: el conjunto exacto de propiedades ---------------------------------
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < pds.length; i++) { sb.append(pds[i].getName()).append(","); }
        String esperado = "URL,XCoord,activo,datos,desparejo,envuelto,nombre,soloEscritura,soloIdx,soloLectura,";
        if (!esperado.equals(sb.toString())) {
            System.out.println("F1 propiedades/orden: " + sb + " != " + esperado);
            fallas |= 1;
        }

        // --- 2: getURL -> "URL", no "uRL" -----------------------------------------
        if (!"URL".equals(Introspector.decapitalize("URL"))
            || !"name".equals(Introspector.decapitalize("Name"))
            || !"XCoord".equals(Introspector.decapitalize("XCoord"))
            || !"x".equals(Introspector.decapitalize("X"))
            || !"".equals(Introspector.decapitalize(""))
            || !"aB".equals(Introspector.decapitalize("aB"))
            || !"ABc".equals(Introspector.decapitalize("ABc"))) {
            System.out.println("F2 decapitalize");
            fallas |= 2;
        }

        // --- 4: boolean con `is`; Boolean con `is` NO ------------------------------
        PropertyDescriptor act = buscar(pds, "activo");
        if (act == null || !"isActivo".equals(nom(act.getReadMethod()))
            || !"setActivo".equals(nom(act.getWriteMethod()))
            || act.getPropertyType() != boolean.class) {
            System.out.println("F4a activo");
            fallas |= 4;
        }
        PropertyDescriptor env = buscar(pds, "envuelto");
        if (env == null || env.getReadMethod() != null
            || !"setEnvuelto".equals(nom(env.getWriteMethod()))) {
            System.out.println("F4b envuelto read=" + (env == null ? "?" : nom(env.getReadMethod())));
            fallas |= 4;
        }

        // --- 8: setter con tipo que no cierra se descarta, gana el getter ----------
        PropertyDescriptor des = buscar(pds, "desparejo");
        if (des == null || !"getDesparejo".equals(nom(des.getReadMethod()))
            || des.getWriteMethod() != null
            || des.getPropertyType() != String.class) {
            System.out.println("F8 desparejo write=" + (des == null ? "?" : nom(des.getWriteMethod())));
            fallas |= 8;
        }

        // --- 16: indexada con arreglo ---------------------------------------------
        PropertyDescriptor dat = buscar(pds, "datos");
        if (!(dat instanceof IndexedPropertyDescriptor)) {
            System.out.println("F16a datos no es indexada");
            fallas |= 16;
        } else {
            IndexedPropertyDescriptor ip = (IndexedPropertyDescriptor) dat;
            if (!"[I".equals(tipo(ip.getPropertyType()))
                || ip.getIndexedPropertyType() != int.class
                || !"getDatos".equals(nom(ip.getReadMethod()))
                || !"setDatos".equals(nom(ip.getWriteMethod()))
                || !"getDatos".equals(nom(ip.getIndexedReadMethod()))
                || !"setDatos".equals(nom(ip.getIndexedWriteMethod()))) {
                System.out.println("F16b datos tipo=" + tipo(ip.getPropertyType())
                    + " idx=" + tipo(ip.getIndexedPropertyType()));
                fallas |= 16;
            }
        }

        // --- 32: propiedad SOLO indexada -> propertyType null ----------------------
        PropertyDescriptor si = buscar(pds, "soloIdx");
        if (!(si instanceof IndexedPropertyDescriptor)) {
            System.out.println("F32a soloIdx no es indexada");
            fallas |= 32;
        } else {
            IndexedPropertyDescriptor ip = (IndexedPropertyDescriptor) si;
            if (ip.getPropertyType() != null || ip.getReadMethod() != null || ip.getWriteMethod() != null
                || ip.getIndexedPropertyType() != String.class
                || !"getSoloIdx".equals(nom(ip.getIndexedReadMethod()))
                || !"setSoloIdx".equals(nom(ip.getIndexedWriteMethod()))) {
                System.out.println("F32b soloIdx tipo=" + tipo(ip.getPropertyType())
                    + " read=" + nom(ip.getReadMethod()));
                fallas |= 32;
            }
        }

        // --- 64: solo lectura / solo escritura ------------------------------------
        PropertyDescriptor sl = buscar(pds, "soloLectura");
        PropertyDescriptor se = buscar(pds, "soloEscritura");
        if (sl == null || sl.getWriteMethod() != null || sl.getPropertyType() != int.class
            || se == null || se.getReadMethod() != null || se.getPropertyType() != String.class) {
            System.out.println("F64 solo lectura/escritura");
            fallas |= 64;
        }

        // --- 128: bound=true por addPropertyChangeListener; constrained=false ------
        PropertyDescriptor nb = buscar(pds, "nombre");
        if (nb == null || !nb.isBound() || nb.isConstrained()) {
            System.out.println("F128 bound=" + (nb == null ? "?" : "" + nb.isBound())
                + " constrained=" + (nb == null ? "?" : "" + nb.isConstrained()));
            fallas |= 128;
        }

        // --- 256: conjuntos de eventos --------------------------------------------
        EventSetDescriptor[] es = bi.getEventSetDescriptors();
        StringBuilder eb = new StringBuilder();
        for (int i = 0; i < es.length; i++) { eb.append(es[i].getName()).append(","); }
        String ev = eb.toString();
        boolean tienePC = ev.indexOf("propertyChange,") >= 0;
        boolean tieneVC = ev.indexOf("vetoableChange,") >= 0;
        if (!tienePC || !tieneVC || es.length != 2) {
            System.out.println("F256 eventos: " + ev);
            fallas |= 256;
        }

        // --- 512: la propiedad "class" aparece sin stopClass -----------------------
        BeanInfo bi2 = Introspector.getBeanInfo(BeanDePrueba.class);
        PropertyDescriptor[] p2 = bi2.getPropertyDescriptors();
        PropertyDescriptor cls = buscar(p2, "class");
        if (cls == null || !"getClass".equals(nom(cls.getReadMethod()))
            || cls.getPropertyType() != Class.class) {
            System.out.println("F512 propiedad class");
            fallas |= 512;
        }

        // --- 1024: constructor PropertyDescriptor(String, Class) -------------------
        PropertyDescriptor manual = new PropertyDescriptor("nombre", BeanDePrueba.class);
        if (!"getNombre".equals(nom(manual.getReadMethod()))
            || !"setNombre".equals(nom(manual.getWriteMethod()))
            || !manual.isBound()) {
            System.out.println("F1024 manual read=" + nom(manual.getReadMethod())
                + " bound=" + manual.isBound());
            fallas |= 1024;
        }
        boolean tiro = false;
        try { new PropertyDescriptor("noExiste", BeanDePrueba.class); }
        catch (java.beans.IntrospectionException e) { tiro = true; }
        if (!tiro) {
            System.out.println("F1024b no tiro por propiedad inexistente");
            fallas |= 1024;
        }

        // --- 2048: defaults de FeatureDescriptor ----------------------------------
        PropertyDescriptor fd = buscar(pds, "nombre");
        if (!"nombre".equals(fd.getDisplayName()) || !"nombre".equals(fd.getShortDescription())) {
            System.out.println("F2048 displayName=" + fd.getDisplayName());
            fallas |= 2048;
        }

        // --- 4096: indices por defecto --------------------------------------------
        if (bi.getDefaultPropertyIndex() != -1 || bi.getDefaultEventIndex() != -1) {
            System.out.println("F4096 indices por defecto");
            fallas |= 4096;
        }

        return fallas == 0 ? -1 : fallas;
    }

    public static void main(String[] a) throws Exception { System.out.println(run()); }
}
