package dao.interfaces;

import model.Group;
import model.User;
import model.UserGroup;

import java.util.List;

public interface UserGroupInterface {

    public void save(UserGroup userGroup);
    public boolean isUserInGroup(Long userId, Long groupId);
    public int countMembersInGroup(Long groupId);
    public List<UserGroup> findByGroupId(Long groupId);
    public UserGroup findByUserAndGroup(Long userId, Long groupId);

}
