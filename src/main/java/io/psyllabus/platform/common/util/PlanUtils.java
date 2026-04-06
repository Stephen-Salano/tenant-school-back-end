package io.psyllabus.platform.common.util;

import io.psyllabus.platform.subscription.enums.PlanType;

/**
 * Utility for managing plan-based constraints and limits.
 * Centralizes monetization logic to avoid leakage across services.
 */
public final class PlanUtils {

    private PlanUtils() {
        throw new UnsupportedOperationException("Utility class - cannot instantiate");
    }

    /**
     * Determines the maximum number of branches allowed for a given plan.
     * <p>
     * Tiers:
     * <ul>
     *   <li>BASIC: 2 (Main + 1 extra)</li>
     *   <li>PROFESSIONAL: 10 (Campus network)</li>
     * </ul>
     * </p>
     *
     * @param planType the school's current subscription plan
     * @return the maximum allowed branch count
     */
    public static int getBranchLimit(PlanType planType) {
        if (planType == null) return 1;
        
        return switch (planType) {
            case BASIC -> 2;
            case PROFESSIONAL -> 10;
        };
    }
}
