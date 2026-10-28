package validation;

import java.math.BigDecimal;
import model.enums.ReceivingOrderMode;

/**
 * Classe utilitária para validação de dados de entrada na gestão de grupos de Xitique.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public final class GroupValidator {

    private GroupValidator() {
    }

    /**
     * Valida se o nome do grupo não é nulo ou vazio.
     * @param groupName Nome do grupo
     * @throws IllegalArgumentException Se o nome for nulo ou vazio
     */
    public static void validateGroupName(String groupName) {
        if (groupName == null || groupName.trim().isEmpty()) {
            throw new IllegalArgumentException("O nome do Grupo não pode ser nulo.");
        }
    }

    /**
     * Valida o comprimento mínimo do nome do grupo (mínimo de 3 caracteres).
     * @param groupName Nome do grupo
     * @throws IllegalArgumentException Se tiver menos de 3 caracteres
     */
    public static void validateGroupNameLength(String groupName) {
        if (groupName == null || groupName.trim().length() < 3 || groupName.trim().length() > 80) {
            throw new IllegalArgumentException("O nome do grupo deve ter entre 3 e 80 caracteres.");
        }
    }

    public static void validateGroupDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("A descrição do grupo é obrigatória.");
        }
        if (description.trim().length() > 150) {
            throw new IllegalArgumentException("A descrição do grupo não pode exceder 150 caracteres.");
        }
    }

    /**
     * Valida se a quota de contribuição por membro é superior a zero.
     * @param amountPerUser Montante estipulado
     * @throws IllegalArgumentException Se for nulo ou menor/igual a zero
     */
    public static void validateAmountPerUser(BigDecimal amountPerUser) {
        if (amountPerUser == null || amountPerUser.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("O valor de contribuição por membro deve ser maior que zero.");
        }
    }

    /**
     * Valida se o dia limite de pagamento está entre 1 e 31.
     * @param payday Dia do mês
     * @throws IllegalArgumentException Se for um dia inválido
     */
    public static void validatePayday(Integer payday) {
        if (payday == null || payday < 1 || payday > 31) {
            throw new IllegalArgumentException("O dia de pagamento (payday) deve ser um dia válido entre 1 e 31.");
        }
    }

    /**
     * Valida se a frequência de rotação em dias é um valor estritamente positivo.
     * @param frequency Frequência em dias
     * @throws IllegalArgumentException Se for menor ou igual a 0
     */
    public static void validateFrequency(Integer frequency) {
        if (frequency == null || frequency <= 0) {
            throw new IllegalArgumentException("A frequência de contribuição deve ser superior a 0 dias.");
        }
    }

    public static void validateMemberCapacity(Integer maxMembers) {
        if (maxMembers == null || maxMembers < 2) {
            throw new IllegalArgumentException("O grupo deve ter capacidade para pelo menos dois membros, incluindo o criador.");
        }
    }

    public static void validateReceivingOrderMode(ReceivingOrderMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("Selecione a forma de definir a ordem de recebimento.");
        }
    }
}