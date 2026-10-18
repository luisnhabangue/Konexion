 <div align="center">

# 🌿 Xitique Digital

### Sistema de Gestão de Poupanças Rotativas

**Poupar juntos. Crescer juntos.**

[![Java](https://img.shields.io/badge/Java-25-065F46?style=flat-square&logo=openjdk&logoColor=white)](https://www.java.com/)
[![JavaFX](https://img.shields.io/badge/JavaFX-Interface-047857?style=flat-square)](https://openjfx.io/)
[![Hibernate](https://img.shields.io/badge/Hibernate-JPA-059669?style=flat-square&logo=hibernate&logoColor=white)](https://hibernate.org/)
[![MySQL](https://img.shields.io/badge/MySQL-Database-0F766E?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/Maven-Build-065F46?style=flat-square&logo=apachemaven&logoColor=white)](https://maven.apache.org/)

---

</div>

## 📖 Sobre o Projeto

O **Xitique Digital** é uma aplicação desenvolvida com o objetivo de facilitar a gestão de grupos de poupança rotativa.

Inspirado na prática tradicional do xitique, o sistema pretende ajudar na organização dos participantes, no controlo das contribuições financeiras e na gestão dos ciclos e das rondas de recebimento.

A aplicação procura tornar a gestão dos grupos mais organizada e transparente, permitindo acompanhar as contribuições, identificar quem deve receber e consultar o histórico das operações realizadas.

## 🎯 Objetivos do Projeto

- Facilitar a criação e a gestão de grupos de poupança rotativa.
- Organizar os participantes e as respetivas responsabilidades.
- Registar e acompanhar as contribuições financeiras.
- Gerir os ciclos de poupança e as rondas de recebimento.
- Controlar os papéis e os estados dos participantes em cada grupo.
- Manter a consistência dos dados e das operações.
- Disponibilizar um histórico das operações relevantes.

## ✨ Funcionalidades

O sistema contempla as seguintes funcionalidades:

| Funcionalidade | Descrição |
|---|---|
| 👤 **Gestão de utilizadores** | Registo e gestão das contas de acesso. |
| 👥 **Gestão de grupos** | Criação, consulta, atualização e gestão dos grupos de xitique. |
| 🤝 **Gestão de participantes** | Associação de utilizadores aos grupos e gestão das respetivas participações. |
| 🛡️ **Gestão de papéis** | Atribuição e alteração de responsabilidades, como líder, tesoureiro e membro. |
| 💰 **Gestão de contribuições** | Registo e acompanhamento dos valores contribuídos pelos participantes. |
| 🔄 **Gestão de ciclos** | Criação e acompanhamento dos ciclos de poupança. |
| 📅 **Gestão de rondas** | Organização da ordem de recebimento dos participantes. |
| 💸 **Gestão de recebimentos** | Registo e acompanhamento dos valores entregues aos participantes. |
| 📋 **Histórico de operações** | Consulta dos acontecimentos relevantes relacionados com os grupos e as suas operações. |
| ✅ **Validação das regras de negócio** | Prevenção de operações inválidas e manutenção da consistência dos dados. |

### Telas e interação com os dados

As principais telas da aplicação já utilizam dados persistidos. O utilizador pode:

- Registar-se e iniciar sessão.
- Criar grupos e consultar os grupos de que participa.
- Gerir membros.
- Abrir ciclos de poupança.
- Registar pagamentos.
- Consultar o histórico.
- Editar os dados do perfil.

## 🚀 Executar e Utilizar

### Pré-requisitos

- Java Development Kit (JDK).
- MySQL Server.
- Maven, caso a execução seja feita através da linha de comandos.
- IntelliJ IDEA ou outro IDE compatível com JavaFX.

### Configuração

**1. Configurar a base de dados**

Configure uma base de dados MySQL local e ajuste a URL, o utilizador e a palavra-passe da ligação no ficheiro `META-INF/persistence.xml`.

O Hibernate está configurado para criar ou atualizar as tabelas da aplicação.

**2. Compilar o projeto**

Na raiz do projeto, execute:

```bash
mvn test
```

No IDE, inicie a classe `view.Application`.

**3. Criar uma conta**

No ecrã de entrada, registe uma conta e inicie sessão.

**4. Criar e gerir um grupo**

Depois de entrar, crie um grupo e adicione utilizadores registados através do respetivo username ou documento.

**5. Iniciar um ciclo**

Abra um ciclo com pelo menos dois membros e registe as contribuições na tela de detalhes.

O histórico, o painel inicial e o perfil apresentam os registos da base de dados, sem depender dos exemplos visuais antigos.

### Dados de demonstração

Para experimentar sem inserir manualmente vários registos, utilize a opção **Carregar dados de demonstração**, disponível na tela de grupos.

A operação é explícita e idempotente por conta. Adiciona à base de dados:

- Um grupo.
- Dois membros fictícios.
- Um ciclo.
- Duas contribuições.

## 📐 Regras de Negócio da Interface

### Registo e validação

- O username, email, telefone e documento não podem repetir-se.
- Emails, telefones e palavras-passe são validados no registo.
- Um grupo exige um nome entre 3 e 80 caracteres, descrição até 150 caracteres, quota positiva, dia de pagamento entre 1 e 31 e frequência válida.
- O criador entra automaticamente como primeiro membro do grupo.

### Gestão de grupos e membros

- Só o criador pode adicionar membros, reordenar a fila ou eliminar o grupo.
- Não se adicionam ou removem membros nem se elimina um grupo depois de começar o histórico de ciclos.
- Um grupo com ciclos iniciados não pode ser eliminado, para preservar o histórico financeiro.

### Ciclos e contribuições

- Um ciclo exige pelo menos dois membros.
- Não é possível abrir um ciclo enquanto outro estiver em andamento.
- A rotação reinicia depois de todos os membros receberem.
- Cada membro pode pagar uma única quota por ciclo.
- O valor pago deve ser pelo menos igual à quota do grupo.
- A data do pagamento deve estar entre o início do ciclo e o dia atual.
- O ciclo conclui quando todos os membros pagam.

## 🛠️ Tecnologias Utilizadas

| Tecnologia | Finalidade |
|---|---|
| **Java** | Desenvolvimento da lógica da aplicação. |
| **JavaFX** | Desenvolvimento da interface gráfica. |
| **Hibernate / JPA** | Mapeamento objeto-relacional e persistência dos dados. |
| **MySQL** | Armazenamento e gestão da base de dados. |
| **Maven** | Gestão de dependências e construção do projeto. |
| **Git** | Controlo de versões do código. |
| **GitHub** | Alojamento do repositório e colaboração da equipa. |

## 🏗️ Arquitetura do Projeto

O código está organizado em camadas, separando as responsabilidades de cada componente para facilitar a manutenção e a evolução da aplicação.

| Camada | Responsabilidade |
|---|---|
| **Model** | Contém as entidades e as classes que representam os conceitos do sistema. |
| **DAO** | Responsável pelas operações de acesso e persistência dos dados. |
| **Service** | Responsável pelas regras de negócio, validações e coordenação das operações. |
| **Controller** | Recebe e trata as ações da interface, comunicando com a camada de serviço. |
| **View** | Contém os componentes da interface gráfica desenvolvidos com JavaFX. |

## 🗄️ Modelagem e Base de Dados

A modelagem do sistema é orientada pelos princípios da Programação Orientada a Objetos e representada através de diagramas UML.

A base de dados é implementada em MySQL, utilizando Hibernate/JPA para estabelecer o mapeamento entre as classes Java e as tabelas relacionais.

O modelo representa os utilizadores, grupos, participações, ciclos, rondas e contribuições, incluindo os dados necessários para gerir os recebimentos e o histórico das operações.

As regras de negócio são tratadas principalmente na camada de serviço, recorrendo também a mecanismos de integridade da base de dados sempre que necessário.

## 🎓 Contexto Académico

O Xitique Digital é um projeto semestral desenvolvido no âmbito da cadeira de Programação, com o propósito de aplicar conceitos de:

- Programação Orientada a Objetos.
- Modelagem UML.
- Persistência de dados.
- Desenvolvimento de aplicações Java.
- Organização arquitetural e separação de responsabilidades.

---

<div align="center">

**Xitique Digital**  
*Projeto académico desenvolvido para fins educacionais.*

</div>
