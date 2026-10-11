package dao.interfaces;

import model.Cycle;
import model.enums.CycleStatus;

import java.util.List;

public interface CycleDaoInterface {
    void save(Cycle cycle);
    Cycle findById(Integer id);
    Cycle findActiveCycleByGroup(Long groupId);
    List<Cycle> findByGroupId(Long groupId);
    Integer findLastCycleNumberByGroup(Long groupId);
    void update(Cycle cycle);
    void delete(Cycle cycle);
}
