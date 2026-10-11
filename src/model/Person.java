package model;

import jakarta.persistence.*;
import model.enums.Gender;

@MappedSuperclass
public abstract class Person {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = false, name = "firstName")
    private String firstName;

    @Column(nullable = false, unique = false, name = "lastName")
    private String lastName;

    @Column(nullable = false, unique = true, name = "documentId")
    private String documentId;

    @Column(nullable = false, name = "gender")
    private Gender gender;

    public Person() {
    }
    public Person(Long id, String firstName, String lastName, String documentId, Gender gender) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.documentId = documentId;
        this.gender = gender;
    }

    public Long getId() {return id;}
    public void setId(Long id) {this.id = id;}
    public String getFirstName() {return firstName;}
    public void setFirstName(String firstName) {this.firstName = firstName;}
    public String getLastName() {return lastName;}
    public void setLastName(String lastName) {this.lastName = lastName;}
    public String getDocumentId() {return documentId;}
    public void setDocumentId(String documentId) {this.documentId = documentId;}
    public Gender getGender() {return gender;}
    public void setGender(Gender gender) {this.gender = gender;}
}
