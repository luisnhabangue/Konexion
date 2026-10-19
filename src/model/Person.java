package model;

import jakarta.persistence.*;
import model.enums.Gender;

/**
 * Classe abstrata que representa a estrutura base de uma pessoa no sistema.
 * Mapeada como superclasse JPA para partilhar atributos comuns com entidades filhas.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
@MappedSuperclass
public abstract class Person {

    /**
     * Identificador único da pessoa.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Primeiro nome da pessoa.
     */
    @Column(nullable = false, unique = false, name = "firstName")
    private String firstName;

    /**
     * Apelido ou último nome da pessoa.
     */
    @Column(nullable = false, unique = false, name = "lastName")
    private String lastName;

    /**
     * Número do documento de identificação (ex.: BI, Passaporte).
     */
    @Column(nullable = true, unique = true, name = "documentId")
    private String documentId;

    /**
     * Género da pessoa.
     */
    @Column(nullable = false, name = "gender")
    private Gender gender;

    /**
     * Construtor padrão necessário para a especificação JPA.
     */
    public Person() {
    }

    /**
     * Construtor completo para inicialização da entidade pessoa.
     *
     * @param id Identificador único
     * @param firstName Primeiro nome
     * @param lastName Apelido
     * @param documentId Documento de identificação único
     * @param gender Género da pessoa
     */
    public Person(Long id, String firstName, String lastName, String documentId, Gender gender) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.documentId = documentId;
        this.gender = gender;
    }

    /** @return O identificador único da pessoa */
    public Long getId() { return id; }

    /** @param id O novo identificador único */
    public void setId(Long id) { this.id = id; }

    /** @return O primeiro nome */
    public String getFirstName() { return firstName; }

    /** @param firstName O novo primeiro nome */
    public void setFirstName(String firstName) { this.firstName = firstName; }

    /** @return O apelido */
    public String getLastName() { return lastName; }

    /** @param lastName O novo apelido */
    public void setLastName(String lastName) { this.lastName = lastName; }

    /** @return O número de documento de identificação */
    public String getDocumentId() { return documentId; }

    /** @param documentId O novo documento de identificação */
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    /** @return O género da pessoa */
    public Gender getGender() { return gender; }

    /** @param gender O novo género */
    public void setGender(Gender gender) { this.gender = gender; }
}