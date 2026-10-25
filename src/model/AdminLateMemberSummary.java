package model;

import java.math.BigDecimal;

public record AdminLateMemberSummary(
        Long userId,
        String fullName,
        String username,
        Long overdueCount,
        Long totalContributions,
        BigDecimal overdueAmount
) {
    public double overdueRate() {
        return totalContributions == 0 ? 0 : overdueCount * 100.0 / totalContributions;
    }
}
