package model;

import java.math.BigDecimal;

public record AdminMemberSummary(Long id, String fullName, String username,
                                 BigDecimal balance, boolean groupCreator) {
    @Override
    public String toString() {
        return fullName + " (@" + username + ")";
    }
}
