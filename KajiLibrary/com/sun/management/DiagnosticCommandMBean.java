package com.sun.management;

import javax.management.DynamicMBean;

/**
 * Los comandos de diagnostico de la VM, expuestos como un MBean que se arma solo.
 *
 * <p>Es un {@link DynamicMBean} y no una interfaz con metodos porque el conjunto de comandos
 * <strong>no se conoce al compilar</strong>: depende de la VM, de su version y de que le hayan
 * compilado adentro. Una interfaz fija tendria que enumerarlos, y quedaria mal el dia que la VM
 * agregue uno.
 *
 * <p>La contrapartida es que el que llama tiene que preguntar primero: {@code getMBeanInfo}
 * devuelve las operaciones que esta VM realmente ofrece, con sus firmas, y recien despues se puede
 * invocar una.
 *
 * @since 1.6
 */
public interface DiagnosticCommandMBean extends DynamicMBean {
}
