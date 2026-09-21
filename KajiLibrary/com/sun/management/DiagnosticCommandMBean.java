package com.sun.management;

import javax.management.DynamicMBean;

/**
 * The VM's diagnostic commands, exposed as an MBean that builds itself.
 *
 * <p>It is a {@link DynamicMBean} and not an interface with methods because the set of commands
 * <strong>is not known when compiling</strong>: it depends on the VM, on its version and on
 * what was compiled into it. A fixed interface would have to enumerate them, and would be left
 * wrong the day the VM adds one.
 *
 * <p>The counterpart is that whoever calls has to ask first: {@code getMBeanInfo} returns the
 * operations this VM really offers, with their signatures, and only afterwards may one be
 * invoked.
 *
 * @since 1.6
 */
public interface DiagnosticCommandMBean extends DynamicMBean {
}
