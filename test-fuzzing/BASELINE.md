# Baseline: one pass over KajiLibrary

Produced by `test-fuzzing/fuzzer/report.py` from the campaign files in
`test-fuzzing/datos/`. Everything below is differential: the same compiled `.class`
runs on the reference JDK and on KajiJDK, and only values the specification pins
down are compared.

| | |
|---|---|
| reference | `C:/Program Files/Java/jdk-25` |
| under test | `KajiLibrary` |
| VM binary | `78addf0dc6defece` |
| operation pool | 8085 (2 queues) |
| campaigns | 13 |
| wall time | 101 min |

## What happened

| | count |
|---|---|
| cases compared | 8304 |
| agreed | 7256 |
| **diverged** | **1048 (12.6%)** |
| skipped as duplicate skeletons | 71 |
| operations the fuzzer could not reach | 109 |
| cases the reference did not answer | 15 |
| divergences that could not be attributed | 0 |

## How they diverged

| kind | count | share |
|---|---|---|
| different exception | 388 | 37% |
| exception versus value | 330 | 31% |
| different value | 235 | 22% |
| threw at a different statement | 57 | 5% |
| more specific exception | 26 | 2% |
| no output (crash or hang) | 12 | 1% |

`more specific exception` is the low-priority bucket: throwing a subclass of what
the specification promises satisfies the specification. `threw at a different
statement` means both sides failed but one got further.

## Where they are

Divergences are charged to the **statement that produced the differing value**, not
to the operation whose turn it was. The `pool` column is how many operations the
package contributes, so the two numbers can be read together; it is not a rate,
because attempts per package were not counted.

| package | divergences | pool |
|---|---|---|
| `java.time` | 388 | 1269 |
| `java.lang` | 184 | 2401 |
| `java.util` | 165 | 1534 |
| `java.nio` | 127 | 535 |
| `java.io` | 64 | 399 |
| `java.math` | 36 | 135 |
| `java.text` | 34 | 385 |
| `java.beans` | 12 | 382 |
| `sun.misc` | 11 | 92 |
| `javax.security` | 9 | 72 |
| `org.ietf` | 6 | 108 |
| `java.security` | 6 | 872 |
| `sun.reflect` | 4 | 14 |
| `javax.smartcardio` | 2 | 68 |

## Operations blamed most often

| operation | divergences |
|---|---|
| `java.time.LocalTime.of` | 14 |
| `java.time.temporal.ValueRange.of` | 11 |
| `java.time.LocalTime.now` | 11 |
| `java.lang.String.indexOf` | 10 |
| `java.util.Locale.<init>` | 8 |
| `java.util.Arrays.copyOfRange` | 8 |
| `java.time.YearMonth.parse` | 8 |
| `java.util.Locale.of` | 7 |
| `java.time.format.DateTimeFormatter.ofPattern` | 7 |
| `java.time.MonthDay.parse` | 7 |
| `java.util.ArrayList.remove` | 6 |
| `java.io.ByteArrayInputStream.readNBytes` | 6 |
| `java.lang.StringBuilder.append` | 6 |
| `java.lang.StringBuffer.<init>` | 6 |
| `java.nio.ShortBuffer.wrap` | 6 |
| `java.lang.StringBuffer.append` | 6 |
| `java.nio.ByteBuffer.put` | 6 |
| `java.nio.FloatBuffer.put` | 6 |
| `java.time.LocalDate.atTime` | 6 |
| `java.nio.DoubleBuffer.wrap` | 6 |
| `java.time.LocalDateTime.now` | 5 |
| `java.time.ZoneOffset.of` | 5 |
| `java.util.ArrayList.set` | 5 |
| `java.io.CharArrayReader.read` | 5 |
| `java.lang.constant.MethodTypeDesc.ofDescriptor` | 5 |

## A sample of what the divergences look like

**`java.time.LocalDateTime.now`** — different exception

