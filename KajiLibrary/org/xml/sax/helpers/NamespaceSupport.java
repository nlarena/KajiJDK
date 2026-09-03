package org.xml.sax.helpers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// KajiLibrary's org.xml.sax.helpers.NamespaceSupport -- la pila que convierte `foo:bar` en un par
// (URI, nombre local).
//
// Los espacios de nombres de XML tienen alcance por elemento: un prefijo declarado en <a> se ve
// adentro de <a> y desaparece despues de </a>, y un elemento interno puede redeclarar el mismo
// prefijo para que signifique otra cosa. Asi que el estado es una pila de contextos, uno por
// elemento abierto, y quien llama es el que la maneja:
//
//     support.pushContext();                       // entrando a un elemento
//     por cada atributo xmlns:p="u":
//         support.declarePrefix("p", "u");
//     support.processName(qName, parts, false);    // el nombre del elemento mismo
//     ... hijos ...
//     support.popContext();                        // saliendo de el
//
// El orden importa: cada declarePrefix de un elemento tiene que pasar despues de su pushContext y
// antes del primer processName, porque processName memoriza sus respuestas por contexto y una
// declaracion que llega mas tarde puede no ser vista por los nombres ya resueltos. Nada obliga a
// esto -- el JDK llego a tener una bandera declsOK que tiraba IllegalStateException y la dejo
// comentada en el fuente que distribuye, asi que quien declara tarde simplemente recibe respuestas
// viejas. Esta biblioteca no vuelve a agregar el chequeo: tiraria donde el JDK devuelve, que es
// una mentira mas ruidosa que el silencio.
//
// Las reglas faciles de errar, todas respetadas aca:
//
//   - El prefijo por omision ("") aplica *solo a elementos*. Un atributo sin prefijo no esta en
//     ningun espacio de nombres, nunca en el de omision, asi que processName("id", parts, true)
//     da un URI de "" incluso con un espacio de nombres por omision vigente. Esta es la lectura
//     equivocada mas comun de la norma.
//   - "xml" viene predeclarado, permanentemente, a http://www.w3.org/XML/1998/namespace. Queda
//     ligado antes de que quien llama tenga oportunidad de hablar, y reset() lo repone.
//   - declarePrefix rechaza "xml" y "xmlns" devolviendo false. No tira: rechazarlos es un
//     resultado normal, y se espera que un parser trate ese false como "eso no era una
//     declaracion de espacio de nombres".
//   - Un prefijo sin ligar no es un error aca, es un null que devuelve processName. Decidir que
//     hacer con eso es asunto de quien llama.
//
// setNamespaceDeclUris(true) enciende el comportamiento opcional donde los atributos xmlns se
// reportan a su vez dentro de un espacio de nombres (NSDECL) en vez de ser invisibles. Solo se
// puede llamar sin ningun elemento abierto, es decir antes del primer pushContext o despues del
// popContext que le corresponde; si no, los contextos ya armados no coincidirian con los que
// vengan despues.
//
// El detalle de implementacion que hace que esto salga barato: un contexto que no declara nada no
// copia las tablas de su padre, las comparte. La copia ocurre en el primer declarePrefix, que
// para documentos reales es una minoria chica de los elementos. Por eso Context tiene tanto un
// `parent` como una bandera `declSeen`, y por eso clear() se toma el trabajo de soltar las
// referencias -- un contexto que se saco de la pila se reusa en el siguiente push a esa
// profundidad, y quedarse con las tablas viejas las mantendria vivas.
public class NamespaceSupport {

    ////////////////////////////////////////////////////////////////////
    // Constantes
    ////////////////////////////////////////////////////////////////////

    // El URI ligado al prefijo "xml", siempre, por la norma misma.
    public static final String XMLNS =
        "http://www.w3.org/XML/1998/namespace";

    // El URI de los atributos xmlns cuando setNamespaceDeclUris(true) esta vigente.
    public static final String NSDECL =
        "http://www.w3.org/xmlns/2000/";

