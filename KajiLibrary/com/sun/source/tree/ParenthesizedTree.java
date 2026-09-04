package com.sun.source.tree;

/**
 * Una expresion entre parentesis.
 *
 * <p>Se conserva en el arbol aunque no cambie el significado, y no es redundante: una herramienta
 * que reescribe codigo tiene que poder reemitir los parentesis que el autor puso, y una que analiza
 * estilo puede querer verlos.
 */
public interface ParenthesizedTree extends ExpressionTree {

    ExpressionTree getExpression();
}
