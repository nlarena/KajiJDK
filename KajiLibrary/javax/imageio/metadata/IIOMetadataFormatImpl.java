package javax.imageio.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.imageio.ImageTypeSpecifier;

/**
 * KajiLibrary's javax.imageio.metadata.IIOMetadataFormatImpl -- la mitad del trabajo de escribir un
 * esquema de metadatos.
 *
 * <p>Implementa los veinticinco {@code getXxx} de {@link IIOMetadataFormat} sobre una tabla interna, y
 * expone metodos <b>protegidos</b> para llenarla. Una subclase declara su esquema en el constructor,
 * con llamadas a {@link #addElement} y {@link #addAttribute}, y hereda todas las consultas.
 *
 * <p>Solo queda abstracto {@link IIOMetadataFormat#canNodeAppear}, que es lo unico que depende del
 * tipo concreto de imagen y que ninguna tabla puede contestar.
 *
 * <h2>Los metodos de construccion son protegidos a proposito</h2>
 *
 * <p>Un esquema se arma una vez, en el constructor, y despues no cambia. Si {@code addElement} fuera
 * publico, cualquiera podria modificar el esquema de un formato ya en uso y dejar arboles validos
 * ayer e invalidos hoy.
 *
 * <h2>Las descripciones vienen de un paquete de recursos</h2>
 *
 * <p>{@link #setResourceBaseName} dice de que paquete sacar los textos de
 * {@code getElementDescription} y {@code getAttributeDescription}, buscados por el nombre del elemento
 * o por {@code elemento/atributo}. Sin paquete, o si la clave no esta, esos metodos devuelven null --
 * que es lo correcto: no hay descripcion, y no una descripcion inventada.
 *
 * <h2>Agregar un hijo es dos llamadas</h2>
 *
 * <p>{@link #addElement} <b>declara</b> el elemento; {@link #addChildElement} lo <b>cuelga</b> de otro.
 * Las dos versiones de {@code addElement} que toman un padre hacen las dos cosas de una, y son las que
 * se usan casi siempre. La separacion existe para poder declarar un elemento que sea hijo de varios.
 */
public abstract class IIOMetadataFormatImpl implements IIOMetadataFormat {

    /** El nombre del formato comun a todos los lectores y escritores. */
    public static final String standardMetadataFormatName = "javax_imageio_1.0";

    /** El formato estandar, armado una sola vez. */
    private static IIOMetadataFormat standardFormat = null;

    /** Como se llama la raiz. */
    private final String rootName;

    /** De donde sacar los textos, o null. */
    private String resourceBaseName = getClass().getName() + "Resources";

    /** Los elementos, por nombre. */
    private final Map<String, Element> elements = new HashMap<String, Element>();

    /**
     * Un esquema con esa raiz y esa politica de hijos.
     *
     * @throws IllegalArgumentException si el nombre es null o la politica no es una de las seis, o si
     *     es {@link #CHILD_POLICY_REPEAT} -- para esa esta el otro constructor
     */
    public IIOMetadataFormatImpl(String rootName, int childPolicy) {
        if (rootName == null) {
            throw new IllegalArgumentException("rootName == null!");
        }
        if (childPolicy < CHILD_POLICY_EMPTY || childPolicy > CHILD_POLICY_MAX
            || childPolicy == CHILD_POLICY_REPEAT) {
            throw new IllegalArgumentException("Invalid value for childPolicy!");
        }
        this.rootName = rootName;
        Element root = new Element(rootName);
        root.childPolicy = childPolicy;
        this.elements.put(rootName, root);
    }

    /**
     * Idem, con la raiz repetible.
     *
     * @throws IllegalArgumentException si el nombre es null o los limites no cierran
     */
    public IIOMetadataFormatImpl(String rootName, int minChildren, int maxChildren) {
        if (rootName == null) {
            throw new IllegalArgumentException("rootName == null!");
        }
        if (minChildren < 0) {
            throw new IllegalArgumentException("minChildren < 0!");
        }
        if (minChildren > maxChildren) {
            throw new IllegalArgumentException("minChildren > maxChildren!");
        }
        this.rootName = rootName;
        Element root = new Element(rootName);
        root.childPolicy = CHILD_POLICY_REPEAT;
        root.minChildren = minChildren;
        root.maxChildren = maxChildren;
        this.elements.put(rootName, root);
    }