    private static final Enumeration<String> EMPTY_ENUMERATION =
        Collections.enumeration(new ArrayList<String>());

    // Si String.intern() anda en la VM sobre la que estamos corriendo. Es un metodo nativo, y en
    // la VM propia de KajiJDK esta declarado pero no implementado, asi que llamarlo tira
    // UnsatisfiedLinkError. El NamespaceSupport del JDK internea todo prefijo, URI y nombre que
    // guarda; eso no es parte del contrato de SAX (nada documenta estas cadenas como interneadas)
    // pero es el comportamiento del JDK, y en una VM que puede hacerlo nosotros tambien lo
    // hacemos. En una que no puede, canon() devuelve la cadena sin tocar: los mapas van por
    // equals(), asi que toda respuesta que da esta clase es identica en cualquiera de los dos
    // casos -- lo unico que cambia es la identidad de referencia de las cadenas devueltas, y nada
    // de aca ni de ParserAdapter depende de eso.
    //
    // Se prueba una sola vez en vez de atrapar la falla en cada llamada, porque esto queda en el
    // medio del lazo mas caliente del procesamiento de espacios de nombres.
    private static final boolean PUEDE_INTERNAR = pruebaIntern();

    private static boolean pruebaIntern() {
        try {
            String s = "";
            return s.intern() != null;
        } catch (Throwable e) {
            return false;
        }
    }

    private static String canon(String s) {
        if (PUEDE_INTERNAR) {
            return s.intern();
        }
        return s;
    }

    ////////////////////////////////////////////////////////////////////
    // Estado
    ////////////////////////////////////////////////////////////////////

    private Context[] contexts;
    private Context currentContext;
    private int contextPos;
    private boolean namespaceDeclUris;

    // Arranca reseteado, es decir con un contexto que tiene la ligadura predeclarada de "xml".
    public NamespaceSupport() {
        reset();
    }

    // De vuelta al estado inicial, listo para otro documento. Tira todos los contextos y repone la
    // ligadura de "xml"; tambien limpia namespaceDeclUris.
    public void reset() {
        contexts = new Context[32];
        namespaceDeclUris = false;
        contextPos = 0;
        // Partido en dos a proposito: `contexts[i] = currentContext = new Context()` compila
        // mal con el javac de esta casa. Ver el comentario en pushContext().
        currentContext = new Context();
        contexts[contextPos] = currentContext;
        currentContext.declarePrefix("xml", XMLNS);
    }

    ////////////////////////////////////////////////////////////////////
    // La pila
    ////////////////////////////////////////////////////////////////////

    // Entrar a un elemento.
    public void pushContext() {
        int max = contexts.length;

        contextPos++;

        // Crece si nos quedamos sin lugar. Los documentos profundos son raros; 32 cubre casi
        // todo.
        if (contextPos >= max) {
            Context newContexts[] = new Context[max * 2];
            System.arraycopy(contexts, 0, newContexts, 0, max);
            contexts = newContexts;
        }

        // Reusar el objeto Context que dejo a esta profundidad un hermano anterior, si lo hay.
        currentContext = contexts[contextPos];
        if (currentContext == null) {
            // Partido en dos. El javac de esta casa genera mal el bytecode de
            // `arreglo[i] = campoDeInstancia = valor`: deja la pila mal armada y el aastore
            // se encuentra un int donde tiene que haber una referencia, con lo que la VM se
            // cae. `campo = valor; arreglo[i] = campo;` es lo mismo y sale bien. Repro en
            // el informe; con un campo estatico o una variable local en el medio no pasa.
            currentContext = new Context();
            contexts[contextPos] = currentContext;
        }

        if (contextPos > 0) {
            currentContext.setParent(contexts[contextPos - 1]);
        }
    }

