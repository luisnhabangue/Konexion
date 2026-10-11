package service;

import dao.GroupDao;
import dao.UserDao;
import dao.UserGroupDao;
import model.Group;
import model.User;
import model.UserGroup;
import util.EntityManagerFactoryUtil;
import validation.GroupValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class GroupService {
    private final GroupDao  groupDao = new GroupDao(EntityManagerFactoryUtil.getEntityManagerFactory());
    private final UserDao userDao = new UserDao(EntityManagerFactoryUtil.getEntityManagerFactory());
    private final UserGroupDao userGroupDao = new UserGroupDao(EntityManagerFactoryUtil.getEntityManagerFactory());


    public void createNewGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("Grupo não pode ser nulo");
        }
        GroupValidator.validateGroupNameLength(group.getGroupName());
        GroupValidator.validateAmountPerUser(group.getAmountPerUser());
        GroupValidator.validatePayday(group.getPayday());
        GroupValidator.validateGroupName(group.getGroupName());
        GroupValidator.validateFrequency(group.getFrequency());

        if (!group.getCreatedBy().isActive()) {
            throw new IllegalStateException("Usuários inativos não podem criar grupos.");
        }
        if (group.getCreatedAt() == null) {
            group.setCreatedAt(LocalDate.now());
        }

        //Para deixar o nome sem espaco
        group.setGroupName(group.getGroupName().trim());
        groupDao.createGroup(group);


    }

    /**
     * Adiciona um Usuário existente a um Grupo existente.
     *
     * @param userId  ID do Usuário a ser adicionado
     * @param groupId ID do Grupo
     */
    public void addMemberToGroup(Long groupId, Long userId) {
        if (groupId == null || userId == null) {
            throw new IllegalArgumentException("IDs do grupo e do usuário são obrigatórios.");
        }

        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }

        User user = userDao.findById(userId);
        if (user == null || !user.isActive()) {
            throw new IllegalArgumentException("Usuário não encontrado ou está inativo.");
        }

        if (userGroupDao.isUserInGroup(userId, groupId)) {
            throw new IllegalStateException("O usuário '" + user.getFirstName() + "' já é membro deste grupo.");
        }

        // posição na ordem de recebimento (1º, 2º, 3º...)
        int currentMembers = userGroupDao.countMembersInGroup(groupId);
        int nextOrder = currentMembers + 1;

        // Associando o usuario ao seu dado grupo
        UserGroup userGroup = new UserGroup();
        userGroup.setGroup(group);
        userGroup.setUser(user);
        userGroup.setBalance(BigDecimal.ZERO);
        userGroup.setReceived(false);
        userGroup.setUserReceivingOrder(nextOrder);


        group.addMember(userGroup);

        groupDao.updateGroup(group);
    }

    public void removeMemberFromGroup(Long groupId, Long userGroupId) {

        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }

        UserGroup userGroup = userGroupDao.findByUserAndGroup(userGroupId,groupId);
        if (userGroup == null ) {
            throw new IllegalArgumentException("Usuário não encontrado ou está inativo.");
        }

        group.removeMember(userGroup);
        groupDao.updateGroup(group);

    }


    public void addNewCycleToGroup(Long groupId, Long cycleId) {
        Group group = findById(groupId);
        if (group == null) {
            throw new IllegalArgumentException("Grupo não encontrado com o ID: " + groupId);
        }





    }

    public Group findById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("ID inválido.");
        }
        return groupDao.findById(id);
    }

    public List<Group> findAll() {
        return groupDao.findAll();
    }


}