    /** De donde sacar los textos; null los apaga. Ver la nota de la clase. */
    protected void setResourceBaseName(String resourceBaseName) {
        this.resourceBaseName = resourceBaseName;
    }

    /** De donde salen. */
    protected String getResourceBaseName() {
        return this.resourceBaseName;
    }

    /**
     * Declara un elemento y lo cuelga de su padre.
     *
     * @throws IllegalArgumentException si el padre no existe, o la politica no sirve
     */
    protected void addElement(String elementName, String parentName, int childPolicy) {
        if (childPolicy < CHILD_POLICY_EMPTY || childPolicy > CHILD_POLICY_MAX
            || childPolicy == CHILD_POLICY_REPEAT) {
            throw new IllegalArgumentException("Invalid value for childPolicy!");
        }
        Element element = getOrCreate(elementName);
        element.childPolicy = childPolicy;
        addChildElement(elementName, parentName);
    }

    /**
     * Idem, repetible.
     *
     * @throws IllegalArgumentException si el padre no existe o los limites no cierran
     */
    protected void addElement(String elementName, String parentName, int minChildren,
                              int maxChildren) {
        if (minChildren < 0) {
            throw new IllegalArgumentException("minChildren < 0!");
        }
        if (minChildren > maxChildren) {
            throw new IllegalArgumentException("minChildren > maxChildren!");
        }
        Element element = getOrCreate(elementName);
        element.childPolicy = CHILD_POLICY_REPEAT;
        element.minChildren = minChildren;
        element.maxChildren = maxChildren;
        addChildElement(elementName, parentName);
    }

    /**
     * Cuelga un elemento ya declarado de otro. Ver la nota de la clase.
     *
     * @throws IllegalArgumentException si el padre no existe
     */
    protected void addChildElement(String elementName, String parentName) {
        Element parent = element(parentName);
        getOrCreate(elementName);
        if (!parent.childList.contains(elementName)) {
            parent.childList.add(elementName);
        }
    }

    /**
     * Saca un elemento del esquema.
     *
     * <p>Lo saca tambien de las listas de hijos de todos los demas: dejarlo colgando produciria un
     * esquema que nombra un elemento que ya no existe.
     */
    protected void removeElement(String elementName) {
        if (this.elements.remove(elementName) != null) {
            java.util.Iterator<Element> it = this.elements.values().iterator();
            while (it.hasNext()) {
                it.next().childList.remove(elementName);
            }
        }
    }

    /**
     * Declara un atributo de valor libre o enumerado.
     *
     * @param dataType uno de los {@code DATATYPE_}
     * @param required si tiene que estar
     * @param defaultValue que vale si no se pone, o null
     * @throws IllegalArgumentException si el elemento no existe o el tipo no sirve
     */
    protected void addAttribute(String elementName, String attrName, int dataType,
                                boolean required, String defaultValue) {
        Element element = element(elementName);
        checkDataType(dataType);
        Attribute attr = new Attribute(attrName);
        attr.valueType = VALUE_ARBITRARY;
        attr.dataType = dataType;
        attr.required = required;
        attr.defaultValue = defaultValue;
        element.attributes.put(attrName, attr);
        element.attrList.add(attrName);
    }

    /**
     * Idem, con una lista cerrada de valores.
     *
     * @throws IllegalArgumentException si la lista es null o vacia
     */
    protected void addAttribute(String elementName, String attrName, int dataType,
                                boolean required, String defaultValue,
                                List<String> enumeratedValues) {
        Element element = element(elementName);
        checkDataType(dataType);
        if (enumeratedValues == null) {
            throw new IllegalArgumentException("enumeratedValues == null!");
        }
        if (enumeratedValues.size() == 0) {
            throw new IllegalArgumentException("enumeratedValues is empty!");
        }
        Attribute attr = new Attribute(attrName);
        attr.valueType = VALUE_ENUMERATION;
        attr.dataType = dataType;
        attr.required = required;
        attr.defaultValue = defaultValue;
        attr.enumeratedValues = new ArrayList<String>(enumeratedValues);
        element.attributes.put(attrName, attr);
        element.attrList.add(attrName);
    }

