package util;

import model.Person;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
/**
 * Utilitário partilhado que centraliza a funcionalidade indicada pelo nome da classe.
 *
 * @since 1.0
 */

public final class UiFormatting {
    private static final NumberFormat MONEY = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-MZ"));
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.forLanguageTag("pt-MZ"));

    static {
        MONEY.setMinimumFractionDigits(2);
        MONEY.setMaximumFractionDigits(2);
    }

    private UiFormatting() {
    }

    public static String money(BigDecimal amount) {
        return MONEY.format(amount == null ? BigDecimal.ZERO : amount) + " MT";
    }

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

    public static String fullName(Person person) {
        return person.getFirstName() + " " + person.getLastName();
    }

    public static String date(LocalDate date) {
        return date == null ? "—" : DATE.format(date);
    }
}
