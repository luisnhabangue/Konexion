package model;

import java.math.BigDecimal;

public record AdminGroupSummary(
        Long id,
        String name,
        String managerName,
        BigDecimal balance,
        BigDecimal contributionAmount,
        Long memberCount,
        Integer latestCycle,
        Long activeCycles,
        boolean archived
) {}
