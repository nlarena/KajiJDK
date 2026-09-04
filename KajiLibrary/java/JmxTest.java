import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.BadAttributeValueExpException;
import javax.management.BadStringOperationException;
import javax.management.Descriptor;
import javax.management.ImmutableDescriptor;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanConstructorInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.MBeanServerNotification;
import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;
import javax.management.ReflectionException;
import javax.management.RuntimeErrorException;
import javax.management.RuntimeMBeanException;
import javax.management.RuntimeOperationsException;
import javax.management.ValueExp;

/**
 * Comportamiento de `javax.management`, para correr en las dos VMs y comparar.
 *
 * <p>`run()` devuelve -1 si todo pasa, o el indice del primer caso que fallo. Las expectativas se
 * corrieron primero contra el JDK 25 y varias salieron distintas de lo que uno supondria: la cadena
 * vacia como `ObjectName` es `*:*`, un valor citado con `*` adentro <b>si</b> es un patron, y
 * `BadAttributeValueExpException.toString()` dice `BadAttributeValueException`, sin el `Exp`.
 *
 * <p>No se comparan los mensajes de las `MalformedObjectNameException`, solo que la excepcion sea
 * esa: los textos son detalle de implementacion y compararlos ataria la prueba a una redaccion.
 */
public class JmxTest {

    private static int n;

    private static boolean ok;

    private static int fallo;

    private static void chk(boolean cond) {
        if (!ok) {
            n++;
            return;
        }
        if (!cond) {
            ok = false;
            fallo = n;
        }
        n++;
    }

    private static void eq(String a, String b) {
        chk(a == null ? b == null : a.equals(b));
    }

    /** Que la cadena no se pueda analizar como nombre. */
    private static void malo(String s) {
        boolean tiro = false;
        try {
            new ObjectName(s);
        } catch (MalformedObjectNameException e) {
            tiro = true;
        } catch (Exception e) {
            tiro = false;
        }
        chk(tiro);
    }