    /**
     * Idem, con un rango.
     *
     * @param minInclusive si el minimo esta incluido
     */
    protected void addAttribute(String elementName, String attrName, int dataType,
                                boolean required, String defaultValue, String minValue,
                                String maxValue, boolean minInclusive, boolean maxInclusive) {
        Element element = element(elementName);
        checkDataType(dataType);
        Attribute attr = new Attribute(attrName);
        int valueType = VALUE_RANGE;
        if (minInclusive) {
            valueType = valueType | VALUE_RANGE_MIN_INCLUSIVE_MASK;
        }
        if (maxInclusive) {
            valueType = valueType | VALUE_RANGE_MAX_INCLUSIVE_MASK;
        }
        attr.valueType = valueType;
        attr.dataType = dataType;
        attr.required = required;
        attr.defaultValue = defaultValue;
        attr.minValue = minValue;
        attr.maxValue = maxValue;
        element.attributes.put(attrName, attr);
        element.attrList.add(attrName);
    }

    /**
     * Idem, con una lista de valores separados por espacios.
     *
     * @param listMinLength cuantos como minimo
     * @throws IllegalArgumentException si los limites no cierran
     */
    protected void addAttribute(String elementName, String attrName, int dataType,
                                boolean required, int listMinLength, int listMaxLength) {
        Element element = element(elementName);
        checkDataType(dataType);
        if (listMinLength < 0 || listMinLength > listMaxLength) {
            throw new IllegalArgumentException("Invalid list bounds!");
        }
        Attribute attr = new Attribute(attrName);
        attr.valueType = VALUE_LIST;
        attr.dataType = dataType;
        attr.required = required;
        attr.listMinLength = listMinLength;
        attr.listMaxLength = listMaxLength;
        element.attributes.put(attrName, attr);
        element.attrList.add(attrName);
    }

    /** Un atajo para un atributo de {@code true} o {@code false}. */
    protected void addBooleanAttribute(String elementName, String attrName,
                                       boolean hasDefaultValue, boolean defaultValue) {
        List<String> values = new ArrayList<String>();
        values.add("TRUE");
        values.add("FALSE");
        String defaultVal = null;
        if (hasDefaultValue) {
            if (defaultValue) {
                defaultVal = "TRUE";
            } else {
                defaultVal = "FALSE";
            }
        }
        addAttribute(elementName, attrName, DATATYPE_BOOLEAN, true, defaultVal, values);
    }

    /**
     * Saca un atributo.
     *
     * @throws IllegalArgumentException si el elemento no existe
     */
    protected void removeAttribute(String elementName, String attrName) {
        Element element = element(elementName);
        element.attributes.remove(attrName);
        element.attrList.remove(attrName);
    }

    /**
     * Declara que ese elemento lleva un objeto de usuario de esa clase.
     *
     * @param required si tiene que estar
     * @param defaultValue que objeto va si no se pone, o null
     */
    protected <T> void addObjectValue(String elementName, Class<T> classType, boolean required,
                                      T defaultValue) {
        Element element = element(elementName);
        ObjectValue value = new ObjectValue();
        value.valueType = VALUE_ARBITRARY;
        value.classType = classType;
        value.defaultValue = defaultValue;
        element.objectValue = value;
    }

    /**
     * Idem, con una lista cerrada de objetos.
     *
     * @throws IllegalArgumentException si la lista es null o vacia
     */
    protected <T> void addObjectValue(String elementName, Class<T> classType, boolean required,
                                      T defaultValue, List<? extends T> enumeratedValues) {
        Element element = element(elementName);
        if (enumeratedValues == null) {
            throw new IllegalArgumentException("enumeratedValues == null!");
        }
        if (enumeratedValues.size() == 0) {
            throw new IllegalArgumentException("enumeratedValues is empty!");
        }
        ObjectValue value = new ObjectValue();
        value.valueType = VALUE_ENUMERATION;
        value.classType = classType;
        value.defaultValue = defaultValue;
        value.enumeratedValues = new ArrayList<Object>(enumeratedValues);
        element.objectValue = value;
    }

    /** Idem, con un rango. */
    protected <T extends Comparable<? super T>> void addObjectValue(String elementName,
                                                                    Class<T> classType,
                                                                    T defaultValue,
                                                                    Comparable<? super T> minValue,
                                                                    Comparable<? super T> maxValue,
                                                                    boolean minInclusive,
                                                                    boolean maxInclusive) {
        Element element = element(elementName);
        ObjectValue value = new ObjectValue();
        int valueType = VALUE_RANGE;
        if (minInclusive) {
            valueType = valueType | VALUE_RANGE_MIN_INCLUSIVE_MASK;
        }
        if (maxInclusive) {
            valueType = valueType | VALUE_RANGE_MAX_INCLUSIVE_MASK;
        }
        value.valueType = valueType;
        value.classType = classType;
        value.defaultValue = defaultValue;
        value.minValue = minValue;
        value.maxValue = maxValue;
        element.objectValue = value;
    }

