package validation;

import java.math.BigDecimal;

public final class GroupValidator {

        public static void validateGroupName(String groupName) {
            if (groupName == null || groupName.trim().isEmpty()) {
                throw new IllegalArgumentException("O nome do Grupo não pode ser nulod");
            }
        }
        public static void validateGroupNameLength(String groupName) {
            if (groupName.length() < 3) {
                throw new IllegalArgumentException("O nome do grupo deve ter no minimo 4 letras");
            }
        }
        public  static void validateAmountPerUser(BigDecimal amountPerUser) {
            if (amountPerUser == null || amountPerUser.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("O valor de contribuição por membro deve ser maior que zero.");
            }
        }
        public static void validatePayday(Integer payday) {
            if (payday == null || payday < 1 || payday > 31) {
                throw new IllegalArgumentException("O dia de pagamento (payday) deve ser um dia válido entre 1 e 31.");
            }


        }
        public static void validateFrequency(Integer frequency) {
            if (frequency <= 0) {
                throw new IllegalArgumentException("A frequência de contribuição deve ser superior a 0 dias.");
            }
        }


}
