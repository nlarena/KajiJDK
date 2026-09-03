import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.management.MBeanOperationInfo;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.InvalidKeyException;
import javax.management.openmbean.KeyAlreadyExistsException;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanConstructorInfoSupport;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenMBeanOperationInfoSupport;
import javax.management.openmbean.OpenMBeanParameterInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularData;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;

/**
 * `javax.management.openmbean`: los tipos abiertos y los datos que los cumplen.
 *
 * <p>Este archivo compila y da -1 con el JDK 25 corriendo **sus** clases. Eso lo vuelve un oraculo:
 * los numeros que se esperan no los invente yo, los dicta el JDK, y una diferencia entre las dos
 * corridas es una diferencia de comportamiento nuestra.
 *
 * <p>Lo que se comprueba no es que los metodos existan sino lo que hacen: que un tipo compuesto
 * ignore el orden de sus items pero una tabla respete el de sus indices, que la clave de una fila
 * salga de la fila, que `put` de una clave repetida tire en vez de pisar, que las restricciones de
 * un parametro se validen de verdad, y que `equals` compare por forma y no por clase.
 */
public class OpenMBeanTest {

    static int failures = 0;

    static void ok(String what, boolean cond) {
        if (!cond) {
            System.out.println("FALLA " + what);
            failures = failures + 1;
        }
    }

    // El tipo de fila que usan casi todas las comprobaciones: un nombre y una edad.
    static CompositeType personType() throws OpenDataException {
        return new CompositeType("Persona", "una persona",
                new String[] { "nombre", "edad" },
                new String[] { "el nombre", "la edad" },
                new OpenType<?>[] { SimpleType.STRING, SimpleType.INTEGER });
    }

    static CompositeData person(CompositeType t, String nombre, int edad)
            throws OpenDataException {
        return new CompositeDataSupport(t, new String[] { "nombre", "edad" },
                new Object[] { nombre, Integer.valueOf(edad) });
    }

