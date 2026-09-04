import java.util.Enumeration;
import java.util.Properties;

import javax.naming.Binding;
import javax.naming.BinaryRefAddr;
import javax.naming.CompositeName;
import javax.naming.CompoundName;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.InvalidNameException;
import javax.naming.LinkRef;
import javax.naming.Name;
import javax.naming.NameClassPair;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;

/**
 * Prueba de comportamiento de javax.naming, escrita para correr **igual** en esta VM y en el JDK
 * real.
 *
 * <p>Cada comprobacion tiene un indice. {@code run()} devuelve -1 si pasaron todas, o el indice de
 * la primera que fallo: un solo int alcanza para comparar las dos VMs sin depender de que la salida
 * por consola coincida caracter por caracter.
 *
 * <p>El grueso esta en `CompoundName` y `CompositeName`, que son las unicas dos clases del paquete
 * con logica de verdad. Lo que se persigue ahi es una sola invariante --que `toString()` vuelva a
 * parsearse-- y sus tres consecuencias incomodas: el citado, el escape, y los componentes vacios.
 * El resto de las clases son contenedores y se prueban por su igualdad y su forma de cadena.
 *
 * <p>Nada de esto toca la red ni el disco: sin proveedor instalado, un `InitialContext` falla
 * enseguida, y eso es justamente uno de los casos.
 */
public class JndiTest {

    // ---- sintaxis de ejemplo ------------------------------------------------------------------------

    /** LDAP: coma, de derecha a izquierda, con comillas y contrabarra. */
    private static Properties ldap() {
        Properties p = new Properties();
        p.put("jndi.syntax.direction", "right_to_left");
        p.put("jndi.syntax.separator", ",");
        p.put("jndi.syntax.escape", "\\");
        p.put("jndi.syntax.beginquote", "\"");
        return p;
    }

    /** Estilo sistema de archivos: barra, de izquierda a derecha. */
    private static Properties archivo() {
        Properties p = new Properties();
        p.put("jndi.syntax.direction", "left_to_right");
        p.put("jndi.syntax.separator", "/");
        p.put("jndi.syntax.escape", "\\");
        p.put("jndi.syntax.beginquote", "\"");
        return p;
    }

    /** Plano: la cadena entera es un componente. */
    private static Properties plano() {
        Properties p = new Properties();
        p.put("jndi.syntax.direction", "flat");
        return p;
    }

    /** De izquierda a derecha, ignorando mayusculas y recortando blancos. */
    private static Properties laxa() {
        Properties p = new Properties();
        p.put("jndi.syntax.direction", "left_to_right");
        p.put("jndi.syntax.separator", "/");
        p.put("jndi.syntax.ignorecase", "true");
        p.put("jndi.syntax.trimblanks", "true");
        return p;
    }

    private static boolean mismo(Name n, String[] esperado) {
        if (n.size() != esperado.length) {
            return false;
        }
        for (int i = 0; i < esperado.length; i++) {
            if (!n.get(i).equals(esperado[i])) {
                return false;
            }
        }
        return true;
    }