    // Salir de un elemento. El objeto Context se queda en el arreglo para reusarse, pero sus
    // tablas se sueltan asi nada sobrevive al elemento.
    public void popContext() {
        contexts[contextPos].clear();

        contextPos--;
        if (contextPos < 0) {
            throw new EmptyStackException();
        }
        currentContext = contexts[contextPos];
    }

    ////////////////////////////////////////////////////////////////////
    // Declaraciones
    ////////////////////////////////////////////////////////////////////

    // Liga `prefix` a `uri` para el elemento actual y sus hijos. Un prefijo vacio fija el espacio
    // de nombres por omision, y un uri vacio lo desactiva (que es como funciona xmlns="").
    //
    // Devuelve false, sin declarar nada, para "xml" y "xmlns": el primero ya esta ligado y no se
    // puede religar, el segundo directamente no es un prefijo.
    public boolean declarePrefix(String prefix, String uri) {
        if (prefix.equals("xml") || prefix.equals("xmlns")) {
            return false;
        } else {
            currentContext.declarePrefix(prefix, uri);
            return true;
        }
    }

    ////////////////////////////////////////////////////////////////////
    // Resolucion
    ////////////////////////////////////////////////////////////////////

    // Parte un nombre calificado en (URI, nombre local, qName) y escribe los tres en `parts`, que
    // tiene que tener lugar para tres. Devuelve `parts` cuando sale bien y null cuando el prefijo
    // del nombre no esta ligado.
    //
    // `isAttribute` no es cosmetico: un nombre de *elemento* sin prefijo toma el espacio de
    // nombres por omision, un nombre de *atributo* sin prefijo no toma ninguno. Ver el comentario
    // de la clase.
    public String[] processName(String qName, String[] parts,
                                boolean isAttribute) {
        String[] myParts = currentContext.processName(qName, isAttribute);
        if (myParts == null) {
            return null;
        } else {
            parts[0] = myParts[0];
            parts[1] = myParts[1];
            parts[2] = myParts[2];
            return parts;
        }
    }

    // El URI que este prefijo significa en este momento, o null si no significa nada. "" pregunta
    // por el espacio de nombres por omision.
    public String getURI(String prefix) {
        return currentContext.getURI(prefix);
    }

    // Todos los prefijos en alcance en este momento, *salvo* el de omision y salvo "xml". El de
    // omision queda afuera porque no es un prefijo y getURI("") ya contesta por el; "xml" queda
    // afuera solo en el sentido de que se declaro en el contexto raiz y por lo tanto si aparece
    // -- quien se preocupe por eso lo filtra por su cuenta.
    public Enumeration<String> getPrefixes() {
        return currentContext.getPrefixes();
    }

    // Un prefijo ligado a este URI, o null. Cual, cuando hay varios ligados al mismo URI, no esta
    // especificado; el prefijo por omision nunca se devuelve, porque quien hace esta pregunta
    // quiere algo que pueda poner delante de dos puntos.
    public String getPrefix(String uri) {
        return currentContext.getPrefix(uri);
    }

    // Todos los prefijos ligados a este URI, a diferencia de la respuesta unica de getPrefix.
    public Enumeration<String> getPrefixes(String uri) {
        List<String> prefixes = new ArrayList<String>();
        Enumeration<String> allPrefixes = getPrefixes();
        while (allPrefixes.hasMoreElements()) {
            String prefix = allPrefixes.nextElement();
            if (uri.equals(getURI(prefix))) {
                prefixes.add(prefix);
            }
        }
        return Collections.enumeration(prefixes);
    }

    // Los prefijos declarados *por este elemento mismo*, no los heredados. Esto es lo que un
    // parser reporta a traves de startPrefixMapping/endPrefixMapping.
    public Enumeration<String> getDeclaredPrefixes() {
        return currentContext.getDeclaredPrefixes();
    }

    ////////////////////////////////////////////////////////////////////
    // El modo opcional de xmlns-dentro-de-un-espacio-de-nombres
    ////////////////////////////////////////////////////////////////////

