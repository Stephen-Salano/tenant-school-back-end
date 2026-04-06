package io.psyllabus.platform.common.aspect;

import io.psyllabus.platform.common.util.SystemContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Aspect to manage SystemContext for methods annotated with @SystemProcess.
 */
@Aspect
@Component
public class SystemProcessAspect {

    @Around("@annotation(io.psyllabus.platform.common.annotation.SystemProcess) || " +
            "@within(io.psyllabus.platform.common.annotation.SystemProcess)")
    public Object manageSystemContext(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean previouslySystem = SystemContext.isSystemProcess();
        SystemContext.setSystemProcess(true);
        try {
            return joinPoint.proceed();
        } finally {
            SystemContext.setSystemProcess(previouslySystem);
            if (!previouslySystem) {
                SystemContext.clear();
            }
        }
    }
}
