package model;

import jakarta.persistence.*;
import model.enums.Gender;
import model.enums.UserRole;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User extends Person {

    @Column(nullable = false, unique = true)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String password;

    @Column(unique = true)
    private String email;

    @Enumerated(EnumType.STRING)
    private UserRole userRole;

    @Column(unique = true)
    private String phoneNumber;

    private boolean active = true;


    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<UserGroup> userGroups = new ArrayList<>();


    @OneToMany(mappedBy = "createdBy")
    private List<Group> createdGroups = new ArrayList<>();

    public User() {
    }

    public User(Long id, String firstName, String lastName, String documentId, Gender gender,
                String username, String password, String email, UserRole userRole, String phoneNumber) {
        super(id, firstName, lastName, documentId, gender);
        this.active = true;
        this.username = username;
        this.password = password;
        this.email = email;
        this.userRole = userRole;
        this.phoneNumber = phoneNumber;
    }


    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public UserRole getUserRole() { return userRole; }
    public void setUserRole(UserRole userRole) { this.userRole = userRole; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public List<UserGroup> getUserGroups() { return userGroups; }
    public void setUserGroups(List<UserGroup> userGroups) { this.userGroups = userGroups; }
    public List<Group> getCreatedGroups() { return createdGroups; }
    public void setCreatedGroups(List<Group> createdGroups) { this.createdGroups = createdGroups; }
}