    public static int run() throws Exception {
        failures = 0;

        // ---- SimpleType: quince constantes y nada mas
        ok("STRING acepta un String", SimpleType.STRING.isValue("hola"));
        ok("STRING no acepta un Integer", !SimpleType.STRING.isValue(Integer.valueOf(1)));
        ok("ningun tipo acepta null", !SimpleType.STRING.isValue(null));
        ok("VOID no acepta nada", !SimpleType.VOID.isValue(null));
        ok("el className es el de la clase", "java.lang.Integer".equals(
                SimpleType.INTEGER.getClassName()));
        ok("los tres nombres coinciden",
                SimpleType.INTEGER.getClassName().equals(SimpleType.INTEGER.getTypeName()));
        ok("no es un arreglo", !SimpleType.INTEGER.isArray());

        // ---- CompositeType: el orden de los items NO es parte del tipo
        CompositeType a = personType();
        CompositeType b = new CompositeType("Persona", "una persona",
                new String[] { "edad", "nombre" },
                new String[] { "la edad", "el nombre" },
                new OpenType<?>[] { SimpleType.INTEGER, SimpleType.STRING });
        ok("dos tipos con los items en otro orden son iguales", a.equals(b));
        ok("y comparten hashCode", a.hashCode() == b.hashCode());
        List<String> claves = new ArrayList<String>();
        for (String k : a.keySet()) {
            claves.add(k);
        }
        ok("keySet viene ordenado por nombre",
                claves.equals(Arrays.asList("edad", "nombre")));
        ok("containsKey de uno que esta", a.containsKey("nombre"));
        ok("containsKey de uno que no esta", !a.containsKey("apellido"));
        ok("containsKey de null no rompe", !a.containsKey(null));
        ok("getType del item", SimpleType.INTEGER.equals(a.getType("edad")));
        ok("getType de uno que no esta da null", a.getType("apellido") == null);

        boolean repetido = false;
        try {
            new CompositeType("T", "t", new String[] { "x", "x" },
                    new String[] { "uno", "otro" },
                    new OpenType<?>[] { SimpleType.STRING, SimpleType.STRING });
        } catch (OpenDataException e) {
            repetido = true;
        }
        ok("un item repetido es OpenDataException", repetido);

        boolean vacio = false;
        try {
            new CompositeType("T", "t", new String[0], new String[0], new OpenType<?>[0]);
        } catch (IllegalArgumentException e) {
            vacio = true;
        }
        ok("un tipo compuesto sin items es IllegalArgument", vacio);

        // ---- CompositeDataSupport: valida los valores contra sus tipos
        CompositeData p = person(a, "Ana", 30);
        ok("get devuelve el valor", "Ana".equals(p.get("nombre")));
        ok("getCompositeType es el que se paso", a.equals(p.getCompositeType()));
        ok("containsKey", p.containsKey("edad"));
        ok("containsValue", p.containsValue(Integer.valueOf(30)));
        // Se copia con un bucle y no con `new ArrayList<Object>(p.values())`: nuestro javac no
        // resuelve ese constructor cuando el argumento es un `Collection<?>` con comodin.
        List<Object> valores = new ArrayList<Object>();
        for (Object v : p.values()) {
            valores.add(v);
        }
        ok("values viene en el orden de los nombres",
                valores.equals(Arrays.asList(new Object[] { Integer.valueOf(30), "Ana" })));
        ok("getAll respeta el orden pedido",
                Arrays.equals(p.getAll(new String[] { "nombre", "edad" }),
                        new Object[] { "Ana", Integer.valueOf(30) }));

        boolean claveMala = false;
        try {
            p.get("apellido");
        } catch (InvalidKeyException e) {
            claveMala = true;
        }
        ok("get de un item inexistente es InvalidKeyException", claveMala);

        boolean tipoMalo = false;
        try {
            new CompositeDataSupport(a, new String[] { "nombre", "edad" },
                    new Object[] { "Ana", "treinta" });
        } catch (OpenDataException e) {
            tipoMalo = true;
        }
        ok("un valor del tipo equivocado es OpenDataException", tipoMalo);

        boolean falta = false;
        try {
            new CompositeDataSupport(a, new String[] { "nombre" }, new Object[] { "Ana" });
        } catch (OpenDataException e) {
            falta = true;
        }
        ok("un item que falta es OpenDataException", falta);

        // Un nulo SI se acepta: es "sin valor", que no es lo mismo que "no esta".
        CompositeData conNulo = new CompositeDataSupport(a,
                new String[] { "nombre", "edad" }, new Object[] { null, Integer.valueOf(1) });
        ok("un item puede valer null", conNulo.get("nombre") == null);
        ok("y containsKey sigue diciendo que si", conNulo.containsKey("nombre"));

        // Igualdad por forma, con el mapa y con los arreglos.
        Map<String, Object> m = new HashMap<String, Object>();
        m.put("nombre", "Ana");
        m.put("edad", Integer.valueOf(30));
        CompositeData p2 = new CompositeDataSupport(a, m);
        ok("los dos constructores dan lo mismo", p.equals(p2));
        ok("y el mismo hashCode", p.hashCode() == p2.hashCode());
        ok("distinto valor, distinto dato", !p.equals(person(a, "Beto", 30)));

        // ---- ArrayType
        ArrayType<Integer[]> refs = ArrayType.getArrayType(SimpleType.INTEGER);
        ok("un arreglo de referencias no es primitivo", !refs.isPrimitiveArray());
        ok("su className lleva L y ;",
                "[Ljava.lang.Integer;".equals(refs.getClassName()));
        ok("una dimension", refs.getDimension() == 1);
        ok("es un arreglo", refs.isArray());
        ok("acepta un Integer[]", refs.isValue(new Integer[] { Integer.valueOf(1) }));
        ok("y no un int[]", !refs.isValue(new int[] { 1 }));

        ArrayType<int[]> prims = new ArrayType<int[]>(SimpleType.INTEGER, true);
        ok("un arreglo primitivo lo dice", prims.isPrimitiveArray());
        ok("su className es [I", "[I".equals(prims.getClassName()));
        ok("acepta un int[]", prims.isValue(new int[] { 1 }));
        ok("y no un Integer[]", !prims.isValue(new Integer[] { Integer.valueOf(1) }));
        ok("los dos declaran el mismo tipo de elemento",
                SimpleType.INTEGER.equals(prims.getElementOpenType())
                        && SimpleType.INTEGER.equals(refs.getElementOpenType()));
        ok("pero no son iguales", !prims.equals(refs));

        // Las dimensiones se suman en vez de anidarse.
        ArrayType<?> dos = new ArrayType<Object>(1, refs);
        ok("un arreglo de un arreglo suma dimensiones", dos.getDimension() == 2);
        ok("y su className tiene dos corchetes",
                "[[Ljava.lang.Integer;".equals(dos.getClassName()));

        boolean sinPrimitivo = false;
        try {
            new ArrayType<String[]>(SimpleType.STRING, true);
        } catch (OpenDataException e) {
            sinPrimitivo = true;
        }
        ok("String no tiene forma primitiva", sinPrimitivo);

        // ---- TabularType: el orden de los indices SI es parte del tipo
        TabularType t1 = new TabularType("Gente", "por nombre", a, new String[] { "nombre" });
        TabularType t2 = new TabularType("Gente", "por nombre", a, new String[] { "nombre" });
        ok("dos tipos tabulares iguales", t1.equals(t2));
        ok("y el mismo hashCode", t1.hashCode() == t2.hashCode());
        TabularType porDos = new TabularType("Gente2", "por los dos", a,
                new String[] { "nombre", "edad" });
        TabularType alReves = new TabularType("Gente2", "por los dos", a,
                new String[] { "edad", "nombre" });
        ok("el orden de los indices distingue dos tipos", !porDos.equals(alReves));
        ok("getIndexNames conserva el orden",
                porDos.getIndexNames().equals(Arrays.asList("nombre", "edad")));

        boolean indiceMalo = false;
        try {
            new TabularType("T", "t", a, new String[] { "apellido" });
        } catch (OpenDataException e) {
            indiceMalo = true;
        }
        ok("un indice que no es item del tipo de fila es OpenDataException", indiceMalo);

        // ---- TabularDataSupport
        TabularDataSupport tabla = new TabularDataSupport(t1);
        ok("empieza vacia", tabla.isEmpty() && tabla.size() == 0);
        CompositeData ana = person(a, "Ana", 30);
        tabla.put(ana);
        ok("despues de put tiene una fila", tabla.size() == 1);
        ok("la clave sale de la fila",
                Arrays.equals(tabla.calculateIndex(ana), new Object[] { "Ana" }));
        ok("se recupera por su clave", ana.equals(tabla.get(new Object[] { "Ana" })));
        ok("containsKey de la que esta", tabla.containsKey(new Object[] { "Ana" }));
        ok("containsKey de una que no", !tabla.containsKey(new Object[] { "Zoe" }));
        ok("get de una que no esta da null", tabla.get(new Object[] { "Zoe" }) == null);
        ok("containsValue", tabla.containsValue(ana));

        boolean repetida = false;
        try {
            tabla.put(person(a, "Ana", 99));
        } catch (KeyAlreadyExistsException e) {
            repetida = true;
        }
        ok("put de una clave repetida tira, no pisa", repetida);
        ok("y la fila original quedo", Integer.valueOf(30).equals(
                tabla.get(new Object[] { "Ana" }).get("edad")));

        // El reparto entre las dos excepciones lo dicta el JDK y es asimetrico: vacia es NPE,
        // largo equivocado pero no vacia es InvalidKey. Esta comprobacion esperaba InvalidKey para
        // el arreglo vacio y el oraculo la corrigio, que es justo para lo que sirve tenerlo.
        boolean claveVacia = false;
        try {
            tabla.get(new Object[0]);
        } catch (NullPointerException e) {
            claveVacia = true;
        }
        ok("una clave vacia es NullPointerException", claveVacia);

        boolean claveLarga = false;
        try {
            tabla.get(new Object[] { "Ana", Integer.valueOf(30) });
        } catch (InvalidKeyException e) {
            claveLarga = true;
        }
        ok("una clave con demasiados valores es InvalidKeyException", claveLarga);

        boolean claveDeOtroTipo = false;
        try {
            tabla.get(new Object[] { Integer.valueOf(1) });
        } catch (InvalidKeyException e) {
            claveDeOtroTipo = true;
        }
        ok("una clave del tipo equivocado es InvalidKeyException", claveDeOtroTipo);

        ok("containsKey de una clave vacia no tira, da false",
                !tabla.containsKey(new Object[0]));

        CompositeData beto = person(a, "Beto", 40);
        tabla.put(beto);
        ok("dos filas", tabla.size() == 2);
        ok("remove devuelve la que saco",
                beto.equals(tabla.remove(new Object[] { "Beto" })));
        ok("y queda una", tabla.size() == 1);
        ok("remove de una que no estaba da null",
                tabla.remove(new Object[] { "Zoe" }) == null);

        // `putAll` es todo o nada: la segunda repite la clave de la primera.
        TabularDataSupport t3 = new TabularDataSupport(t1);
        t3.put(person(a, "Ana", 30));
        boolean atomico = false;
        try {
            t3.putAll(new CompositeData[] { person(a, "Beto", 40), person(a, "Ana", 50) });
        } catch (KeyAlreadyExistsException e) {
            atomico = true;
        }
        ok("putAll con una clave repetida tira", atomico);
        ok("y no dejo nada a medio poner", t3.size() == 1);

        // El proxy de `Map`: `put(clave, valor)` ignora la clave.
        TabularDataSupport t4 = new TabularDataSupport(t1);
        t4.put(new Object[] { "cualquiera" }, person(a, "Ana", 30));
        ok("put del Map usa la clave de la fila, no la que se le paso",
                t4.get(new Object[] { "Ana" }) != null);
        ok("y no guarda la que se le paso",
                t4.get(new Object[] { "cualquiera" }) == null);

        // La clave es un arreglo, y dos arreglos distintos con el mismo contenido tienen que
        // encontrar la misma fila. Sin normalizar la clave, esto falla.
        ok("dos arreglos con el mismo contenido encuentran la misma fila",
                t4.get(new Object[] { "Ana" }) == t4.get(new Object[] { "Ana" }));

        TabularDataSupport copia = (TabularDataSupport) t4.clone();
        ok("clone copia las filas", copia.size() == t4.size());
        copia.clear();
        ok("y es independiente", t4.size() == 1 && copia.size() == 0);

        // ---- Los Info abiertos y sus restricciones
        OpenMBeanParameterInfoSupport sinNada = new OpenMBeanParameterInfoSupport(
                "p", "un parametro", SimpleType.INTEGER);
        ok("sin restricciones no hay omision", !sinNada.hasDefaultValue());
        ok("ni valores legales", !sinNada.hasLegalValues());
        ok("ni minimo ni maximo", !sinNada.hasMinValue() && !sinNada.hasMaxValue());
        ok("isValue mira el tipo", sinNada.isValue(Integer.valueOf(5)));
        ok("y rechaza otro tipo", !sinNada.isValue("cinco"));

        OpenMBeanParameterInfoSupport conRango = new OpenMBeanParameterInfoSupport(
                "p", "un parametro", SimpleType.INTEGER, Integer.valueOf(5),
                Integer.valueOf(1), Integer.valueOf(10));
        ok("el rango se declara", conRango.hasMinValue() && conRango.hasMaxValue());
        ok("dentro del rango vale", conRango.isValue(Integer.valueOf(7)));
        ok("el minimo vale", conRango.isValue(Integer.valueOf(1)));
        ok("el maximo vale", conRango.isValue(Integer.valueOf(10)));
        ok("por debajo no", !conRango.isValue(Integer.valueOf(0)));
        ok("por encima tampoco", !conRango.isValue(Integer.valueOf(11)));

        OpenMBeanParameterInfoSupport conLista = new OpenMBeanParameterInfoSupport(
                "p", "un parametro", SimpleType.STRING, "rojo",
                new String[] { "rojo", "verde" });
        ok("los valores legales se declaran", conLista.hasLegalValues());
        ok("uno de la lista vale", conLista.isValue("verde"));
        ok("uno de afuera no", !conLista.isValue("azul"));
        Set<?> legales = conLista.getLegalValues();
        ok("getLegalValues tiene los dos", legales.size() == 2 && legales.contains("rojo"));

        boolean lasDos = false;
        try {
            new OpenMBeanParameterInfoSupport("p", "d", SimpleType.INTEGER, Integer.valueOf(5),
                    new Integer[] { Integer.valueOf(5) });
            // La forma de arriba es legal; la que no lo es, es lista MAS rango, y no hay
            // constructor que las tome juntas. Se comprueba por el otro camino: un rango invertido.
            new OpenMBeanParameterInfoSupport("p", "d", SimpleType.INTEGER, null,
                    Integer.valueOf(10), Integer.valueOf(1));
        } catch (OpenDataException e) {
            lasDos = true;
        }
        ok("un minimo mayor que el maximo es OpenDataException", lasDos);

        boolean fueraDeRango = false;
        try {
            new OpenMBeanParameterInfoSupport("p", "d", SimpleType.INTEGER, Integer.valueOf(99),
                    Integer.valueOf(1), Integer.valueOf(10));
        } catch (OpenDataException e) {
            fueraDeRango = true;
        }
        ok("una omision fuera del rango es OpenDataException", fueraDeRango);

        OpenMBeanAttributeInfoSupport attr = new OpenMBeanAttributeInfoSupport(
                "edad", "la edad", SimpleType.INTEGER, true, false, false);
        ok("el atributo se lee", attr.isReadable());
        ok("y no se escribe", !attr.isWritable());
        ok("y no es un is", !attr.isIs());
        ok("su type es el className del tipo abierto",
                "java.lang.Integer".equals(attr.getType()));

        OpenMBeanAttributeInfoSupport attr2 = new OpenMBeanAttributeInfoSupport(
                "edad", "otra descripcion", SimpleType.INTEGER, true, false, false);
        ok("la descripcion no entra en la igualdad del atributo", attr.equals(attr2));

        OpenMBeanOperationInfoSupport op = new OpenMBeanOperationInfoSupport(
                "sumar", "suma dos", new OpenMBeanParameterInfoSupport[] { sinNada },
                SimpleType.INTEGER, MBeanOperationInfo.INFO);
        ok("la operacion declara su retorno abierto",
                SimpleType.INTEGER.equals(op.getReturnOpenType()));
        ok("y el className correspondiente",
                "java.lang.Integer".equals(op.getReturnType()));
        ok("y su firma", op.getSignature().length == 1);

        boolean impactoMalo = false;
        try {
            new OpenMBeanOperationInfoSupport("x", "d",
                    new OpenMBeanParameterInfoSupport[0], SimpleType.VOID, 99);
        } catch (IllegalArgumentException e) {
            impactoMalo = true;
        }
        ok("un impacto desconocido es IllegalArgument", impactoMalo);

        OpenMBeanConstructorInfoSupport ctor = new OpenMBeanConstructorInfoSupport(
                "C", "un constructor", new OpenMBeanParameterInfoSupport[] { sinNada });
        ok("el constructor guarda su firma", ctor.getSignature().length == 1);

        OpenMBeanInfoSupport info = new OpenMBeanInfoSupport("C", "un mbean",
                new OpenMBeanAttributeInfoSupport[] { attr },
                new OpenMBeanConstructorInfoSupport[] { ctor },
                new OpenMBeanOperationInfoSupport[] { op },
                null);
        ok("el info guarda todo", info.getAttributes().length == 1
                && info.getConstructors().length == 1
                && info.getOperations().length == 1
                && info.getNotifications().length == 0);
        ok("el className", "C".equals(info.getClassName()));

        // ---- quien admite restricciones y quien no
        //
        // El reparto sorprende y por eso esta comprobado: un `CompositeType` SI admite valor por
        // omision, y un `ArrayType` o un `TabularType` NO. Esta prueba afirmaba lo contrario para
        // el compuesto y el oraculo la corrigio.
        OpenMBeanParameterInfoSupport compuestoConOmision =
                new OpenMBeanParameterInfoSupport("p", "d", a, p);
        ok("un CompositeType SI admite valor por omision",
                compuestoConOmision.hasDefaultValue());

        boolean arregloRestringido = false;
        try {
            new OpenMBeanParameterInfoSupport("p", "d", refs,
                    new Integer[] { Integer.valueOf(1) });
        } catch (OpenDataException e) {
            arregloRestringido = true;
        }
        ok("un ArrayType no admite valor por omision", arregloRestringido);

        boolean tablaRestringida = false;
        try {
            new OpenMBeanParameterInfoSupport("p", "d", t1, tabla);
        } catch (OpenDataException e) {
            tablaRestringida = true;
        }
        ok("un TabularType no admite valor por omision", tablaRestringida);

        // ---- isValue de los tipos compuestos y tabulares
        ok("el CompositeType reconoce su dato", a.isValue(p));
        ok("y no reconoce otra cosa", !a.isValue("hola"));
        ok("el TabularType reconoce su tabla", t1.isValue(tabla));
        ok("y no reconoce un dato compuesto", !t1.isValue(p));

        if (failures == 0) {
            return -1;
        }
        return failures;
    }

    public static void main(String[] args) throws Exception {
        System.out.println("OpenMBeanTest " + OpenMBeanTest.run());
    }
}
