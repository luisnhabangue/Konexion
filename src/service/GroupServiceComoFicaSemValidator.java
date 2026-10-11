package service;

import dao.GroupDao;
import model.Group;
import util.EntityManagerFactoryUtil;

import java.math.BigDecimal;
import java.time.LocalDate;

public class GroupServiceComoFicaSemValidator {
    private final GroupDao  groupDao = new GroupDao(EntityManagerFactoryUtil.getEntityManagerFactory());


    public void createNewGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("O nome do Grupo não pode ser nulod");
        }
        if (group.getGroupName() == null || group.getGroupName().trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do grupo é obrigatório.");
        }
        if (group.getAmountPerUser() == null || group.getAmountPerUser().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor de contribuição por membro deve ser maior que zero.");
        }
        if (group.getPayday() == null || group.getPayday() < 1 || group.getPayday() > 31) {
            throw new IllegalArgumentException("O dia de pagamento (payday) deve ser um dia válido entre 1 e 31.");
        }
        if (groupDao.findByName(group.getGroupName().trim()) != null) {
            throw new IllegalArgumentException("Já existe um grupo cadastrado com o nome: " + group.getGroupName());
        }
        if (group.getFrequency() <= 0) {
            throw new IllegalArgumentException("A frequência de contribuição deve ser superior a 0 dias.");
        }
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


}
