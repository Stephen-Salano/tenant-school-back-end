package io.psyllabus.platform.common.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Thread-local context to mark a thread as a "System Process".
 *
 * <p>This allows non-web operations (like data seeding, batch imports, or cron jobs)
 * to bypass strict auditing checks and use the SYSTEM_UUID instead of a real user ID.</p>
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SystemContext {

    private static final ThreadLocal<Boolean> IS_SYSTEM_PROCESS = ThreadLocal.withInitial(() -> false);

    /**
     * Marks the current thread as running a system-level process.
     */
    public static void setSystemProcess(boolean isSystem) {
        IS_SYSTEM_PROCESS.set(isSystem);
    }

    /**
     * Returns true if the current thread is marked as a system-level process.
     */
    public static boolean isSystemProcess() {
        return IS_SYSTEM_PROCESS.get();
    }

    /**
     * Clears the system process flag from the current thread.
     */
    public static void clear() {
        IS_SYSTEM_PROCESS.remove();
    }
}