    public static int run() {
        int i = 0;
        try {
            // ---- CompositeName: partir y contar ---------------------------------------------------

            if (!mismo(new CompositeName("a/b/c"), new String[] {"a", "b", "c"})) return i; i++;   // 0
            if (new CompositeName("").size() != 0) return i; i++;                                  // 1
            if (!new CompositeName().isEmpty()) return i; i++;                                     // 2

            // Los vacios, que es donde casi todos se equivocan: "/" es UN componente vacio, no dos.
            if (!mismo(new CompositeName("/"), new String[] {""})) return i; i++;                   // 3
            if (!mismo(new CompositeName("a/"), new String[] {"a", ""})) return i; i++;             // 4
            if (!mismo(new CompositeName("/a"), new String[] {"", "a"})) return i; i++;             // 5
            if (!mismo(new CompositeName("a//b"), new String[] {"a", "", "b"})) return i; i++;      // 6
            if (!mismo(new CompositeName("//"), new String[] {"", ""})) return i; i++;              // 7

            // ---- CompositeName: la invariante del ida y vuelta -------------------------------------

            String[] casos = {"", "/", "a", "a/b/c", "a/", "/a", "a//b", "//",
                              "\"a/b\"", "a\\/b", "x y/z"};
            for (int k = 0; k < casos.length; k++) {
                CompositeName n = new CompositeName(casos[k]);
                CompositeName vuelta = new CompositeName(n.toString());
                if (!n.equals(vuelta)) return i;
                if (n.size() != vuelta.size()) return i;
            }
            i++;                                                                                   // 8

            // El vacio y el de un componente vacio NO se imprimen igual: si lo hicieran, el ida y
            // vuelta de arriba no podria distinguirlos.
            if (new CompositeName("").toString().equals(new CompositeName("/").toString())) return i; i++; // 9

            // ---- CompositeName: citado y escape ----------------------------------------------------

            if (!mismo(new CompositeName("\"a/b\""), new String[] {"a/b"})) return i; i++;         // 10
            if (!mismo(new CompositeName("a\\/b"), new String[] {"a/b"})) return i; i++;           // 11
            if (!mismo(new CompositeName("'a/b'"), new String[] {"a/b"})) return i; i++;           // 12
            // Una comilla en el medio es un caracter comun: solo abre cita al principio.
            if (!mismo(new CompositeName("a\"b"), new String[] {"a\"b"})) return i; i++;           // 13

            // Un componente con separador adentro tiene que salir citado o escapado, y volver.
            CompositeName conBarra = new CompositeName();
            conBarra.add("a/b");
            if (conBarra.size() != 1) return i; i++;                                               // 14
            if (!mismo(new CompositeName(conBarra.toString()), new String[] {"a/b"})) return i; i++; // 15

            // ---- CompositeName: prefijos, sufijos y pertenencia ------------------------------------

            CompositeName abc = new CompositeName("a/b/c");
            if (!abc.getPrefix(0).isEmpty()) return i; i++;                                        // 16
            if (!mismo(abc.getPrefix(2), new String[] {"a", "b"})) return i; i++;                   // 17
            if (!mismo(abc.getPrefix(3), new String[] {"a", "b", "c"})) return i; i++;              // 18
            if (!mismo(abc.getSuffix(1), new String[] {"b", "c"})) return i; i++;                   // 19
            if (!abc.getSuffix(3).isEmpty()) return i; i++;                                        // 20

            if (!abc.startsWith(new CompositeName("a/b"))) return i; i++;                          // 21
            if (!abc.startsWith(new CompositeName(""))) return i; i++;                             // 22
            if (abc.startsWith(new CompositeName("b"))) return i; i++;                             // 23
            if (!abc.endsWith(new CompositeName("b/c"))) return i; i++;                            // 24
            if (abc.endsWith(new CompositeName("a"))) return i; i++;                               // 25
            if (!abc.endsWith(new CompositeName(""))) return i; i++;                               // 26
            // Un nombre mas largo que este no puede ser prefijo ni sufijo.
            if (abc.startsWith(new CompositeName("a/b/c/d"))) return i; i++;                       // 27

            // Un CompoundName nunca es prefijo de un CompositeName, tengan lo que tengan adentro.
            if (abc.startsWith(new CompoundName("a/b", archivo()))) return i; i++;                 // 28

            // ---- CompositeName: mutacion ------------------------------------------------------------

            CompositeName m = new CompositeName("a/b");
            // Devuelven `this` ya modificado: la identidad es parte del contrato.
            if (m.add("c") != m) return i; i++;                                                    // 29
            if (!mismo(m, new String[] {"a", "b", "c"})) return i; i++;                             // 30
            m.add(0, "z");
            if (!mismo(m, new String[] {"z", "a", "b", "c"})) return i; i++;                        // 31
            Object sacado = m.remove(1);
            if (!"a".equals(sacado)) return i; i++;                                                // 32
            if (!mismo(m, new String[] {"z", "b", "c"})) return i; i++;                             // 33
            m.addAll(new CompositeName("p/q"));
            if (!mismo(m, new String[] {"z", "b", "c", "p", "q"})) return i; i++;                   // 34
            m.addAll(1, new CompositeName("w"));
            if (!mismo(m, new String[] {"z", "w", "b", "c", "p", "q"})) return i; i++;              // 35

            // Pegar algo que no es compuesto es InvalidNameException, no ClassCastException.
            try {
                new CompositeName("a").addAll(new CompoundName("b", archivo()));
                return i;                                                                          // 36
            } catch (InvalidNameException esperada) {
                i++;
            }

            // ---- CompositeName: igualdad, orden y clon ----------------------------------------------

            if (!new CompositeName("a/b").equals(new CompositeName("a/b"))) return i; i++;         // 37
            if (new CompositeName("a/b").equals(new CompositeName("A/b"))) return i; i++;          // 38
            if (new CompositeName("a/b").hashCode() != new CompositeName("a/b").hashCode()) return i; i++; // 39
            // Mismos componentes pero otro tipo: no son iguales.
            if (new CompositeName("a/b").equals(new CompoundName("a/b", archivo()))) return i; i++; // 40
            if (new CompositeName("a").compareTo(new CompositeName("a")) != 0) return i; i++;      // 41
            if (new CompositeName("a").compareTo(new CompositeName("b")) >= 0) return i; i++;      // 42
            // Prefijo comun: gana el mas corto.
            if (new CompositeName("a").compareTo(new CompositeName("a/b")) >= 0) return i; i++;    // 43
            try {
                new CompositeName("a").compareTo(new CompoundName("a", archivo()));
                return i;                                                                          // 44
            } catch (ClassCastException esperada) {
                i++;
            }

            // El clon es independiente: mutar la copia no toca el original.
            CompositeName orig = new CompositeName("a/b");
            CompositeName copia = (CompositeName) orig.clone();
            copia.add("c");
            if (orig.size() != 2) return i; i++;                                                   // 45
            if (copia.size() != 3) return i; i++;                                                  // 46

            // getAll recorre en orden.
            Enumeration<String> e = new CompositeName("a/b/c").getAll();
            StringBuilder sb = new StringBuilder();
            while (e.hasMoreElements()) sb.append(e.nextElement());
            if (!"abc".equals(sb.toString())) return i; i++;                                       // 47

            // ---- CompoundName: de izquierda a derecha ------------------------------------------------

            if (!mismo(new CompoundName("a/b/c", archivo()), new String[] {"a", "b", "c"})) return i; i++; // 48
            if (!"a/b/c".equals(new CompoundName("a/b/c", archivo()).toString())) return i; i++;   // 49

            // ---- CompoundName: de derecha a izquierda, que es lo que sorprende -----------------------

            CompoundName dn = new CompoundName("cn=juan,o=acme", ldap());
            // El componente 0 es el mas significativo, o sea el de MAS A LA DERECHA.
            if (!mismo(dn, new String[] {"o=acme", "cn=juan"})) return i; i++;                     // 50
            if (!"cn=juan,o=acme".equals(dn.toString())) return i; i++;                            // 51
            // El prefijo son los mas significativos, que del lado de la cadena estan a la derecha.
            if (!"o=acme".equals(dn.getPrefix(1).toString())) return i; i++;                       // 52
            if (!"cn=juan".equals(dn.getSuffix(1).toString())) return i; i++;                      // 53

            CompoundName dn2 = (CompoundName) dn.clone();
            dn2.add("c=ar");
            if (!mismo(dn2, new String[] {"o=acme", "cn=juan", "c=ar"})) return i; i++;            // 54
            if (!"c=ar,cn=juan,o=acme".equals(dn2.toString())) return i; i++;                      // 55

            dn2.add(0, "dc=raiz");
            if (!"c=ar,cn=juan,o=acme,dc=raiz".equals(dn2.toString())) return i; i++;              // 56

            if (!dn.startsWith(new CompoundName("o=acme", ldap()))) return i; i++;                 // 57
            if (!dn.endsWith(new CompoundName("cn=juan", ldap()))) return i; i++;                  // 58
            if (dn.startsWith(new CompoundName("cn=juan", ldap()))) return i; i++;                 // 59

            // El ida y vuelta tiene que valer tambien de derecha a izquierda.
            if (!dn2.equals(new CompoundName(dn2.toString(), ldap()))) return i; i++;              // 60

            // ---- CompoundName: citado y escape con la sintaxis dada ----------------------------------

            if (!mismo(new CompoundName("\"a,b\"", ldap()), new String[] {"a,b"})) return i; i++;  // 61
            if (!mismo(new CompoundName("a\\,b", ldap()), new String[] {"a,b"})) return i; i++;    // 62

            // Un componente con el separador adentro sale citado o escapado, y vuelve entero.
            CompoundName conComa = new CompoundName("x", ldap());
            conComa.add("a,b");
            if (conComa.size() != 2) return i; i++;                                                // 63
            CompoundName vueltaComa = new CompoundName(conComa.toString(), ldap());
            if (!vueltaComa.equals(conComa)) return i; i++;                                        // 64
            if (!mismo(vueltaComa, new String[] {"x", "a,b"})) return i; i++;                      // 65

            // Y uno que empieza con la comilla: la comilla se escapa, porque al principio abre cita.
            CompoundName conComilla = new CompoundName("", ldap());
            CompoundName cc = new CompoundName("z", ldap());
            cc.add("\"raro");
            if (!mismo(new CompoundName(cc.toString(), ldap()), new String[] {"z", "\"raro"})) return i; i++; // 66

            // Y uno con contrabarra, que si no se duplica se la come el parseo.
            CompoundName cb = new CompoundName("z", ldap());
            cb.add("a\\b");
            if (!mismo(new CompoundName(cb.toString(), ldap()), new String[] {"z", "a\\b"})) return i; i++;   // 67

            // ---- CompoundName: plano ------------------------------------------------------------------

            CompoundName pl = new CompoundName("a/b/c", plano());
            if (!mismo(pl, new String[] {"a/b/c"})) return i; i++;                                 // 68
            if (!"a/b/c".equals(pl.toString())) return i; i++;                                     // 69
            // Un nombre plano no puede tener dos componentes.
            try {
                pl.add("x");
                return i;                                                                          // 70
            } catch (InvalidNameException esperada) {
                i++;
            }
            // Pero uno vacio si acepta el primero.
            CompoundName pv = new CompoundName("", plano());
            pv.add("unico");
            if (!mismo(pv, new String[] {"unico"})) return i; i++;                                 // 71

            // ---- CompoundName: ignorar mayusculas y recortar blancos ----------------------------------

            CompoundName lax = new CompoundName("A/B", laxa());
            if (!lax.equals(new CompoundName("a/b", laxa()))) return i; i++;                       // 72
            if (lax.hashCode() != new CompoundName("a/b", laxa()).hashCode()) return i; i++;       // 73
            if (!new CompoundName(" a / b ", laxa()).equals(new CompoundName("a/b", laxa()))) return i; i++; // 74
            if (new CompoundName("A/B", laxa()).compareTo(new CompoundName("a/b", laxa())) != 0) return i; i++; // 75
            // Y la asimetria del contrato: la sintaxis que manda es la del que pregunta.
            if (!lax.startsWith(new CompoundName("a", laxa()))) return i; i++;                     // 76

            // ---- CompoundName: sintaxis obligatoria y tipos --------------------------------------------

            try {
                new CompoundName("a", null);
                return i;                                                                          // 77
            } catch (NullPointerException esperada) {
                i++;
            }
            try {
                new CompoundName("a", archivo()).addAll(new CompositeName("b"));
                return i;                                                                          // 78
            } catch (InvalidNameException esperada) {
                i++;
            }
            if (new CompoundName("a", archivo()).startsWith(new CompositeName("a"))) return i; i++; // 79

            // El clon comparte la sintaxis, asi que sigue siendo comparable con el original.
            CompoundName cl = (CompoundName) new CompoundName("a/b", archivo()).clone();
            if (!cl.equals(new CompoundName("a/b", archivo()))) return i; i++;                     // 80

            // ---- NameClassPair y Binding ---------------------------------------------------------------

            NameClassPair ncp = new NameClassPair("juan", "java.lang.String");
            if (!"juan".equals(ncp.getName())) return i; i++;                                      // 81
            if (!"java.lang.String".equals(ncp.getClassName())) return i; i++;                     // 82
            if (!ncp.isRelative()) return i; i++;                                                  // 83
            if (!"juan: java.lang.String".equals(ncp.toString())) return i; i++;                   // 84

            NameClassPair abs = new NameClassPair("ldap://h/x", "java.lang.Object", false);
            if (abs.isRelative()) return i; i++;                                                   // 85
            if (!abs.toString().startsWith("(not relative)")) return i; i++;                       // 86

            // El nombre absoluto es opcional: sin ponerlo, tira en vez de devolver null.
            try {
                ncp.getNameInNamespace();
                return i;                                                                          // 87
            } catch (UnsupportedOperationException esperada) {
                i++;
            }
            ncp.setNameInNamespace("ou=gente,o=acme");
            if (!"ou=gente,o=acme".equals(ncp.getNameInNamespace())) return i; i++;                // 88

            // Binding deduce el nombre de clase del objeto cuando no se lo declararon.
            Binding b = new Binding("x", "hola");
            if (!"java.lang.String".equals(b.getClassName())) return i; i++;                       // 89
            if (!"hola".equals(b.getObject())) return i; i++;                                      // 90
            // Pero el declarado gana sobre el deducido.
            Binding b2 = new Binding("x", "com.ejemplo.Falso", "hola");
            if (!"com.ejemplo.Falso".equals(b2.getClassName())) return i; i++;                     // 91
            // Y sin ninguna de las dos cosas es null, no una excepcion.
            if (new Binding("x", null).getClassName() != null) return i; i++;                      // 92
            if (!"x: java.lang.String:hola".equals(b.toString())) return i; i++;                   // 93

            // ---- RefAddr y sus dos formas ---------------------------------------------------------------

            StringRefAddr sa = new StringRefAddr("URL", "ldap://h/");
            if (!"URL".equals(sa.getType())) return i; i++;                                        // 94
            if (!"ldap://h/".equals(sa.getContent())) return i; i++;                               // 95
            if (!sa.equals(new StringRefAddr("URL", "ldap://h/"))) return i; i++;                  // 96
            if (sa.equals(new StringRefAddr("OTRO", "ldap://h/"))) return i; i++;                  // 97
            if (sa.hashCode() != new StringRefAddr("URL", "ldap://h/").hashCode()) return i; i++;  // 98
            // Contenido nulo: el hash es el del tipo y no explota.
            if (new StringRefAddr("URL", null).hashCode() != "URL".hashCode()) return i; i++;      // 99
            if (new StringRefAddr("URL", null).equals(sa)) return i; i++;                          // 100

            // El binario compara byte a byte, no por identidad del arreglo.
            byte[] bytes = {1, 2, 3};
            BinaryRefAddr ba = new BinaryRefAddr("bin", bytes);
            if (!ba.equals(new BinaryRefAddr("bin", new byte[] {1, 2, 3}))) return i; i++;         // 101
            if (ba.equals(new BinaryRefAddr("bin", new byte[] {1, 2}))) return i; i++;             // 102
            if (ba.hashCode() != new BinaryRefAddr("bin", new byte[] {1, 2, 3}).hashCode()) return i; i++; // 103
            // Y el constructor copia: cambiar el arreglo del que llamo no cambia la direccion.
            bytes[0] = 9;
            if (!ba.equals(new BinaryRefAddr("bin", new byte[] {1, 2, 3}))) return i; i++;         // 104
            // El de rango copia solo el tramo pedido.
            BinaryRefAddr br = new BinaryRefAddr("bin", new byte[] {0, 1, 2, 3, 4}, 1, 3);
            if (!br.equals(new BinaryRefAddr("bin", new byte[] {1, 2, 3}))) return i; i++;         // 105
            // Un binario y un texto nunca son iguales aunque compartan tipo.
            if (ba.equals(new StringRefAddr("bin", "123"))) return i; i++;                         // 106

            // ---- Reference ------------------------------------------------------------------------------

            Reference r = new Reference("com.ejemplo.Ds", "com.ejemplo.DsFactory", null);
            if (!"com.ejemplo.Ds".equals(r.getClassName())) return i; i++;                         // 107
            if (!"com.ejemplo.DsFactory".equals(r.getFactoryClassName())) return i; i++;           // 108
            if (r.getFactoryClassLocation() != null) return i; i++;                                // 109
            if (r.size() != 0) return i; i++;                                                      // 110

            r.add(new StringRefAddr("URL", "uno"));
            r.add(new StringRefAddr("URL", "dos"));
            r.add(new StringRefAddr("user", "juan"));
            if (r.size() != 3) return i; i++;                                                      // 111
            // get(String) devuelve la PRIMERA de ese tipo: el orden es la preferencia.
            if (!"uno".equals(r.get("URL").getContent())) return i; i++;                           // 112
            if (!"juan".equals(r.get("user").getContent())) return i; i++;                         // 113
            if (r.get("nada") != null) return i; i++;                                              // 114
            if (!"dos".equals(r.get(1).getContent())) return i; i++;                               // 115

            r.add(0, new StringRefAddr("URL", "cero"));
            if (!"cero".equals(r.get("URL").getContent())) return i; i++;                          // 116
            Object quitada = r.remove(0);
            if (!(quitada instanceof RefAddr)) return i; i++;                                      // 117
            if (!"uno".equals(r.get("URL").getContent())) return i; i++;                           // 118

            // La igualdad mira clase y direcciones EN ORDEN, y a proposito ignora la fabrica.
            Reference r2 = new Reference("com.ejemplo.Ds", "OTRA.Fabrica", "http://x/");
            r2.add(new StringRefAddr("URL", "uno"));
            r2.add(new StringRefAddr("URL", "dos"));
            r2.add(new StringRefAddr("user", "juan"));
            if (!r.equals(r2)) return i; i++;                                                      // 119
            if (r.hashCode() != r2.hashCode()) return i; i++;                                      // 120
            // Pero el orden si cuenta.
            Reference r3 = new Reference("com.ejemplo.Ds");
            r3.add(new StringRefAddr("URL", "dos"));
            r3.add(new StringRefAddr("URL", "uno"));
            r3.add(new StringRefAddr("user", "juan"));
            if (r.equals(r3)) return i; i++;                                                       // 121
            // Y la clase tambien.
            if (r.equals(new Reference("otra.Clase"))) return i; i++;                              // 122

            // El clon tiene lista propia: agregarle una direccion no toca al original.
            Reference rc = (Reference) r.clone();
            if (!rc.equals(r)) return i; i++;                                                      // 123
            rc.add(new StringRefAddr("extra", "x"));
            if (r.size() != 3) return i; i++;                                                      // 124
            if (rc.size() != 4) return i; i++;                                                     // 125
            // Y conserva la fabrica, que equals no mira pero clone si copia.
            if (!"com.ejemplo.DsFactory".equals(rc.getFactoryClassName())) return i; i++;          // 126

            r.clear();
            if (r.size() != 0) return i; i++;                                                      // 127

            // ---- LinkRef --------------------------------------------------------------------------------

            LinkRef lr = new LinkRef("a/b");
            if (!"a/b".equals(lr.getLinkName())) return i; i++;                                    // 128
            if (!"javax.naming.LinkRef".equals(lr.getClassName())) return i; i++;                  // 129
            if (lr.size() != 1) return i; i++;                                                     // 130
            if (!"LinkAddress".equals(lr.get(0).getType())) return i; i++;                         // 131
            // El constructor de Name usa la forma de cadena del nombre.
            if (!"a/b".equals(new LinkRef(new CompositeName("a/b")).getLinkName())) return i; i++; // 132
            // Si le sacan la direccion deja de ser un enlace, y lo dice en vez de tirar NPE.
            LinkRef roto = new LinkRef("a/b");
            roto.clear();
            try {
                roto.getLinkName();
                return i;                                                                          // 133
            } catch (NamingException esperada) {
                i++;
            }

            // ---- NamingException: el estado que acumula ---------------------------------------------------

            NamingException ne = new NamingException("fallo");
            if (!"fallo".equals(ne.getExplanation())) return i; i++;                               // 134
            if (ne.getResolvedName() != null) return i; i++;                                       // 135

            // Los setters de nombre CLONAN: el nombre es mutable y la excepcion ya viajo.
            CompositeName resuelto = new CompositeName("a/b");
            ne.setResolvedName(resuelto);
            resuelto.add("c");
            if (ne.getResolvedName().size() != 2) return i; i++;                                   // 136

            // appendRemainingComponent va acumulando mientras la excepcion sube.
            NamingException ne2 = new NamingException("x");
            ne2.appendRemainingComponent("c");
            ne2.appendRemainingComponent("d");
            if (!mismo(ne2.getRemainingName(), new String[] {"c", "d"})) return i; i++;            // 137
            ne2.appendRemainingName(new CompositeName("e/f"));
            if (!mismo(ne2.getRemainingName(), new String[] {"c", "d", "e", "f"})) return i; i++;  // 138

            // getCause y getRootCause son dos nombres de lo mismo.
            NamingException ne3 = new NamingException("y");
            Exception causa = new IllegalStateException("causa");
            ne3.setRootCause(causa);
            if (ne3.getRootCause() != causa) return i; i++;                                        // 139
            if (ne3.getCause() != causa) return i; i++;                                            // 140
            // Pero la asimetria es real: setRootCause no toca la causa de Throwable.
            NamingException ne4 = new NamingException("z");
            ne4.initCause(causa);
            if (ne4.getRootCause() != causa) return i; i++;                                        // 141
            // Y una causada por si misma no se enlaza, para que imprimirla no cuelgue.
            NamingException ne5 = new NamingException("w");
            ne5.setRootCause(ne5);
            if (ne5.getRootCause() != null) return i; i++;                                         // 142

            // ---- La jerarquia de excepciones, que es la mitad del paquete ---------------------------------

            if (!(new NoInitialContextException() instanceof NamingException)) return i; i++;      // 143
            if (!(new javax.naming.NameNotFoundException() instanceof NamingException)) return i; i++; // 144
            if (!(new javax.naming.NoPermissionException()
                    instanceof javax.naming.NamingSecurityException)) return i; i++;               // 145
            if (!(new javax.naming.SizeLimitExceededException()
                    instanceof javax.naming.LimitExceededException)) return i; i++;                // 146
            if (!(new javax.naming.MalformedLinkException()
                    instanceof javax.naming.LinkException)) return i; i++;                         // 147
            if (!(new javax.naming.CannotProceedException() instanceof NamingException)) return i; i++; // 148

            // LinkException lleva su propio par de nombres, aparte de los del contexto.
            javax.naming.LinkException le = new javax.naming.LinkException("link");
            le.setLinkResolvedName(new CompositeName("a"));
            le.setLinkRemainingName(new CompositeName("b/c"));
            if (!mismo(le.getLinkResolvedName(), new String[] {"a"})) return i; i++;               // 149
            if (!mismo(le.getLinkRemainingName(), new String[] {"b", "c"})) return i; i++;         // 150
            if (le.getResolvedName() != null) return i; i++;                                       // 151

            // ---- InitialContext sin proveedor ------------------------------------------------------------
            //
            // Sin `java.naming.factory.initial` puesto, TODA operacion falla con
            // NoInitialContextException. No es un agujero de esta implementacion: es lo que hace el
            // JDK real, y esta declarado en la firma.

            InitialContext ic = new InitialContext();
            try {
                ic.lookup("cualquiera");
                return i;                                                                          // 152
            } catch (NoInitialContextException esperada) {
                i++;
            }
            try {
                ic.bind("x", "y");
                return i;                                                                          // 153
            } catch (NoInitialContextException esperada) {
                i++;
            }
            try {
                ic.list(new CompositeName("x"));
                return i;                                                                          // 154
            } catch (NoInitialContextException esperada) {
                i++;
            }
            try {
                ic.getEnvironment();
                return i;                                                                          // 155
            } catch (NoInitialContextException esperada) {
                i++;
            }
            try {
                InitialContext.doLookup("x");
                return i;                                                                          // 156
            } catch (NoInitialContextException esperada) {
                i++;
            }

            // composeName si anda, porque no necesita proveedor: el contexto inicial es el origen.
            if (!"a/b".equals(ic.composeName("a/b", ""))) return i; i++;                           // 157
            Name compuesto = ic.composeName(new CompositeName("a/b"), new CompositeName(""));
            if (!mismo(compuesto, new String[] {"a", "b"})) return i; i++;                         // 158

            // Cerrar sin proveedor no falla, y cerrar dos veces tampoco.
            ic.close();
            ic.close();
            i++;                                                                                   // 159

            // Nombrar una fabrica que no existe falla en el constructor, no tres llamadas despues.
            java.util.Hashtable<Object, Object> env = new java.util.Hashtable<Object, Object>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "no.existe.Fabrica");
            try {
                new InitialContext(env);
                return i;                                                                          // 160
            } catch (NamingException esperada) {
                i++;
            }

            // Las constantes de Context son las claves reales del entorno.
            if (!"java.naming.factory.initial".equals(Context.INITIAL_CONTEXT_FACTORY)) return i; i++; // 161
            if (!"java.naming.provider.url".equals(Context.PROVIDER_URL)) return i; i++;           // 162

            return -1;
        } catch (NamingException ex) {
            return i;
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
