package dao.interfaces;

import model.Contribution;


import java.math.BigDecimal;
import java.util.List;

public interface ContributionDaoInterface {
    void save(Contribution contribution);
    Contribution findById(Long id);
    Contribution findByUserGroupAndCycle(Long userGroupId, Integer cycleId);
    List<Contribution> findByCycleId(Integer cycleId);
    List<Contribution> findByUserGroupId(Long userGroupId);
    BigDecimal sumTotalPaidByCycle(Integer cycleId);
    void update(Contribution contribution);
    void delete(Contribution contribution);
}