    private static ObjectName on(String s) {
        try {
            return new ObjectName(s);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e);
        }
    }

    public static int run() {
        n = 0;
        ok = true;
        fallo = -1;

        // ---- ObjectName: forma canonica y orden de las claves --------------------------------
        eq(on("d:k=v").getCanonicalName(), "d:k=v");                                     // 0
        eq(on("d:b=2,a=1").getCanonicalName(), "d:a=1,b=2");                             // 1
        eq(on("d:b=2,a=1").toString(), "d:b=2,a=1");                                     // 2
        eq(on("d:b=2,a=1").getKeyPropertyListString(), "b=2,a=1");                       // 3
        eq(on("d:b=2,a=1").getCanonicalKeyPropertyListString(), "a=1,b=2");              // 4
        // Orden por unidad de codigo: las mayusculas van antes.
        eq(on("d:B=2,a=1,C=3").getCanonicalName(), "d:B=2,C=3,a=1");                     // 5
        eq(on("d:aa=1,a=2").getCanonicalName(), "d:a=2,aa=1");                           // 6
        eq(on("d:k=").getCanonicalName(), "d:k=");                                       // 7
        eq(on(":k=v").getDomain(), "");                                                  // 8

        // ---- la cadena vacia es el comodin ---------------------------------------------------
        eq(on("").getCanonicalName(), "*:*");                                            // 9
        eq(on("").getDomain(), "*");                                                     // 10
        chk(on("").isPattern() && on("").isDomainPattern());                             // 11
        chk(on("").isPropertyListPattern());                                             // 12
        eq(ObjectName.WILDCARD.getCanonicalName(), "*:*");                               // 13

        // ---- las tres clases de comodin ------------------------------------------------------
        chk(on("*:k=v").isDomainPattern() && !on("*:k=v").isPropertyListPattern());       // 14
        chk(on("d*n:k=v").isDomainPattern());                                            // 15
        chk(on("d?n:k=v").isDomainPattern());                                            // 16
        chk(on("d:*").isPropertyListPattern() && !on("d:*").isDomainPattern());          // 17
        chk(on("d:k=*").isPropertyValuePattern());                                       // 18
        chk(!on("d:k=*").isPropertyListPattern());                                       // 19
        chk(on("d:k=a*b").isPropertyValuePattern());                                     // 20
        chk(on("d:k=?").isPropertyValuePattern());                                       // 21
        chk(!on("d:k=v").isPattern());                                                   // 22
        // El `*` de lista se va al final, este donde este.
        eq(on("d:*,k=v").getCanonicalName(), "d:k=v,*");                                 // 23
        eq(on("d:*,k=v").toString(), "d:k=v,*");                                         // 24
        eq(on("d:k=v,*,l=w").getCanonicalName(), "d:k=v,l=w,*");                          // 25
        eq(on("d:*").getKeyPropertyListString(), "");                                    // 26
        chk(on("d:k=*,l=v").isPropertyValuePattern("k"));                                // 27
        chk(!on("d:k=*,l=v").isPropertyValuePattern("l"));                               // 28
        boolean iae = false;
        try {
            on("d:k=v").isPropertyValuePattern("z");
        } catch (IllegalArgumentException e) {
            iae = true;
        }
        chk(iae);                                                                        // 29

        // ---- citar no apaga el comodin; la barra si ------------------------------------------
        chk(on("d:k=\"a*b\"").isPropertyValuePattern());                                 // 30
        chk(!on("d:k=\"a\\*b\"").isPropertyValuePattern());                              // 31
        eq(on("d:k=\"a,b\"").getKeyProperty("k"), "\"a,b\"");                            // 32
        eq(on("d:k=\"\"").getCanonicalName(), "d:k=\"\"");                               // 33

        // ---- nombres invalidos ---------------------------------------------------------------
        malo("d");                                                                       // 34
        malo("d:");                                                                      // 35
        malo("*");                                                                       // 36
        malo("d:=v");                                                                    // 37
        malo("d:k=v,k=w");                                                               // 38
        malo("d:k=v,");                                                                  // 39
        malo("d:,k=v");                                                                  // 40
        malo("d:k=v:w");                                                                 // 41
        malo("d\n:k=v");                                                                 // 42
        malo("d:k");                                                                     // 43
        malo("d:k=a\"b");                                                                // 44
        malo("d:k=\"a");                                                                 // 45
        malo("d:k=\"a\\zb\"");                                                           // 46
        malo("d:k=\"a\nb\"");                                                            // 47
        malo("d:*,*");                                                                   // 48
        malo("d:*x");                                                                    // 49
        malo("d:k*x=v");                                                                 // 50
        malo("d:k?x=v");                                                                 // 51
        malo("::k=v");                                                                   // 52

        // ---- nombres validos que uno diria que no --------------------------------------------
        eq(on("d:k=v w").getKeyProperty("k"), "v w");                                    // 53
        eq(on("d:k =v").getKeyProperty("k "), "v");                                 // 54
        eq(on("d:k\"x=v").getKeyProperty("k\"x"), "v");                                  // 55
        eq(on("d=x:k=v").getDomain(), "d=x");                                            // 56
        eq(on("d,x:k=v").getDomain(), "d,x");                                            // 57
        eq(on("d:k=v,*,").getCanonicalName(), "d:k=v,*");                                // 58

        // ---- identidad -----------------------------------------------------------------------
        chk(on("d:b=2,a=1").equals(on("d:a=1,b=2")));                                    // 59
        chk(on("d:b=2,a=1").hashCode() == on("d:a=1,b=2").hashCode());                   // 60
        chk(on("d:k=v").hashCode() == "d:k=v".hashCode());                               // 61
        chk(!on("d:k=v").equals("d:k=v"));                                               // 62
        chk(!on("d:k=v").equals(null));                                                  // 63

        // ---- apply ---------------------------------------------------------------------------
        chk(on("*:*").apply(on("d:k=v")));                                               // 64
        chk(on("d:*").apply(on("d:k=v")));                                               // 65
        chk(!on("d:*").apply(on("e:k=v")));                                              // 66
        chk(on("d:k=v").apply(on("d:k=v")));                                             // 67
        chk(!on("d:k=v").apply(on("d:k=v,l=w")));                                        // 68
        chk(on("d:k=v,*").apply(on("d:k=v,l=w")));                                       // 69
        chk(on("d:k=*").apply(on("d:k=abc")));                                           // 70
        chk(on("d:k=a*").apply(on("d:k=abc")));                                          // 71
        chk(on("d:k=a?c").apply(on("d:k=abc")));                                         // 72
        chk(!on("d:k=a?c").apply(on("d:k=abbc")));                                       // 73
        chk(on("d*:k=v").apply(on("d1:k=v")));                                           // 74
        chk(on("*d:k=v").apply(on("xd:k=v")));                                           // 75
        chk(on("?:k=v").apply(on("d:k=v")));                                             // 76
        // Un patron nunca designa a otro patron.
        chk(!on("d:k=v").apply(on("d:*")));                                              // 77
        chk(!on("*:*").apply(on("*:*")));                                                // 78
        chk(on("d:k=\"a*b\"").apply(on("d:k=\"axxb\"")));                                // 79

        // ---- quote / unquote -----------------------------------------------------------------
        eq(ObjectName.quote("a"), "\"a\"");                                              // 80
        eq(ObjectName.quote("a\"b"), "\"a\\\"b\"");                                      // 81
        eq(ObjectName.quote("a\\b"), "\"a\\\\b\"");                                      // 82
        eq(ObjectName.quote("a\nb"), "\"a\\nb\"");                                       // 83
        eq(ObjectName.quote("a*b"), "\"a\\*b\"");                                        // 84
        eq(ObjectName.quote("a?b"), "\"a\\?b\"");                                        // 85
        eq(ObjectName.quote("a\tb"), "\"a\tb\"");                                        // 86
        eq(ObjectName.quote(""), "\"\"");                                                // 87
        eq(ObjectName.unquote("\"a\""), "a");                                            // 88
        eq(ObjectName.unquote("\"a\\\"b\""), "a\"b");                                    // 89
        eq(ObjectName.unquote("\"a\\nb\""), "a\nb");                                     // 90
        eq(ObjectName.unquote("\"a\\\\b\""), "a\\b");                                    // 91
        eq(ObjectName.unquote("\"a\\*b\""), "a*b");                                      // 92
        eq(ObjectName.unquote("\"\""), "");                                              // 93
        // unquote es mas estricta que el analizador: aca el `*` sin escapar es error.
        chk(unquoteMalo("a"));                                                           // 94
        chk(unquoteMalo("\"a"));                                                         // 95
        chk(unquoteMalo("\"a\\qb\""));                                                   // 96
        chk(unquoteMalo("\"a*b\""));                                                     // 97
        // Lo que sale de quote se puede volver a leer y da lo mismo.
        eq(ObjectName.unquote(ObjectName.quote("a*b?c\"d\\e")), "a*b?c\"d\\e");          // 98

        // ---- constructores partidos ----------------------------------------------------------
        ObjectName tres = null;
        try {
            tres = new ObjectName("d", "k", "v");
        } catch (MalformedObjectNameException e) {
            chk(false);
        }
        eq(tres == null ? null : tres.getCanonicalName(), "d:k=v");                      // 99
        chk(patronDeValor("d", "k", "*"));                                               // 100
        chk(!patronDeValor("d", "k", "v"));                                              // 101
        chk(dominioMalo("d:x"));                                                         // 102
        chk(claveMala("d", "k*"));                                                       // 103

        // ---- orden ---------------------------------------------------------------------------
        chk(signo(on("a:k=v").compareTo(on("b:k=v"))) < 0);                              // 104
        chk(on("d:k=v").compareTo(on("d:k=v")) == 0);                                    // 105
        chk(signo(on("d:a=1").compareTo(on("d:b=1"))) < 0);                              // 106
        // La clave `type` pesa mas que el resto de la forma canonica.
        chk(signo(on("d:type=b,a=9").compareTo(on("d:type=a,a=1"))) > 0);                // 107

        // ---- excepciones ---------------------------------------------------------------------
        Exception env = new IllegalStateException("x");
        MBeanException me = new MBeanException(env, "m");
        chk(me.getTargetException() == env && me.getCause() == env);                     // 108
        eq(me.getMessage(), "m");                                                        // 109
        ReflectionException re = new ReflectionException(env);
        chk(re.getTargetException() == env && re.getCause() == env);                     // 110
        RuntimeException rte = new IllegalArgumentException("y");
        RuntimeMBeanException rme = new RuntimeMBeanException(rte);
        chk(rme.getTargetException() == rte && rme.getCause() == rte);                   // 111
        RuntimeOperationsException roe = new RuntimeOperationsException(rte, "z");
        chk(roe.getTargetException() == rte && roe.getCause() == rte);                   // 112
        Error err = new StackOverflowError();
        RuntimeErrorException ree = new RuntimeErrorException(err);
        chk(ree.getTargetError() == err && ree.getCause() == err);                       // 113
        chk(new AttributeNotFoundException("a") instanceof JMException);                 // 114
        // Los dos arboles no se tocan: el no verificado no es un JMException.
        chk(!JMException.class.isInstance(rme));                                         // 115
        // Rareza del JDK: dice BadAttributeValueException, sin el `Exp`.
        eq(new BadAttributeValueExpException("xy").toString(),
           "BadAttributeValueException: xy");                                            // 116
        eq(new BadAttributeValueExpException(null).toString(),
           "BadAttributeValueException: null");                                          // 117
        eq(new BadStringOperationException("op").toString(),
           "BadStringOperationException: op");                                           // 118

        // ---- Attribute / ObjectInstance ------------------------------------------------------
        Attribute at = new Attribute("n", "v");
        eq(at.toString(), "n = v");                                                      // 119
        chk(at.hashCode() == ("n".hashCode() ^ "v".hashCode()));                         // 120
        chk(at.equals(new Attribute("n", "v")));                                         // 121
        chk(!at.equals(new Attribute("n", null)));                                       // 122
        chk(new Attribute("n", null).hashCode() == "n".hashCode());                      // 123
        chk(nombreNulo());                                                               // 124
        ObjectInstance oi = new ObjectInstance(on("d:k=v"), "C");
        eq(oi.toString(), "C[d:k=v]");                                                   // 125
        chk(oi.hashCode() == (on("d:k=v").hashCode() ^ "C".hashCode()));                 // 126
        chk(instanciaConPatron());                                                       // 127

        // ---- descriptores --------------------------------------------------------------------
        ImmutableDescriptor d1 = new ImmutableDescriptor(new String[] {"b", "a"},
                                                         new Object[] {"2", "1"});
        eq(d1.toString(), "{a=1, b=2}");                                                 // 128
        eq(unir(d1.getFieldNames()), "a|b");                                             // 129
        eq(unir(d1.getFields()), "a=1|b=2");                                             // 130
        eq(String.valueOf(d1.getFieldValue("a")), "1");                                  // 131
        // Los nombres no distinguen mayusculas.
        eq(String.valueOf(d1.getFieldValue("A")), "1");                                  // 132
        chk(d1.getFieldValue("zz") == null);                                             // 133
        chk(d1.equals(new ImmutableDescriptor(new String[] {"a", "b"},
                                              new Object[] {"1", "2"})));                // 134
        chk(d1.isValid() && d1.clone() == d1);                                           // 135
        eq(new ImmutableDescriptor("x=1", "y=2").toString(), "{x=1, y=2}");              // 136
        eq(ImmutableDescriptor.EMPTY_DESCRIPTOR.toString(), "{}");                       // 137
        eq(ImmutableDescriptor.union(d1, new ImmutableDescriptor("c=3")).toString(),
           "{a=1, b=2, c=3}");                                                           // 138
        chk(descriptorInmutable(d1));                                                    // 139
        Object[] vs = d1.getFieldValues("a", "zz");
        chk(vs.length == 2 && "1".equals(vs[0]) && vs[1] == null);                       // 140

        // ---- MBean*Info ----------------------------------------------------------------------
        MBeanParameterInfo p = new MBeanParameterInfo("p", "java.lang.String", "desc");
        eq(p.toString(), "javax.management.MBeanParameterInfo[description=desc, name=p, "
                + "type=java.lang.String, descriptor={}]");                              // 141
        MBeanAttributeInfo ai = new MBeanAttributeInfo("A", "int", "da", true, false, false);
        eq(ai.toString(), "javax.management.MBeanAttributeInfo[description=da, name=A, "
                + "type=int, read-only, descriptor={}]");                                // 142
        MBeanAttributeInfo ai2 = new MBeanAttributeInfo("B", "boolean", "db", true, true, true);
        eq(ai2.toString(), "javax.management.MBeanAttributeInfo[description=db, name=B, "
                + "type=boolean, read/write, isIs, descriptor={}]");                     // 143
        eq(new MBeanAttributeInfo("C", "int", "dc", false, true, false).toString(),
           "javax.management.MBeanAttributeInfo[description=dc, name=C, type=int, "
                + "write-only, descriptor={}]");                                         // 144
        eq(new MBeanAttributeInfo("D", "int", "dd", false, false, false).toString(),
           "javax.management.MBeanAttributeInfo[description=dd, name=D, type=int, "
                + "no-access, descriptor={}]");                                          // 145
        MBeanOperationInfo op = new MBeanOperationInfo("op", "dop",
                new MBeanParameterInfo[] {p}, "void", MBeanOperationInfo.ACTION);
        chk(op.toString().indexOf("impact=action") > 0);                                 // 146
        chk(new MBeanOperationInfo("o", "d", null, "void", MBeanOperationInfo.INFO)
                .toString().indexOf("impact=info") > 0);                                 // 147
        chk(new MBeanOperationInfo("o", "d", null, "void", MBeanOperationInfo.ACTION_INFO)
                .toString().indexOf("impact=action/info") > 0);                          // 148
        chk(new MBeanOperationInfo("o", "d", null, "void", MBeanOperationInfo.UNKNOWN)
                .toString().indexOf("impact=unknown") > 0);                              // 149
        chk(op.getSignature().length == 1 && op.getSignature() != op.getSignature());    // 150
        MBeanConstructorInfo ci = new MBeanConstructorInfo("ci", "dci",
                new MBeanParameterInfo[] {p});
        chk(ci.toString().indexOf("signature=[javax.management.MBeanParameterInfo") > 0); // 151
        MBeanNotificationInfo ni = new MBeanNotificationInfo(new String[] {"t1", "t2"},
                "javax.management.Notification", "dni");
        eq(ni.toString(), "javax.management.MBeanNotificationInfo[description=dni, "
                + "name=javax.management.Notification, notifTypes=[t1, t2], descriptor={}]"); // 152
        MBeanInfo mi = new MBeanInfo("com.C", "dmi", new MBeanAttributeInfo[] {ai},
                new MBeanConstructorInfo[] {ci}, new MBeanOperationInfo[] {op},
                new MBeanNotificationInfo[] {ni});
        eq(mi.getClassName(), "com.C");                                                  // 153
        chk(mi.getAttributes().length == 1 && mi.getOperations().length == 1);           // 154
        // Un arreglo nulo se guarda vacio, no nulo.
        MBeanInfo vacio = new MBeanInfo("com.C", "d", null, null, null, null);
        chk(vacio.getAttributes().length == 0 && vacio.getNotifications().length == 0);  // 155
        eq(mi.getDescriptor().toString(), "{}");                                         // 156
        chk(mi.equals(new MBeanInfo("com.C", "dmi", new MBeanAttributeInfo[] {ai},
                new MBeanConstructorInfo[] {ci}, new MBeanOperationInfo[] {op},
                new MBeanNotificationInfo[] {ni})));                                     // 157
        chk(ai.equals(new MBeanAttributeInfo("A", "int", "da", true, false, false)));    // 158
        chk(!ai.equals(ai2));                                                            // 159
        chk(p.clone() == p);                                                             // 160

        // ---- AttributeList -------------------------------------------------------------------
        AttributeList al = new AttributeList();
        al.add(at);
        al.add(new Attribute("m", Integer.valueOf(1)));
        eq(al.toString(), "[n = v, m = 1]");                                             // 161
        chk(al.size() == 2 && al.asList().size() == 2);                                  // 162
        chk(listaContaminada());                                                         // 163

        // ---- notificaciones ------------------------------------------------------------------
        Notification nt = new Notification("t", "src", 7L, 11L, "msg");
        eq(nt.toString(), "javax.management.Notification[source=src][type=t][message=msg]"); // 164
        chk(nt.getSequenceNumber() == 7L && nt.getTimeStamp() == 11L);                   // 165
        eq(String.valueOf(nt.getSource()), "src");                                       // 166
        chk(nt.getUserData() == null);                                                   // 167
        eq(new Notification("t", "s", 1L).getMessage(), "");                             // 168
        nt.setSource("otra");
        eq(String.valueOf(nt.getSource()), "otra");                                      // 169
        AttributeChangeNotification acn = new AttributeChangeNotification("s", 1L, 2L, "m",
                "An", "int", "o", "n");
        eq(acn.getType(), "jmx.attribute.change");                                       // 170
        eq(AttributeChangeNotification.ATTRIBUTE_CHANGE, "jmx.attribute.change");        // 171
        eq(acn.getAttributeName() + "/" + acn.getAttributeType() + "/"
                + acn.getOldValue() + "/" + acn.getNewValue(), "An/int/o/n");            // 172
        eq(MBeanServerNotification.REGISTRATION_NOTIFICATION, "JMX.mbean.registered");   // 173
        eq(MBeanServerNotification.UNREGISTRATION_NOTIFICATION,
           "JMX.mbean.unregistered");                                                    // 174
        MBeanServerNotification msn = new MBeanServerNotification(
                MBeanServerNotification.REGISTRATION_NOTIFICATION, "s", 1L, on("d:k=v"));
        eq(msn.toString(), "javax.management.MBeanServerNotification[source=s]"
                + "[type=JMX.mbean.registered][message=][mbeanName=d:k=v]");             // 175
        eq(msn.getMBeanName().getCanonicalName(), "d:k=v");                              // 176

        // ---- Query: la forma textual ---------------------------------------------------------
        eq(Query.value(3).toString(), "3");                                              // 177
        eq(Query.value(3L).toString(), "3");                                             // 178
        eq(Query.value(3.5).toString(), "3.5");                                          // 179
        eq(Query.value(true).toString(), "true");                                        // 180
        eq(Query.value("s").toString(), "'s'");                                          // 181
        eq(Query.value("a'b").toString(), "'a''b'");                                     // 182
        eq(Query.attr("At").toString(), "At");                                           // 183
        eq(Query.attr("Cl", "At").toString(), "Cl.At");                                  // 184
        eq(Query.classattr().toString(), "Class");                                       // 185
        eq(Query.eq(Query.attr("a"), Query.value(1)).toString(), "(a) = (1)");           // 186
        eq(Query.gt(Query.attr("a"), Query.value(1)).toString(), "(a) > (1)");           // 187
        eq(Query.lt(Query.attr("a"), Query.value(1)).toString(), "(a) < (1)");           // 188
        eq(Query.geq(Query.attr("a"), Query.value(1)).toString(), "(a) >= (1)");         // 189
        eq(Query.leq(Query.attr("a"), Query.value(1)).toString(), "(a) <= (1)");         // 190
        eq(Query.and(Query.eq(Query.attr("a"), Query.value(1)),
                     Query.gt(Query.attr("b"), Query.value(2))).toString(),
           "((a) = (1)) and ((b) > (2))");                                               // 191
        eq(Query.or(Query.eq(Query.attr("a"), Query.value(1)),
                    Query.lt(Query.attr("b"), Query.value(2))).toString(),
           "((a) = (1)) or ((b) < (2))");                                                // 192
        eq(Query.not(Query.eq(Query.attr("a"), Query.value(1))).toString(),
           "not ((a) = (1))");                                                           // 193
        eq(Query.between(Query.attr("a"), Query.value(1), Query.value(2)).toString(),
           "(a) between (1) and (2)");                                                   // 194
        eq(Query.match(Query.attr("a"), Query.value("f*o?")).toString(), "a like 'f*o?'"); // 195
        eq(Query.in(Query.attr("a"),
                    new ValueExp[] {Query.value(1), Query.value(2)}).toString(),
           "a in (1, 2)");                                                               // 196
        eq(Query.isInstanceOf(Query.value("java.lang.String")).toString(),
           "InstanceOf 'java.lang.String'");                                             // 197
        eq(Query.plus(Query.value(1), Query.value(2)).toString(), "1 + 2");              // 198
        eq(Query.minus(Query.value(1), Query.value(2)).toString(), "1 - 2");             // 199
        eq(Query.times(Query.value(3), Query.value(2)).toString(), "3 * 2");             // 200
        eq(Query.div(Query.value(1), Query.value(2)).toString(), "1 / 2");               // 201
        // Los tres atajos escapan el texto antes de pegarle la estrella.
        eq(Query.initialSubString(Query.attr("a"), Query.value("pre")).toString(),
           "a like 'pre*'");                                                             // 202
        eq(Query.anySubString(Query.attr("a"), Query.value("mid")).toString(),
           "a like '*mid*'");                                                            // 203
        eq(Query.finalSubString(Query.attr("a"), Query.value("suf")).toString(),
           "a like '*suf'");                                                             // 204
        eq(Query.initialSubString(Query.attr("a"), Query.value("a*b")).toString(),
           "a like 'a\\*b*'");                                                           // 205
        // Los parentesis solo donde hacen falta.
        eq(Query.plus(Query.value(1), Query.times(Query.value(2), Query.value(3)))
                .toString(), "1 + 2 * 3");                                               // 206
        eq(Query.times(Query.value(1), Query.plus(Query.value(2), Query.value(3)))
                .toString(), "1 * (2 + 3)");                                             // 207
        eq(Query.minus(Query.value(1), Query.minus(Query.value(2), Query.value(3)))
                .toString(), "1 - (2 - 3)");                                             // 208
        eq(Query.minus(Query.minus(Query.value(1), Query.value(2)), Query.value(3))
                .toString(), "1 - 2 - 3");                                               // 209
        eq(Query.EQ + "/" + Query.GT + "/" + Query.LT + "/" + Query.GE + "/" + Query.LE,
           "4/0/1/2/3");                                                                 // 210
        eq(Query.PLUS + "/" + Query.MINUS + "/" + Query.TIMES + "/" + Query.DIV,
           "0/1/2/3");                                                                   // 211

        // ---- Query: evaluar sin agente -------------------------------------------------------
        // Con constantes de los dos lados la consulta se resuelve sola.
        ObjectName cualquiera = on("d:k=v");
        chk(aplica(Query.eq(Query.value(1), Query.value(1)), cualquiera));               // 212
        chk(!aplica(Query.eq(Query.value(1), Query.value(2)), cualquiera));              // 213
        chk(aplica(Query.gt(Query.value(2), Query.value(1)), cualquiera));               // 214
        chk(aplica(Query.leq(Query.value(1), Query.value(1)), cualquiera));              // 215
        // 1 y 1.0 comparan iguales pese a ser de tipos distintos.
        chk(aplica(Query.eq(Query.value(1), Query.value(1.0)), cualquiera));             // 216
        chk(aplica(Query.eq(Query.value("a"), Query.value("a")), cualquiera));           // 217
        chk(aplica(Query.lt(Query.value("a"), Query.value("b")), cualquiera));           // 218
        chk(aplica(Query.eq(Query.value(true), Query.value(true)), cualquiera));         // 219
        chk(aplica(Query.between(Query.value(2), Query.value(1), Query.value(3)),
                   cualquiera));                                                         // 220
        chk(!aplica(Query.between(Query.value(4), Query.value(1), Query.value(3)),
                    cualquiera));                                                        // 221
        // Extremos incluidos.
        chk(aplica(Query.between(Query.value(1), Query.value(1), Query.value(3)),
                   cualquiera));                                                         // 222
        chk(aplica(Query.in(Query.value(2),
                   new ValueExp[] {Query.value(1), Query.value(2)}), cualquiera));       // 223
        chk(!aplica(Query.in(Query.value(9),
                    new ValueExp[] {Query.value(1), Query.value(2)}), cualquiera));      // 224
        chk(aplica(Query.and(Query.eq(Query.value(1), Query.value(1)),
                             Query.eq(Query.value(2), Query.value(2))), cualquiera));    // 225
        chk(!aplica(Query.and(Query.eq(Query.value(1), Query.value(1)),
                              Query.eq(Query.value(2), Query.value(3))), cualquiera));   // 226
        chk(aplica(Query.or(Query.eq(Query.value(1), Query.value(9)),
                            Query.eq(Query.value(2), Query.value(2))), cualquiera));     // 227
        chk(aplica(Query.not(Query.eq(Query.value(1), Query.value(9))), cualquiera));    // 228
        // La aritmetica entera se queda entera.
        eq(evaluar(Query.plus(Query.value(2), Query.value(3)), cualquiera), "5");        // 229
        eq(evaluar(Query.div(Query.value(7), Query.value(2)), cualquiera), "3");         // 230
        eq(evaluar(Query.div(Query.value(7.0), Query.value(2)), cualquiera), "3.5");     // 231
        eq(evaluar(Query.plus(Query.value("a"), Query.value("b")), cualquiera), "'ab'"); // 232

        return ok ? -1 : fallo;
    }

    // ---- ayudantes -----------------------------------------------------------------------------

    private static int signo(int x) {
        return x < 0 ? -1 : (x > 0 ? 1 : 0);
    }

    private static String unir(String[] a) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < a.length; i++) {
            if (i > 0) {
                b.append('|');
            }
            b.append(a[i]);
        }
        return b.toString();
    }

    private static boolean unquoteMalo(String s) {
        try {
            ObjectName.unquote(s);
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean patronDeValor(String d, String k, String v) {
        try {
            return new ObjectName(d, k, v).isPropertyValuePattern();
        } catch (MalformedObjectNameException e) {
            return false;
        }
    }

    private static boolean dominioMalo(String d) {
        try {
            new ObjectName(d, "k", "v");
            return false;
        } catch (MalformedObjectNameException e) {
            return true;
        }
    }

    private static boolean claveMala(String d, String k) {
        try {
            new ObjectName(d, k, "v");
            return false;
        } catch (MalformedObjectNameException e) {
            return true;
        }
    }

    private static boolean nombreNulo() {
        try {
            new Attribute(null, "v");
            return false;
        } catch (RuntimeOperationsException e) {
            return e.getTargetException() instanceof IllegalArgumentException;
        }
    }

    private static boolean instanciaConPatron() {
        try {
            new ObjectInstance(new ObjectName("d:*"), "C");
            return false;
        } catch (RuntimeOperationsException e) {
            return true;
        } catch (MalformedObjectNameException e) {
            return false;
        }
    }

    private static boolean descriptorInmutable(Descriptor d) {
        try {
            d.setField("z", "1");
            return false;
        } catch (RuntimeOperationsException e) {
            return true;
        }
    }

    private static boolean listaContaminada() {
        AttributeList l = new AttributeList();
        l.add((Object) "no soy un Attribute");
        try {
            l.asList();
            return false;
        } catch (IllegalArgumentException e) {
            return true;
        }
    }

    private static boolean aplica(QueryExp q, ObjectName name) {
        try {
            return q.apply(name);
        } catch (Exception e) {
            return false;
        }
    }

    private static String evaluar(ValueExp v, ObjectName name) {
        try {
            return v.apply(name).toString();
        } catch (Exception e) {
            return "EX:" + e.getClass().getName();
        }
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
