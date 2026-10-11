package model.enums;

/**
 * Enumeração que define os papéis e níveis de acesso de um utilizador no sistema.
 *
 * @author Sistema iXitique
 * @version 1.0
 */
public enum UserRole {

    /** Administrador do sistema com acesso total a todas as funcionalidades. */
    admin,

    /** Gestor de grupo responsável por administrar grupos e aprovar membros/ciclos. */
    group_manager,

    /** Membro participante num ou mais grupos. */
    member,

    /** Utilizador base com permissões padrão. */
    base
}