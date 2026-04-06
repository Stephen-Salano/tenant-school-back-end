package io.psyllabus.platform.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark a method or class as a system-level process.
 *
 * <p>When a method annotated with @SystemProcess is executed,
 * the ThreadLocal SystemContext is set to true, allowing
 * auditing to use the SYSTEM_UUID safely.</p>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface SystemProcess {
}
