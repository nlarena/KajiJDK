package com.sun.jdi;

import java.util.List;
import java.util.Map;

/**
 * Una clase, interfaz o tipo de arreglo cargado en la maquina depurada.
 *
 * <p>Es el tipo mas grande de JDI porque es por donde se pregunta todo lo estatico: los campos, los
 * metodos, el fuente, la tabla de lineas.
 *
 * <p>Dos pares de metodos que conviene no confundir. {@code fields} da los campos
 * <strong>declarados</strong> y {@code allFields} agrega los heredados; {@code methods} y
 * {@code allMethods}, lo mismo. El primero de cada par es el que casi siempre se quiere y el
 * segundo el que casi siempre se usa por error.
 *
 * <p>{@code isPrepared} distingue una clase que la VM ya ligo de una que solo cargo. Antes de
 * prepararla no se le pueden leer los campos estaticos, y ahi sale la mitad de los
 * {@link ClassNotPreparedException}.
 *
 * @since 1.3
 */
public interface ReferenceType extends Type, Comparable<ReferenceType>, Accessible {

    /**
     * El nombre.
     *
     * @return el resultado
     */
    String name();

    /**
     * El generic signature.
     *
     * @return el resultado
     */
    String genericSignature();

    /**
     * El class loader.
     *
     * @return el resultado
     */
    ClassLoaderReference classLoader();

    /**
     * El module.
     *
     * @return el resultado
     */
    ModuleReference module();

    /**
     * El source name.
     *
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    String sourceName()
            throws AbsentInformationException;

    /**
     * El source names.
     *
     * @param name el String
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    List<String> sourceNames(String name)
            throws AbsentInformationException;

    /**
     * El source paths.
     *
     * @param name el String
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    List<String> sourcePaths(String name)
            throws AbsentInformationException;

    /**
     * El source debug extension.
     *
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    String sourceDebugExtension()
            throws AbsentInformationException;

    /**
     * Si static.
     *
     * @return el resultado
     */
    boolean isStatic();

    /**
     * Si abstract.
     *
     * @return el resultado
     */
    boolean isAbstract();

    /**
     * Si final.
     *
     * @return el resultado
     */
    boolean isFinal();

    /**
     * Si prepared.
     *
     * @return el resultado
     */
    boolean isPrepared();

    /**
     * Si verified.
     *
     * @return el resultado
     */
    boolean isVerified();

    /**
     * Si initialized.
     *
     * @return el resultado
     */
    boolean isInitialized();

    /**
     * El failed to initialize.
     *
     * @return el resultado
     */
    boolean failedToInitialize();

    /**
     * El fields.
     *
     * @return el resultado
     */
    List<Field> fields();

    /**
     * El visible fields.
     *
     * @return el resultado
     */
    List<Field> visibleFields();

    /**
     * Todos los fields, heredados incluidos.
     *
     * @return el resultado
     */
    List<Field> allFields();

    /**
     * El field by name.
     *
     * @param name el String
     * @return el resultado
     */
    Field fieldByName(String name);

    /**
     * El methods.
     *
     * @return el resultado
     */
    List<Method> methods();

    /**
     * El visible methods.
     *
     * @return el resultado
     */
    List<Method> visibleMethods();

    /**
     * Todos los methods, heredados incluidos.
     *
     * @return el resultado
     */
    List<Method> allMethods();

    /**
     * El methods by name.
     *
     * @param name el String
     * @return el resultado
     */
    List<Method> methodsByName(String name);

    /**
     * El methods by name.
     *
     * @param name el String
     * @param name2 el String
     * @return el resultado
     */
    List<Method> methodsByName(String name, String name2);

    /**
     * El nested types.
     *
     * @return el resultado
     */
    List<ReferenceType> nestedTypes();

    /**
     * El value.
     *
     * @param field el Field
     * @return el resultado
     */
    Value getValue(Field field);

    /**
     * El values.
     *
     * @param values el List<? extends Field>
     * @return el resultado
     */
    Map<Field, Value> getValues(List<? extends Field> values);

    /**
     * El class object.
     *
     * @return el resultado
     */
    ClassObjectReference classObject();

    /**
     * Todos los line locations, heredados incluidos.
     *
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    List<Location> allLineLocations()
            throws AbsentInformationException;

    /**
     * Todos los line locations, heredados incluidos.
     *
     * @param name el String
     * @param name2 el String
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    List<Location> allLineLocations(String name, String name2)
            throws AbsentInformationException;

    /**
     * El locations of line.
     *
     * @param index el int
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    List<Location> locationsOfLine(int index)
            throws AbsentInformationException;

    /**
     * El locations of line.
     *
     * @param name el String
     * @param name2 el String
     * @param index el int
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    List<Location> locationsOfLine(String name, String name2, int index)
            throws AbsentInformationException;

    /**
     * El available strata.
     *
     * @return el resultado
     */
    List<String> availableStrata();

    /**
     * El default stratum.
     *
     * @return el resultado
     */
    String defaultStratum();

    /**
     * El instances.
     *
     * @param index el long
     * @return el resultado
     */
    List<ObjectReference> instances(long index);

    /**
     * Dos reflejos son iguales si nombran a lo mismo en la misma VM.
     *
     * @param obj el Object
     * @return el resultado
     */
    boolean equals(Object obj);

    /**
     * Coherente con {@link #equals}.
     *
     * @return el resultado
     */
    int hashCode();

    /**
     * El major version.
     *
     * @return el resultado
     */
    int majorVersion();

    /**
     * El minor version.
     *
     * @return el resultado
     */
    int minorVersion();

    /**
     * El constant pool count.
     *
     * @return el resultado
     */
    int constantPoolCount();

    /**
     * El constant pool.
     *
     * @return el resultado
     */
    byte[] constantPool();
}
