package java.lang.classfile;

// Una pieza del cuerpo de un método que NO es una instrucción: una etiqueta, la marca de un número
// de línea, el alcance de una variable local o un manejador de excepción. No ocupa bytes en el
// arreglo `code`; sale de las tablas de los atributos que lo acompañan.
public interface PseudoInstruction extends CodeElement {
}