    // Solo se puede llamar entre documentos, es decir sin ningun elemento abierto. Cambiarlo en
    // medio del analisis haria que los contextos ya armados no coincidan con los que vengan
    // despues, asi que tira.
    public void setNamespaceDeclUris(boolean value) {
        if (contextPos != 0) {
            throw new IllegalStateException();
        }
        if (value == namespaceDeclUris) {
            return;
        }
        namespaceDeclUris = value;
        if (value) {
            currentContext.declarePrefix("xmlns", NSDECL);
        } else {
            // Apagarlo tiene que soltar la ligadura de xmlns, y la forma correcta mas barata es un
            // contexto raiz nuevo con solo "xml" adentro. Partido en dos por lo de pushContext().
            currentContext = new Context();
            contexts[contextPos] = currentContext;
            currentContext.declarePrefix("xml", XMLNS);
        }
    }

    public boolean isNamespaceDeclUris() {
        return namespaceDeclUris;
    }

    ////////////////////////////////////////////////////////////////////
    // Las ligaduras que corresponden a un elemento.
    ////////////////////////////////////////////////////////////////////

    // Interna y no estatica porque processName tiene que consultar la bandera namespaceDeclUris
    // del NamespaceSupport que la contiene.
    //
    // Las dos tablas de nombres son caches de memorizacion: dentro de un mismo elemento, el mismo
    // qName resuelve siempre a la misma terna, y los elementos repiten nombres de atributo todo el
    // tiempo. Estan separadas para elementos y atributos justamente porque los dos resuelven
    // distinto.
    final class Context {

        Map<String, String> prefixTable;
        Map<String, String> uriTable;
        Map<String, String[]> elementNameTable;
        Map<String, String[]> attributeNameTable;
        String defaultNS = null;
        boolean declSeen = false;

        private List<String> declarations = null;
        private Context parent = null;

        Context() {
            copyTables();
        }

        // Reusar este objeto para un elemento nuevo bajo `parent`. Comparte las tablas del padre
        // en vez de copiarlas; la copia pasa en declarePrefix, y solo si alguna vez se lo llama.
        void setParent(Context parent) {
            this.parent = parent;
            declarations = null;
            prefixTable = parent.prefixTable;
            uriTable = parent.uriTable;
            elementNameTable = parent.elementNameTable;
            attributeNameTable = parent.attributeNameTable;
            defaultNS = parent.defaultNS;
            declSeen = false;
        }

        // Soltar todo al salir. El objeto sobrevive en el arreglo contexts para reusarse, pero no
        // tiene que dejar alcanzables las tablas del elemento que se saco de la pila.
        void clear() {
            parent = null;
            prefixTable = null;
            uriTable = null;
            elementNameTable = null;
            attributeNameTable = null;
            defaultNS = null;
        }

        void declarePrefix(String prefix, String uri) {
            // Primera declaracion en este elemento: dejar de compartir las tablas del padre.
            if (!declSeen) {
                copyTables();
            }
            if (declarations == null) {
                declarations = new ArrayList<String>();
            }

            // Interneados para que las ternas cacheadas de processName se puedan comparar y
            // compartir barato, y para que las cadenas que se le entregan a quien llama sean las
            // canonicas.
            prefix = canon(prefix);
            uri = canon(uri);
            if ("".equals(prefix)) {
                // xmlns="" desactiva el espacio de nombres por omision en vez de ligarlo a "".
                if ("".equals(uri)) {
                    defaultNS = null;
                } else {
                    defaultNS = uri;
                }
            } else {
                prefixTable.put(prefix, uri);
                uriTable.put(uri, prefix);
            }
            declarations.add(prefix);
        }