    /**
     * Idem, con un arreglo de esa clase.
     *
     * @throws IllegalArgumentException si los limites no cierran
     */
    protected void addObjectValue(String elementName, Class<?> classType, int arrayMinLength,
                                  int arrayMaxLength) {
        Element element = element(elementName);
        if (arrayMinLength < 0 || arrayMinLength > arrayMaxLength) {
            throw new IllegalArgumentException("Invalid array bounds!");
        }
        ObjectValue value = new ObjectValue();
        value.valueType = VALUE_LIST;
        value.classType = classType;
        value.arrayMinLength = arrayMinLength;
        value.arrayMaxLength = arrayMaxLength;
        element.objectValue = value;
    }

    /** Saca el objeto de usuario de ese elemento. */
    protected void removeObjectValue(String elementName) {
        element(elementName).objectValue = null;
    }

    /** Como se llama la raiz. */
    public String getRootName() {
        return this.rootName;
    }

    /** Lo unico que la tabla no puede contestar; ver la nota de la clase. */
    public abstract boolean canNodeAppear(String elementName, ImageTypeSpecifier imageType);

    /**
     * Cuantos hijos como minimo.
     *
     * @throws IllegalArgumentException si la politica no es {@link #CHILD_POLICY_REPEAT}
     */
    public int getElementMinChildren(String elementName) {
        Element element = element(elementName);
        if (element.childPolicy != CHILD_POLICY_REPEAT) {
            throw new IllegalArgumentException("Child policy not CHILD_POLICY_REPEAT!");
        }
        return element.minChildren;
    }

    /**
     * Cuantos como maximo.
     *
     * @throws IllegalArgumentException si la politica no es {@link #CHILD_POLICY_REPEAT}
     */
    public int getElementMaxChildren(String elementName) {
        Element element = element(elementName);
        if (element.childPolicy != CHILD_POLICY_REPEAT) {
            throw new IllegalArgumentException("Child policy not CHILD_POLICY_REPEAT!");
        }
        return element.maxChildren;
    }

    /** Que es ese elemento, o null si no hay texto. Ver la nota de la clase. */
    public String getElementDescription(String elementName, Locale locale) {
        element(elementName);
        return resource(elementName, locale);
    }

    /**
     * Cual de las seis politicas.
     *
     * @throws IllegalArgumentException si el elemento no existe
     */
    public int getChildPolicy(String elementName) {
        return element(elementName).childPolicy;
    }

    /**
     * Que hijos puede tener; null si no puede tener ninguno.
     *
     * @throws IllegalArgumentException si el elemento no existe
     */
    public String[] getChildNames(String elementName) {
        Element element = element(elementName);
        if (element.childPolicy == CHILD_POLICY_EMPTY) {
            return null;
        }
        return element.childList.toArray(new String[element.childList.size()]);
    }

    /**
     * Que atributos puede tener.
     *
     * @throws IllegalArgumentException si el elemento no existe
     */
    public String[] getAttributeNames(String elementName) {
        Element element = element(elementName);
        return element.attrList.toArray(new String[element.attrList.size()]);
    }

    /** Que forma tiene el valor. */
    public int getAttributeValueType(String elementName, String attrName) {
        return attribute(elementName, attrName).valueType;
    }

    /** De que tipo es. */
    public int getAttributeDataType(String elementName, String attrName) {
        return attribute(elementName, attrName).dataType;
    }

    /** Si tiene que estar. */
    public boolean isAttributeRequired(String elementName, String attrName) {
        return attribute(elementName, attrName).required;
    }

    /** Que vale si no se pone, o null. */
    public String getAttributeDefaultValue(String elementName, String attrName) {
        return attribute(elementName, attrName).defaultValue;
    }

    /**
     * Los valores permitidos.
     *
     * @throws IllegalArgumentException si no es de tipo enumeracion
     */
    public String[] getAttributeEnumerations(String elementName, String attrName) {
        Attribute attr = attribute(elementName, attrName);
        if (attr.valueType != VALUE_ENUMERATION) {
            throw new IllegalArgumentException("Attribute not an enumeration!");
        }
        return attr.enumeratedValues.toArray(new String[attr.enumeratedValues.size()]);
    }

