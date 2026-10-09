package com.sun.source.util;

import javax.lang.model.element.VariableElement;

/**
 * Where the compiler gets a parameter's name from when the {@code .class} does not bring it.
 *
 * <h2>Why it may not bring it</h2>
 *
 * <p>Because keeping the parameters' names is optional: {@code javac} only emits them with
 * {@code -parameters}. Without that, a compiled class exposes {@code arg0}, {@code arg1} --
 * which compiles all the same but is useless for a tool that generates code or
 * documentation.
 *
 * <p>This hook allows them to be completed from somewhere else: a metadata file, the source if
 * it is at hand, a convention. Returning {@code null} is saying "I do not know", and there the
 * {@code argN} stays.
 */
public interface ParameterNameProvider {

    /** That parameter's name, or {@code null} if it is not known. */
    CharSequence getParameterName(VariableElement parameter);
}