        String[] processName(String qName, boolean isAttribute) {
            Map<String, String[]> table;

            // La division entre elemento y atributo, que es toda la razon de que haya dos tablas.
            if (isAttribute) {
                table = attributeNameTable;
            } else {
                table = elementNameTable;
            }

            String[] name = table.get(qName);
            if (name != null) {
                return name;
            }

            name = new String[3];
            name[2] = canon(qName);
            int index = qName.indexOf(':');

            // Sin dos puntos: un nombre sin prefijo.
            if (index == -1) {
                if (isAttribute) {
                    // Un atributo sin prefijo no esta en ningun espacio de nombres -- nunca en el
                    // de omision. La unica excepcion es el atributo xmlns mismo, y solo cuando
                    // quien llama pidio que a los atributos xmlns se les de un URI.
                    //
                    // El `==` no es un desliz ni es un equals() disfrazado: aca el JDK compara
                    // por identidad, asi que un "xmlns" que no sea el literal interneado --uno
                    // construido en tiempo de ejecucion, pongamos-- cae en "" incluso con la
                    // funcionalidad encendida. Verificado contra jdk-25.0.2: processName(new
                    // StringBuilder("xml").append("ns").toString(), p, true) contesta "" alla, y
                    // contesta "" aca. Escrito como equals() esta biblioteca no coincidiria con
                    // el JDK ante esa entrada, asi que identidad es. Los parsers le pasan a este
                    // metodo nombres que vinieron del documento, y un parser que los internee
                    // (cosa que ParserAdapter no necesita hacer, porque filtra los atributos
                    // xmlns antes siquiera de preguntar) es el unico llamador para el que esta
                    // rama se dispara.
                    if (qName == "xmlns" && namespaceDeclUris) {
                        name[0] = NSDECL;
                    } else {
                        name[0] = "";
                    }
                } else if (defaultNS == null) {
                    name[0] = "";
                } else {
                    name[0] = defaultNS;
                }
                name[1] = name[2];
            }

            // Un nombre con prefijo.
            else {
                String prefix = qName.substring(0, index);
                String local = qName.substring(index + 1);
                String uri;
                if ("".equals(prefix)) {
                    uri = defaultNS;
                } else {
                    uri = prefixTable.get(prefix);
                }
                // Un prefijo sin ligar, o un xmlns:* usado donde se esperaba un nombre de
                // elemento, es una respuesta null y no una excepcion: que hacer con eso es
                // decision de quien llama, no nuestra.
                if (uri == null
                        || (!isAttribute && "xmlns".equals(prefix))) {
                    return null;
                }
                name[0] = uri;
                name[1] = canon(local);
            }

            table.put(name[2], name);
            return name;
        }

        String getURI(String prefix) {
            if ("".equals(prefix)) {
                return defaultNS;
            } else if (prefixTable == null) {
                return null;
            } else {
                return prefixTable.get(prefix);
            }
        }

        String getPrefix(String uri) {
            if (uriTable == null) {
                return null;
            } else {
                return uriTable.get(uri);
            }
        }

        Enumeration<String> getDeclaredPrefixes() {
            if (declarations == null) {
                return EMPTY_ENUMERATION;
            } else {
                return Collections.enumeration(declarations);
            }
        }

        Enumeration<String> getPrefixes() {
            if (prefixTable == null) {
                return EMPTY_ENUMERATION;
            } else {
                return Collections.enumeration(prefixTable.keySet());
            }
        }

        // Sacar copias privadas de lo que veniamos compartiendo con el padre. Los dos caches de
        // nombres *no* se copian sino que arrancan vacios: memorizan respuestas que dependen de
        // las ligaduras, y las ligaduras estan por cambiar.
        private void copyTables() {
            if (prefixTable != null) {
                prefixTable = new HashMap<String, String>(prefixTable);
            } else {
                prefixTable = new HashMap<String, String>();
            }
            if (uriTable != null) {
                uriTable = new HashMap<String, String>(uriTable);
            } else {
                uriTable = new HashMap<String, String>();
            }
            elementNameTable = new HashMap<String, String[]>();
            attributeNameTable = new HashMap<String, String[]>();
            declSeen = true;
        }
    }
}