    /**
     * El minimo del rango.
     *
     * @throws IllegalArgumentException si no es de tipo rango
     */
    public String getAttributeMinValue(String elementName, String attrName) {
        Attribute attr = attribute(elementName, attrName);
        if ((attr.valueType & VALUE_RANGE) != VALUE_RANGE) {
            throw new IllegalArgumentException("Attribute not a range!");
        }
        return attr.minValue;
    }

    /**
     * El maximo.
     *
     * @throws IllegalArgumentException si no es de tipo rango
     */
    public String getAttributeMaxValue(String elementName, String attrName) {
        Attribute attr = attribute(elementName, attrName);
        if ((attr.valueType & VALUE_RANGE) != VALUE_RANGE) {
            throw new IllegalArgumentException("Attribute not a range!");
        }
        return attr.maxValue;
    }

    /**
     * Cuantos valores como minimo.
     *
     * @throws IllegalArgumentException si no es de tipo lista
     */
    public int getAttributeListMinLength(String elementName, String attrName) {
        Attribute attr = attribute(elementName, attrName);
        if (attr.valueType != VALUE_LIST) {
            throw new IllegalArgumentException("Attribute not a list!");
        }
        return attr.listMinLength;
    }

    /**
     * Cuantos como maximo.
     *
     * @throws IllegalArgumentException si no es de tipo lista
     */
    public int getAttributeListMaxLength(String elementName, String attrName) {
        Attribute attr = attribute(elementName, attrName);
        if (attr.valueType != VALUE_LIST) {
            throw new IllegalArgumentException("Attribute not a list!");
        }
        return attr.listMaxLength;
    }

    /** Que es ese atributo, o null. */
    public String getAttributeDescription(String elementName, String attrName, Locale locale) {
        attribute(elementName, attrName);
        return resource(elementName + "/" + attrName, locale);
    }

    /** Que forma tiene el objeto de usuario; {@link #VALUE_NONE} si no lleva. */
    public int getObjectValueType(String elementName) {
        Element element = element(elementName);
        if (element.objectValue == null) {
            return VALUE_NONE;
        }
        return element.objectValue.valueType;
    }

    /**
     * De que clase es.
     *
     * @throws IllegalArgumentException si el elemento no lleva objeto
     */
    public Class<?> getObjectClass(String elementName) {
        return objectValue(elementName).classType;
    }

    /** Que objeto va si no se pone, o null. */
    public Object getObjectDefaultValue(String elementName) {
        return objectValue(elementName).defaultValue;
    }

    /**
     * Los objetos permitidos.
     *
     * @throws IllegalArgumentException si no es de tipo enumeracion
     */
    public Object[] getObjectEnumerations(String elementName) {
        ObjectValue value = objectValue(elementName);
        if (value.valueType != VALUE_ENUMERATION) {
            throw new IllegalArgumentException("Not an enumeration!");
        }
        return value.enumeratedValues.toArray(new Object[value.enumeratedValues.size()]);
    }

    /**
     * El minimo.
     *
     * @throws IllegalArgumentException si no es de tipo rango
     */
    public Comparable<?> getObjectMinValue(String elementName) {
        ObjectValue value = objectValue(elementName);
        if ((value.valueType & VALUE_RANGE) != VALUE_RANGE) {
            throw new IllegalArgumentException("Not a range!");
        }
        return value.minValue;
    }

    /**
     * El maximo.
     *
     * @throws IllegalArgumentException si no es de tipo rango
     */
    public Comparable<?> getObjectMaxValue(String elementName) {
        ObjectValue value = objectValue(elementName);
        if ((value.valueType & VALUE_RANGE) != VALUE_RANGE) {
            throw new IllegalArgumentException("Not a range!");
        }
        return value.maxValue;
    }

    /**
     * Cuantos elementos como minimo.
     *
     * @throws IllegalArgumentException si no es un arreglo
     */
    public int getObjectArrayMinLength(String elementName) {
        ObjectValue value = objectValue(elementName);
        if (value.valueType != VALUE_LIST) {
            throw new IllegalArgumentException("Not a list!");
        }
        return value.arrayMinLength;
    }

