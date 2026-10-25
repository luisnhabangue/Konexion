package model.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter
public class UserRoleConverter implements AttributeConverter<UserRole, String> {

    @Override
    public String convertToDatabaseColumn(UserRole role) {
        return role == null ? null : role.name();
    }

    @Override
    public UserRole convertToEntityAttribute(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toUpperCase(Locale.ROOT)) {
            case "ADMINISTRATOR", "ADMIN" -> UserRole.ADMINISTRATOR;
            case "USER", "GROUP_LEADER", "GROUP_MANAGER", "MEMBER", "BASE" -> UserRole.USER;
            default -> throw new IllegalArgumentException("Perfil de utilizador desconhecido: " + value);
        };
    }
}