```java
java.time.LocalDateTime v0 = java.time.LocalDateTime.now(java.time.ZoneId.of(s(0, 226))); com.sun.nio.file.ExtendedCopyOption v1 = com.sun.nio.file.ExtendedCopyOption.valueOf(s(1040, 1044, 0));
```

| | |
|---|---|
| reference | `EXC@0:java.time.DateTimeException` |
| KajiJDK | `EXC@0:java.time.zone.ZoneRulesException` |

**`java.lang.foreign.MemorySegment.isLoaded`** — exception versus value

```java
boolean v0 = java.lang.foreign.MemorySegment.ofArray(new short[] {(short) 1, (short) -1, (short) -32768, (short) 32767}).isLoaded();
```

| | |
|---|---|
| reference | `EXC@0:java.lang.UnsupportedOperationException` |
| KajiJDK | `false` |

**`java.time.Month.minus`** — different exception

```java
java.time.Month v0 = java.time.Month.of(0).minus(2147483648L); com.sun.nio.sctp.SctpServerChannel v1 = com.sun.nio.sctp.SctpServerChannel.open(); int v2 = v1.validOps();
```

| | |
|---|---|
| reference | `EXC@0:java.time.DateTimeException` |
| KajiJDK | `EXC@0:java.lang.ArrayIndexOutOfBoundsException` |

**`java.io.CharArrayReader.skip`** — exception versus value

```java
long v0 = new java.io.CharArrayReader(new char[] {(char) 9}, 64, 0).skip(-2147483649L);
```

| | |
|---|---|
| reference | `EXC@0:java.lang.IllegalArgumentException` |
| KajiJDK | `0` |

**`java.lang.String.valueOf`** — exception versus value

```java
java.lang.String v0 = java.lang.String.valueOf(new char[] {(char) 0, (char) 56320, (char) 304, (char) 65}, 128, 32);
```

| | |
|---|---|
| reference | `EXC@0:java.lang.StringIndexOutOfBoundsException` |
| KajiJDK | `S[5695,1,6,0,5697,1,24,0,5700,1,5,0,5701,1,0,0,5712,1,9,0,5722,1,0,0,5728,1,24,0,5741,1,0,0]` |

**`java.math.BigDecimal.divide`** — different exception

```java
java.math.BigDecimal v0 = new java.math.BigDecimal(new char[] {(char) 65, (char) 55296, (char) 65535}, 63, 5).divide(new java.math.BigDecimal(0.1d), 1, java.math.RoundingMode.valueOf(s(933, 920, 926, 918, 55297, 56360))); java.beans.DefaultPersistenceDelegate v1 = new java.beans.DefaultPersistenceDe
```

| | |
|---|---|
| reference | `EXC@0:java.lang.NumberFormatException` |
| KajiJDK | `EXC@0:java.lang.StringIndexOutOfBoundsException` |

**`java.util.Formatter.<init>`** — different value

```java
java.util.Formatter v0 = new java.util.Formatter(java.util.Locale.of(s(55297, 56320, 12397, 12424, 12419))); java.util.Locale v1 = v0.locale();
```

| | |
|---|---|
| reference | `T[????]` |
| KajiJDK | `T[𐐨ねよゃ]` |

**`java.lang.constant.MethodTypeDesc.descriptorString`** — different exception

```java
java.lang.String v0 = java.lang.constant.MethodTypeDesc.ofDescriptor(s(12414, 0, 12381)).descriptorString();
```

| | |
|---|---|
| reference | `EXC@0:java.lang.IllegalArgumentException` |
| KajiJDK | `EXC@0:java.lang.StringIndexOutOfBoundsException` |

**`java.time.LocalTime.toSecondOfDay`** — different exception

```java
int v0 = java.time.LocalTime.parse(s(122, 104)).toSecondOfDay(); java.beans.ParameterDescriptor v1 = new java.beans.ParameterDescriptor();
```

| | |
|---|---|
| reference | `EXC@0:java.time.format.DateTimeParseException` |
| KajiJDK | `EXC@0:java.lang.StringIndexOutOfBoundsException` |

**`java.time.ZoneOffset.of`** — exception versus value

