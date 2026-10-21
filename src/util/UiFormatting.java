package util;

import model.Person;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utilitário responsável pela conversão e formatação visual de dados na interface gráfica.
 *
 * @since 1.0
 */
public final class UiFormatting {

    /** Formatador numérico monetário configurado para a localização de Moçambique (pt-MZ). */
    private static final NumberFormat MONEY = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-MZ"));

    /** Formatador de datas no padrão "dd MMM yyyy" em português de Moçambique. */
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("pt-MZ"));

    static {
        MONEY.setMinimumFractionDigits(2);
        MONEY.setMaximumFractionDigits(2);
    }

    /**
     * Construtor privado para impedir a instanciação da classe utilitária.
     */
    private UiFormatting() {
    }

    /**
     * Formata um montante numérico no formato monetário em Meticais (MT).
     *
     * @param amount O valor a ser formatado (se for {@code null}, assume {@code BigDecimal.ZERO}).
     * @return A representação em texto do valor monetário com o sufixo "MT".
     */
    public static String money(BigDecimal amount) {
        return MONEY.format(amount == null ? BigDecimal.ZERO : amount) + " MT";
    }

    /**
     * Converte uma cadeia de texto contendo um valor numérico/monetário num objeto {@link BigDecimal}.
     *
     * @param value O texto do valor a ser convertido.
     * @return O valor equivalente como {@link BigDecimal}.
     * @throws NumberFormatException Se a cadeia estiver vazia ou num formato numérico inválido.
     */
    public static BigDecimal parseMoney(String value) {
        if (value == null || value.isBlank()) {
            throw new NumberFormatException("Valor vazio.");
        }
        String normalized = value.trim().replace(" ", "");
        if (normalized.contains(",") && normalized.contains(".")) {
            normalized = normalized.replace(".", "").replace(",", ".");
        } else if (normalized.contains(",")) {
            normalized = normalized.replace(",", ".");
        }
        return new BigDecimal(normalized);
    }

    /**
     * Extrai as iniciais em maiúsculas do primeiro e último nome da pessoa fornecida.
     *
     * @param person A pessoa ({@link Person}) da qual se extraem as iniciais.
     * @return As iniciais combinadas em maiúsculas, ou {@code "?"} se a pessoa for {@code null}.
     */
    public static String initials(Person person) {
        if (person == null) {
            return "?";
        }
        String first = person.getFirstName() == null || person.getFirstName().isBlank()
                ? "" : person.getFirstName().substring(0, 1);
        String last = person.getLastName() == null || person.getLastName().isBlank()
                ? "" : person.getLastName().substring(0, 1);
        return (first + last).toUpperCase(Locale.ROOT);
    }

    /**
     * Retorna o nome completo concatenando o primeiro e o último nome da pessoa.
     *
     * @param person A instância de {@link Person}.
     * @return O nome completo da pessoa.
     */
    public static String fullName(Person person) {
        return person.getFirstName() + " " + person.getLastName();
    }

    /**
     * Formata uma data no padrão visual "dd MMM yyyy".
     *
     * @param date A data ({@link LocalDate}) a ser formatada.
     * @return A data formatada em texto ou {@code "—"} se a data for {@code null}.
     */
    public static String date(LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }
}