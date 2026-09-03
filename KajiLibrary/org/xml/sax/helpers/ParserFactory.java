package org.xml.sax.helpers;

import org.xml.sax.Parser;

// KajiLibrary's org.xml.sax.helpers.ParserFactory -- "dame el parser SAX1 que nombra la propiedad
// de sistema", y nada mas.
//
// Es la forma que tenia SAX1 de evitar una dependencia en tiempo de compilacion con un parser
// concreto: el nombre de la clase vive en la propiedad de sistema `org.xml.sax.parser`, esta
// clase la carga por reflexion y la castea a Parser. Cada manera de fallar es una excepcion
// chequeada distinta, y estan todas en la clausula throws en vez de envueltas:
//
//   NullPointerException      la propiedad ni siquiera esta seteada
//   ClassNotFoundException    nombra una clase que no esta en el classpath
//   IllegalAccessException    la clase o su constructor sin argumentos no es accesible
//   InstantiationException    es abstracta, o no tiene constructor sin argumentos
//   ClassCastException        cargo, pero no es un org.xml.sax.Parser
//
// Ojo que NullPointerException en una clausula throws es raro y es a proposito: SAX1 eligio
// indicar "sin configurar" con una excepcion no chequeada y documentarla. Tenerla en la clausula
// es parte del contrato, no adorno.
//
// Esta clase esta deprecada en el JDK junto con el resto de SAX1; XMLReaderFactory es su
// reemplazo de SAX2. En esta biblioteca, igual que en un JDK de fabrica sin parser configurado,
// las dos son mecanismos que no tienen nada que encontrar: KajiLibrary no trae ningun parser XML,
// asi que makeParser() tira NullPointerException salvo que quien llama apunte la propiedad a una
// clase propia. Esa es la respuesta correcta y no un stub -- el trabajo de la fabrica es
// encontrar el parser de otro, y buscar, busca.
public class ParserFactory {

    // No se instancia: aca todo es estatico.
    private ParserFactory() {
    }

    // El parser que nombra la propiedad de sistema `org.xml.sax.parser`.
    public static Parser makeParser()
            throws ClassNotFoundException, IllegalAccessException,
                   InstantiationException, NullPointerException,
                   ClassCastException {
        String className = System.getProperty("org.xml.sax.parser");
        if (className == null) {
            throw new NullPointerException("No value for sax.parser property");
        } else {
            return makeParser(className);
        }
    }

    // El parser con exactamente este nombre de clase, cargado con el propio loader de esta clase
    // para que se encuentre un driver que este al lado de SAX mismo.
    public static Parser makeParser(String className)
            throws ClassNotFoundException, IllegalAccessException,
                   InstantiationException, ClassCastException {
        ClassLoader loader = ParserFactory.class.getClassLoader();
        Class<?> c;
        if (loader == null) {
            c = Class.forName(className);
        } else {
            c = loader.loadClass(className);
        }
        // El cast es lo que convierte "cargue algo" en "cargue un Parser", y que falle es uno de
        // los resultados documentados.
        return (Parser) c.newInstance();
    }
}