```java
java.time.ZoneOffset v0 = java.time.ZoneOffset.of(s(206, 212, 203, 202, 305)); java.beans.PropertyChangeSupport v1 = new java.beans.PropertyChangeSupport(v0);
```

| | |
|---|---|
| reference | `EXC@0:java.time.DateTimeException` |
| KajiJDK | `T[+1824:57]` |

**`java.lang.Math.nextAfter`** — different value

```java
double v0 = java.lang.Math.nextAfter(Double.MIN_VALUE, 1.0d);
```

| | |
|---|---|
| reference | `d9.9E-324/2` |
| KajiJDK | `d1.0E-323/2` |

**`java.lang.Math.copySign`** — different value

```java
double v0 = java.lang.Math.copySign(Double.MIN_VALUE, Double.MIN_VALUE);
```

| | |
|---|---|
| reference | `d4.9E-324/1` |
| KajiJDK | `d5.0E-324/1` |

**`java.util.Collections.list`** — different value

```java
java.util.ArrayList v0 = java.util.Collections.list(javax.security.sasl.Sasl.getSaslClientFactories()); int v1 = v0.size();
```

| | |
|---|---|
| reference | `6` |
| KajiJDK | `0` |

**`java.beans.PropertyEditorSupport.getJavaInitializationString`** — different value

```java
boolean v0 = s(1502, 1505, 1508, 90).startsWith(s(90, 948, 946, 947, 969), 63); java.lang.String v1 = new java.beans.PropertyEditorSupport(java.lang.Integer.valueOf(1)).getJavaInitializationString();
```

| | |
|---|---|
| reference | `false\|S[63,63,63]` |
| KajiJDK | `false\|S[110,117,108,108]` |

**`java.time.temporal.ValueRange.of`** — exception versus value

```java
java.time.temporal.ValueRange v0 = java.time.temporal.ValueRange.of(9223372036854775807L, 256L, -9223372036854775808L, 256L); java.lang.String[] v1 = new java.beans.PropertyEditorSupport().getTags();
```

| | |
|---|---|
| reference | `EXC@0:java.lang.IllegalArgumentException` |
| KajiJDK | `null` |

**`java.time.LocalTime.parse`** — different exception

```java
java.time.LocalTime v0 = java.time.LocalTime.parse(s(305, 1057, 1045)); boolean v1 = new java.beans.PropertyEditorSupport().supportsCustomEditor();
```

| | |
|---|---|
| reference | `EXC@0:java.time.format.DateTimeParseException` |
| KajiJDK | `EXC@0:java.lang.StringIndexOutOfBoundsException` |

**`java.util.Locale.of`** — different value

```java
java.util.Locale v0 = java.util.Locale.of(s(951, 769), s(101, 769, 107));
```

| | |
|---|---|
| reference | `T[??_E?K]` |
| KajiJDK | `T[ή_ÉK]` |

**`java.util.Locale.<init>`** — different value

```java
java.util.Locale v0 = new java.util.Locale(s(71, 65, 72, 67, 122), s(234, 240, 122), s(1103, 1093, 1079, 122)); java.beans.XMLEncoder v1 = new java.beans.XMLEncoder(java.io.OutputStream.nullOutputStream());
```

| | |
|---|---|
| reference | `T[gahcz_��Z_???z]` |
| KajiJDK | `T[gahcz_ÊÐZ_яхзz]` |

**`sun.misc.Signal.getNumber`** — different exception

```java
int v0 = new sun.misc.Signal(s(72, 71, 79, 769)).getNumber();
```

| | |
|---|---|
| reference | `EXC@0:java.lang.IllegalArgumentException` |
| KajiJDK | `EXC@0:java.lang.UnsupportedOperationException` |

**`java.time.ZoneOffset.equals`** — exception versus value

```java
boolean v0 = java.time.ZoneOffset.ofHoursMinutesSeconds(255, -2147483648, 1).equals(java.lang.Integer.valueOf(1)); java.beans.beancontext.BeanContextChild v1 = new java.beans.beancontext.BeanContextChildSupport().getBeanContextChildPeer();
```

