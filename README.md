
<div align="center">

<img width="100%" src="https://capsule-render.vercel.app/api?type=waving&color=0:022C22,45:065F46,100:059669&height=200&section=header&text=KONEXION&fontSize=40&fontColor=FFFFFF&fontAlignY=38&desc=Sistema%20de%20Gest%C3%A3o%20de%20Poupan%C3%A7as%20Rotativas&descSize=15&descAlignY=60&descColor=ECFDF5" />

<br/>

### Poupar juntos. Crescer juntos.

**Uma solução digital inspirada na tradição do xitique.**

<br/>

[![Java](https://img.shields.io/badge/Java-25-065F46?style=flat-square&logo=openjdk&logoColor=white)](https://www.java.com/)
[![JavaFX](https://img.shields.io/badge/JavaFX-Interface-047857?style=flat-square)](https://openjfx.io/)
[![Hibernate](https://img.shields.io/badge/Hibernate-JPA-059669?style=flat-square&logo=hibernate&logoColor=white)](https://hibernate.org/)
[![MySQL](https://img.shields.io/badge/MySQL-Database-374151?style=flat-square&logo=mysql&logoColor=white)](https://www.mysql.com/)
[![Payment Simulation](https://img.shields.io/badge/Payment-API%20Simulation-065F46?style=flat-square&logoColor=white)](#-simulação-de-pagamentos)

<br/>

[Repositório](https://github.com/luisnhabangue/iXitique) · [Equipa](#-equipa)

</div>

---

## 🌿 Sobre o Projeto

O **Konexion** é uma aplicação desktop desenvolvida para facilitar a gestão de grupos de poupança rotativa, inspirada na prática tradicional do xitique.

A aplicação procura tornar a organização dos participantes, o acompanhamento das contribuições e a gestão dos ciclos de poupança mais simples e transparentes.

Através do sistema, os utilizadores podem gerir grupos, acompanhar as contribuições, organizar as rondas de recebimento e consultar informações relacionadas com as operações realizadas.

O projeto aplica conceitos de Programação Orientada a Objetos, persistência de dados e desenvolvimento de interfaces gráficas com Java.

## 🎯 Objetivos

- Facilitar a criação e a gestão de grupos de poupança rotativa.
- Organizar os participantes e as respetivas responsabilidades.
- Registar e acompanhar as contribuições financeiras.
- Gerir os ciclos de poupança e as rondas de recebimento.
- Aplicar regras de negócio e validações.
- Preservar o histórico das operações.
- Explorar a simulação de pagamentos através de uma API.

## ✨ Funcionalidades

| Funcionalidade | Descrição |
|---|---|
| 👤 **Gestão de utilizadores** | Registo, autenticação e gestão de contas. |
| 👥 **Gestão de grupos** | Criação e consulta de grupos de xitique. |
| 🤝 **Participações** | Associação de utilizadores aos grupos e gestão das respetivas participações. |
| 🛡️ **Controlo de acessos** | Organização dos acessos de acordo com os papéis e as permissões definidos no sistema. |
| 💰 **Contribuições** | Registo e acompanhamento dos valores contribuídos pelos participantes. |
| 🔄 **Ciclos de poupança** | Abertura e acompanhamento dos ciclos. |
| 📅 **Rondas de recebimento** | Organização da ordem de recebimento dos participantes. |
| 💳 **Simulação de pagamentos** | Simulação do processo de pagamento através de uma API. |
| 📋 **Histórico** | Consulta das operações relevantes. |
| ✅ **Validações** | Aplicação de regras de negócio para prevenir operações inválidas. |
| 👤 **Perfil** | Consulta e edição dos dados do utilizador. |

### 🖥️ Interface e interação com os dados

A aplicação inclui telas para as principais operações do sistema, ligadas à persistência dos dados.

Entre as operações disponibilizadas encontram-se:

- Registar uma conta e iniciar sessão.
- Criar e consultar grupos.
- Gerir participantes.
- Abrir ciclos de poupança.
- Registar contribuições.
- Consultar o histórico.
- Editar os dados do perfil.

## 💳 Simulação de Pagamentos

O sistema utiliza uma **simulação de uma API de pagamento** para representar o processo de pagamento dentro da aplicação.

Esta abordagem permite trabalhar o fluxo de uma operação de pagamento sem apresentar o sistema como estando integrado com um serviço financeiro real.

A simulação faz parte do contexto académico do projeto e pode servir de base para uma futura integração com um fornecedor de pagamentos, caso essa evolução seja implementada.

## 📐 Regras de Negócio

### Registo e validação

- Username, email, telefone e documento devem ser únicos.
- Os dados de registo são validados.
- O nome do grupo deve ter entre 3 e 80 caracteres.
- A descrição do grupo pode ter até 150 caracteres.
- A quota deve ser positiva.
- O dia de pagamento deve estar entre 1 e 31.
- A frequência deve corresponder a um valor válido.
- O criador é automaticamente o primeiro membro do grupo.

### Gestão de grupos e participantes

- O criador tem permissões específicas para gerir os participantes, reordenar a fila e eliminar o grupo.
- A gestão dos participantes é restringida depois de começar o histórico dos ciclos.
- Um grupo com ciclos iniciados não pode ser eliminado, para preservar o histórico financeiro.

### Ciclos e contribuições

- Um ciclo exige pelo menos dois participantes.
- Um grupo só pode ter um ciclo ativo de cada vez.
- Os participantes ativos de um ciclo são definidos quando o ciclo é iniciado.
- Cada participante pode pagar uma única quota por ciclo.
- O valor pago deve respeitar o montante mínimo definido para a contribuição.
- A data do pagamento deve estar entre o início do ciclo e a data atual.
- O ciclo é concluído quando todos os participantes cumprem as condições de pagamento definidas.

## 🛠️ Tecnologias Utilizadas

<div align="center">

| Tecnologia | Finalidade |
|:---:|---|
| **Java 25** | Desenvolvimento da lógica da aplicação. |
| **JavaFX** | Construção da interface gráfica desktop. |
| **Hibernate / JPA** | Mapeamento objeto-relacional e persistência dos dados. |
| **MySQL** | Armazenamento e gestão dos dados. |
| **Git** | Controlo de versões. |
| **GitHub** | Alojamento do repositório e colaboração. |
| **IntelliJ IDEA** | Ambiente de desenvolvimento. |

</div>

## 🏗️ Arquitetura do Projeto

O projeto está organizado em camadas, separando as responsabilidades para facilitar a manutenção e a evolução da aplicação.

| Camada | Responsabilidade |
|---|---|
| **Model** | Representa as entidades e os conceitos do sistema. |
| **DAO** | Responsável pelas operações de acesso e persistência dos dados. |
| **Service** | Implementa as regras de negócio e as validações. |
| **Controller** | Trata as ações da interface e comunica com os serviços. |
| **View** | Contém os componentes gráficos desenvolvidos com JavaFX. |

Esta separação permite organizar o código e reduzir a dependência direta entre a interface, as regras de negócio e a persistência.

## 🗄️ Modelagem e Base de Dados

A modelagem do sistema baseia-se nos princípios da Programação Orientada a Objetos.

A persistência é realizada com **MySQL e Hibernate/JPA**, que permitem mapear as classes Java para as tabelas relacionais.

O modelo contempla conceitos como:

- Utilizadores.
- Grupos.
- Participações nos grupos.
- Ciclos de poupança.
- Rondas.
- Contribuições.
- Registos relacionados com os recebimentos e o histórico das operações.

As regras de negócio são tratadas principalmente na camada `Service`, enquanto o Hibernate/JPA assegura o mapeamento e a persistência das entidades.

## 🚀 Executar o Projeto

### Pré-requisitos

- JDK compatível com a versão utilizada no projeto.
- MySQL Server.
- IntelliJ IDEA ou outro IDE compatível com JavaFX.

### 1. Obter o projeto

Clona o repositório:

```bash
git clone https://github.com/luisnhabangue/iXitique.git
```

Abre a pasta do projeto no IntelliJ IDEA.

### 2. Configurar a base de dados

Configura o servidor MySQL e verifica os parâmetros de ligação no ficheiro `persistence.xml`, incluindo:

- URL da base de dados.
- Nome de utilizador.
- Palavra-passe.
- Configurações de persistência do Hibernate.

Confirma que a base de dados configurada existe no servidor MySQL.

### 3. Configurar e executar

No IntelliJ IDEA, confirma que as bibliotecas necessárias, incluindo JavaFX, Hibernate/JPA e o driver JDBC do MySQL, estão disponíveis.

Localiza a classe `view.Application` e executa a aplicação através do IDE.

### 4. Começar a utilizar

1. Regista uma conta e inicia sessão.
2. Cria um grupo de poupança.
3. Adiciona participantes registados.
4. Abre um ciclo com pelo menos dois participantes.
5. Regista as contribuições e consulta os dados disponíveis na aplicação.

### Dados de demonstração

Se a opção **Carregar dados de demonstração** estiver disponível na versão atual, poderá ser utilizada para inserir registos de teste na base de dados.

Na versão descrita no projeto, os dados de demonstração incluem:

- Um grupo.
- Dois membros fictícios.
- Um ciclo.
- Duas contribuições.

A operação foi concebida para evitar a duplicação dos mesmos dados de demonstração por conta.

## 🔐 Organização de Acessos

O sistema distingue os papéis de acesso à aplicação das responsabilidades associadas à participação num grupo.

- **Administrador:** papel de acesso ao sistema, de acordo com as permissões implementadas.
- **Líder do grupo:** responsável pela gestão do grupo dentro das permissões atribuídas.
- **Membro:** participa no grupo e acompanha as operações permitidas.

As permissões devem respeitar o contexto do grupo e as regras de acesso implementadas. A simulação de pagamentos não implica a existência de um papel de tesoureiro.

## 👩🏽‍💻 Equipa

O Konexion é um projeto desenvolvido em equipa.

<div align="center">

<table>
<tr>
<td align="center" width="33%">
<a href="https://github.com/aristidiatembe-dev">
<img src="https://github.com/aristidiatembe-dev.png" width="90" alt="Perfil GitHub da autora" />
<br/>
<strong>Aristidia Tembe</strong>
</a>
<br/>
<sub>Desenvolvimento</sub>
</td>
<td align="center" width="33%">
<a href="https://github.com/luisnhabangue">
<img src="https://github.com/luisnhabangue.png" width="90" alt="Perfil GitHub de Luis Nhabangue" />
<br/>
<strong>Luis Nhabangue</strong>
</a>
<br/>
<sub>Desenvolvimento</sub>
</td>
<td align="center" width="33%">
<a href="https://github.com/Wagner-Altz">
<img src="https://github.com/Wagner-Altz.png" width="90" alt="Perfil GitHub de Wagner Altz" />
<br/>
<strong>Wagner Altz</strong>
</a>
<br/>
<sub>Desenvolvimento</sub>
</td>
</tr>
</table>

</div>

## 🎓 Contexto Académico

O **Konexion** é um projeto semestral desenvolvido no âmbito da cadeira de Programação, com o objetivo de aplicar conhecimentos de:

- Programação Orientada a Objetos.
- Modelagem de sistemas.
- Persistência de dados.
- Desenvolvimento de aplicações desktop com Java.
- Arquitetura em camadas.
- Separação de responsabilidades e regras de negócio.
- Controlo de versões com Git.

---

<div align="center">

<img width="100%" src="https://capsule-render.vercel.app/api?type=waving&color=0:059669,50:065F46,100:022C22&height=100&section=footer" />

### Konexion

**Poupar juntos. Crescer juntos.**

<sub>Projeto académico desenvolvido para fins educacionais.</sub>

<br/>

[Repositório GitHub](https://github.com/luisnhabangue/iXitique)

</div>