    /**
     * Cuantos como maximo.
     *
     * @throws IllegalArgumentException si no es un arreglo
     */
    public int getObjectArrayMaxLength(String elementName) {
        ObjectValue value = objectValue(elementName);
        if (value.valueType != VALUE_LIST) {
            throw new IllegalArgumentException("Not a list!");
        }
        return value.arrayMaxLength;
    }

    /** El esquema de {@code javax_imageio_1.0}; siempre la misma instancia. */
    public static IIOMetadataFormat getStandardFormatInstance() {
        synchronized (IIOMetadataFormatImpl.class) {
            if (standardFormat == null) {
                standardFormat = new StandardMetadataFormat();
            }
            return standardFormat;
        }
    }

    /** El elemento, o falla si no esta. */
    private Element element(String elementName) {
        if (elementName == null) {
            throw new IllegalArgumentException("element name == null!");
        }
        Element element = this.elements.get(elementName);
        if (element == null) {
            throw new IllegalArgumentException("No such element: " + elementName);
        }
        return element;
    }

    /** El elemento, creandolo si hace falta. */
    private Element getOrCreate(String elementName) {
        if (elementName == null) {
            throw new IllegalArgumentException("element name == null!");
        }
        Element element = this.elements.get(elementName);
        if (element == null) {
            element = new Element(elementName);
            this.elements.put(elementName, element);
        }
        return element;
    }

    /** El atributo, o falla. */
    private Attribute attribute(String elementName, String attrName) {
        Element element = element(elementName);
        if (attrName == null) {
            throw new IllegalArgumentException("attribute name == null!");
        }
        Attribute attr = element.attributes.get(attrName);
        if (attr == null) {
            throw new IllegalArgumentException("No such attribute: " + attrName);
        }
        return attr;
    }

    /** El objeto de usuario, o falla. */
    private ObjectValue objectValue(String elementName) {
        Element element = element(elementName);
        if (element.objectValue == null) {
            throw new IllegalArgumentException("No object within element " + elementName);
        }
        return element.objectValue;
    }

    /** El texto de esa clave, o null. */
    private String resource(String key, Locale locale) {
        if (this.resourceBaseName == null) {
            return null;
        }
        Locale where = locale;
        if (where == null) {
            where = Locale.getDefault();
        }
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(this.resourceBaseName, where,
                                                             getClass().getClassLoader());
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            // No hay descripcion. Devolver null es lo correcto: el contrato lo admite, e inventar un
            // texto seria peor que no tener ninguno.
            return null;
        }
    }

    /** Que el tipo de dato sea uno de los cinco. */
    private static void checkDataType(int dataType) {
        if (dataType < DATATYPE_STRING || dataType > DATATYPE_DOUBLE) {
            throw new IllegalArgumentException("Invalid value for dataType!");
        }
    }

    /** Un elemento del esquema. */
    private static final class Element {

        /** Como se llama. */
        final String name;

        /** Cual de las seis politicas. */
        int childPolicy = CHILD_POLICY_EMPTY;

        /** Solo con {@link #CHILD_POLICY_REPEAT}. */
        int minChildren = 0;

        /** Idem. */
        int maxChildren = 0;

        /** Que hijos, en orden de declaracion. */
        final List<String> childList = new ArrayList<String>();

        /** Los atributos, por nombre. */
        final Map<String, Attribute> attributes = new HashMap<String, Attribute>();

        /** Sus nombres, en orden de declaracion: el mapa no lo conserva. */
        final List<String> attrList = new ArrayList<String>();

        /** El objeto de usuario, o null. */
        ObjectValue objectValue = null;

        Element(String name) {
            this.name = name;
        }
    }

    /** Un atributo del esquema. */
    private static final class Attribute {

        final String name;

        int valueType = VALUE_ARBITRARY;

        int dataType = DATATYPE_STRING;

        boolean required = false;

        String defaultValue = null;

        List<String> enumeratedValues = null;

        String minValue = null;

        String maxValue = null;

        int listMinLength = 0;

        int listMaxLength = Integer.MAX_VALUE;

        Attribute(String name) {
            this.name = name;
        }
    }

    /** El objeto de usuario declarado de un elemento. */
    private static final class ObjectValue {

        int valueType = VALUE_NONE;

        Class<?> classType = null;

        Object defaultValue = null;

        List<Object> enumeratedValues = null;

        Comparable<?> minValue = null;

        Comparable<?> maxValue = null;

        int arrayMinLength = 0;

        int arrayMaxLength = Integer.MAX_VALUE;
    }
}
