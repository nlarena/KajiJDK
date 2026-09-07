package com.sun.jdi;

import java.util.List;

/**
 * Un metodo de un tipo de la maquina depurada.
 *
 * <p>{@code arguments} necesita la tabla de variables locales y puede no estar;
 * {@code argumentTypeNames} sale de la firma y siempre esta. Es la diferencia entre saber como se
 * llamaban los parametros y saber de que tipo eran.
 *
 * <p>{@code isObsolete} marca un metodo cuyo codigo se reemplazo en caliente: la referencia sigue
 * valiendo y el codigo que representaba ya no existe.
 *
 * @since 1.3
 */
public interface Method extends TypeComponent, Locatable, Comparable<Method> {

    /**
     * El return type name.
     *
     * @return el resultado
     */
    String returnTypeName();

    /**
     * El return type.
     *
     * @return el resultado
     * @throws ClassNotLoadedException si corresponde
     */
    Type returnType()
            throws ClassNotLoadedException;

    /**
     * El argument type names.
     *
     * @return el resultado
     */
    List<String> argumentTypeNames();

    /**
     * El argument types.
     *
     * @return el resultado
     * @throws ClassNotLoadedException si corresponde
     */
    List<Type> argumentTypes()
            throws ClassNotLoadedException;

    /**
     * Si abstract.
     *
     * @return el resultado
     */
    boolean isAbstract();

    /**
     * Si default.
     *
     * @return el resultado
     */
    boolean isDefault();

    /**
     * Si synchronized.
     *
     * @return el resultado
     */
    boolean isSynchronized();

    /**
     * Si native.
     *
     * @return el resultado
     */
    boolean isNative();

    /**
     * Si var args.
     *
     * @return el resultado
     */
    boolean isVarArgs();

    /**
     * Si bridge.
     *
     * @return el resultado
     */
    boolean isBridge();

    /**
     * Si constructor.
     *
     * @return el resultado
     */
    boolean isConstructor();

    /**
     * Si static initializer.
     *
     * @return el resultado
     */
    boolean isStaticInitializer();

    /**
     * Si obsolete.
     *
     * @return el resultado
     */
    boolean isObsolete();

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
     * El location of code index.
     *
     * @param index el long
     * @return el resultado
     */
    Location locationOfCodeIndex(long index);

    /**
     * El variables.
     *
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    List<LocalVariable> variables()
            throws AbsentInformationException;

    /**
     * El variables by name.
     *
     * @param name el String
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    List<LocalVariable> variablesByName(String name)
            throws AbsentInformationException;

    /**
     * El arguments.
     *
     * @return el resultado
     * @throws AbsentInformationException si corresponde
     */
    List<LocalVariable> arguments()
            throws AbsentInformationException;

    /**
     * El bytecodes.
     *
     * @return el resultado
     */
    byte[] bytecodes();

    /**
     * El location.
     *
     * @return el resultado
     */
    Location location();

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
}
