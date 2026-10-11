package model;

import jakarta.persistence.*;
import model.enums.Gender;
import model.enums.UserRole;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidade que representa um utilizador do sistema.
 * Extende {@link Person} e gere credenciais de acesso, papéis de permissão e associações a grupos.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
@Entity
@Table(name = "users")
public class User extends Person {

    /** Nome de utilizador para autenticação. */
    @Column(nullable = false, unique = true)
    private String username;

    /** Hash da palavra-passe criptografada. */
    @Column(name = "password_hash", nullable = false)
    private String password;

    /** Endereço de e-mail do utilizador. */
    @Column(unique = true)
    private String email;

    /** Papel ou nível de acesso do utilizador. */
    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    /** Número de telefone do utilizador. */
    @Column(unique = true)
    private String phoneNumber;

    /** Estado da conta do utilizador (ativo ou inativo). */
    private boolean active = true;

    /** Lista de associações do utilizador a grupos. */
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserGroup> userGroups = new ArrayList<>();

    /** Lista de grupos criados por este utilizador. */
    @OneToMany(mappedBy = "createdBy")
    private List<Group> createdGroups = new ArrayList<>();

    /** Construtor padrão da JPA. */
    public User() {
    }

    /**
     * Construtor para registo de um novo utilizador.
     *
     * @param id Identificador único
     * @param firstName Primeiro nome
     * @param lastName Apelido
     * @param documentId Documento de identificação
     * @param gender Género
     * @param username Nome de utilizador
     * @param password Palavra-passe (será encriptada no serviço)
     * @param email Endereço de e-mail
     * @param phoneNumber Número de telefone
     */
    public User(Long id, String firstName, String lastName, String documentId, Gender gender,
                String username, String password, String email, String phoneNumber) {
        super(id, firstName, lastName, documentId, gender);
        this.active = true;
        this.username = username;
        this.password = password;
        this.email = email;
        this.userRole = UserRole.base;
        this.phoneNumber = phoneNumber;
    }

    /** @return O nome de utilizador */
    public String getUsername() { return username; }
    /** @param username O novo nome de utilizador */
    public void setUsername(String username) { this.username = username; }

    /** @return A hash da palavra-passe */
    public String getPassword() { return password; }
    /** @param password A nova palavra-passe */
    public void setPassword(String password) { this.password = password; }

    /** @return O e-mail do utilizador */
    public String getEmail() { return email; }
    /** @param email O novo e-mail */
    public void setEmail(String email) { this.email = email; }

    /** @return O papel do utilizador */
    public UserRole getUserRole() { return userRole; }
    /** @param userRole O novo papel */
    public void setUserRole(UserRole userRole) { this.userRole = userRole; }

    /** @return O número de telefone */
    public String getPhoneNumber() { return phoneNumber; }
    /** @param phoneNumber O novo número de telefone */
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    /** @return {@code true} se o utilizador estiver ativo; {@code false} caso contrário */
    public boolean isActive() { return active; }
    /** @param active O novo estado ativo/inativo */
    public void setActive(boolean active) { this.active = active; }

    /** @return A lista de vínculos com grupos */
    public List<UserGroup> getUserGroups() { return userGroups; }
    /** @param userGroups A nova lista de vínculos */
    public void setUserGroups(List<UserGroup> userGroups) { this.userGroups = userGroups; }

    /** @return A lista de grupos criados pelo utilizador */
    public List<Group> getCreatedGroups() { return createdGroups; }
    /** @param createdGroups A nova lista de grupos criados */
    public void setCreatedGroups(List<Group> createdGroups) { this.createdGroups = createdGroups; }
}