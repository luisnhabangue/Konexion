package dao.interfaces;

import model.Group;

import java.util.List;

public interface GroupDaoInterface {


    public Group findById(Long id);
    public Group findByName(String name);
    public List<Group> findAll();
    public void updateGroup(Group group);
    public void deleteGroup(Group group);
    public void createGroup(Group group);

}