| | |
|---|---|
| reference | `EXC@0:java.time.DateTimeException` |
| KajiJDK | `false` |

## What was not tested

109 operations could not be reached: no case could be built for them, because an
argument type has no producer or because nothing in the result is comparable. These
are holes in coverage, not passes.

- `com.sun.nio.file.ExtendedCopyOption.values()[Lcom/sun/nio/file/ExtendedCopyOption;`
- `com.sun.nio.sctp.Association.maxOutboundStreams()I`
- `com.sun.nio.sctp.Notification.association()Lcom/sun/nio/sctp/Association;`
- `com.sun.nio.sctp.PeerAddressChangeNotification.association()Lcom/sun/nio/sctp/Association;`
- `java.beans.BeanDescriptor.<init>(Ljava/lang/Class;Ljava/lang/Class;)V`
- `java.beans.EventSetDescriptor.getAddListenerMethod()Ljava/lang/reflect/Method;`
- `java.beans.EventSetDescriptor.getGetListenerMethod()Ljava/lang/reflect/Method;`
- `java.beans.FeatureDescriptor.setExpert(Z)V`
- `java.beans.MethodDescriptor.<init>(Ljava/lang/reflect/Method;)V`
- `java.beans.MethodDescriptor.<init>(Ljava/lang/reflect/Method;[Ljava/beans/ParameterDescriptor;)V`
- `java.beans.PropertyChangeEvent.setPropagationId(Ljava/lang/Object;)V`
- `java.beans.PropertyChangeSupport.fireIndexedPropertyChange(Ljava/lang/String;IZZ)V`
- `java.beans.PropertyDescriptor.createPropertyEditor(Ljava/lang/Object;)Ljava/beans/PropertyEditor;`
- `java.beans.PropertyEditorManager.registerEditor(Ljava/lang/Class;Ljava/lang/Class;)V`
- `java.beans.SimpleBeanInfo.getPropertyDescriptors()[Ljava/beans/PropertyDescriptor;`
- `java.beans.VetoableChangeSupport.fireVetoableChange(Ljava/lang/String;Ljava/lang/Object;Ljava/lang/Object;)V`
- `java.beans.beancontext.BeanContext.instantiateChild(Ljava/lang/String;)Ljava/lang/Object;`
- `java.beans.beancontext.BeanContextEvent.getPropagatedFrom()Ljava/beans/beancontext/BeanContext;`
- `java.beans.beancontext.BeanContextServiceAvailableEvent.getSourceAsBeanContextServices()Ljava/beans/beancontext/BeanContextServices;`
- `java.beans.beancontext.BeanContextServiceProvider.getCurrentServiceSelectors(Ljava/beans/beancontext/BeanContextServices;Ljava/lang/Class;)Ljava/util/Iterator;`
- `java.beans.beancontext.BeanContextServiceProvider.getService(Ljava/beans/beancontext/BeanContextServices;Ljava/lang/Object;Ljava/lang/Class;Ljava/lang/Object;)Ljava/lang/Object;`
- `java.beans.beancontext.BeanContextServices.releaseService(Ljava/beans/beancontext/BeanContextChild;Ljava/lang/Object;Ljava/lang/Object;)V`
- `java.beans.beancontext.BeanContextSupport.addBeanContextMembershipListener(Ljava/beans/beancontext/BeanContextMembershipListener;)V`
- `java.io.BufferedInputStream.read([BII)I`
- `java.io.BufferedReader.reset()V`
- `java.io.ByteArrayOutputStream.writeTo(Ljava/io/OutputStream;)V`
- `java.io.DataInputStream.skipBytes(I)I`
- `java.io.InputStreamReader.<init>(Ljava/io/InputStream;Ljava/nio/charset/CharsetDecoder;)V`
- `java.io.ObjectInputStream.readDouble()D`
- `java.io.ObjectStreamClass.lookupAny(Ljava/lang/Class;)Ljava/io/ObjectStreamClass;`

