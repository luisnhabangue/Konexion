
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

[Repositório](https://github.com/luisnhabangue/iXitique) · [Equipa](#equipa)

</div>

---

> 📖 **Guia de utilizador:** instruções passo a passo para instalar, executar e utilizar a aplicação em [GUIA_UTILIZADOR.md](GUIA_UTILIZADOR.md).

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
| 🔑 **Recuperação de palavra-passe** | Envio de um código de 6 dígitos por e-mail para redefinir a palavra-passe. |
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
| 🆘 **Suporte** | Envio e acompanhamento de pedidos de ajuda, com respostas da equipa administrativa. |

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
- A capacidade do grupo, incluindo o criador, é definida na criação e deve ser de pelo menos dois membros.
- O valor esperado do bolo por ciclo é a quota multiplicada pela capacidade; a rotação prevê um ciclo por membro.
- A ordem de recebimento pode seguir a fila manual ou ser sorteada quando o grupo atinge a capacidade.

### Gestão de grupos e participantes

- O criador tem permissões específicas para gerir os participantes, reordenar a fila e eliminar o grupo.
- A gestão dos participantes é restringida depois de começar o histórico dos ciclos.
- Um grupo com ciclos iniciados não pode ser eliminado, para preservar o histórico financeiro.

### Ciclos e contribuições

- Os ciclos de grupos novos só começam depois de o grupo atingir a capacidade definida.
- Na ordem manual, o responsável organiza a fila e inicia o primeiro ciclo; depois disso, o sistema seleciona os beneficiários seguintes pela ordem configurada.
- Na ordem aleatória, o sistema sorteia a fila ao completar o grupo e inicia a rotação.
- Um grupo só pode ter um ciclo ativo de cada vez.
- Os participantes ativos de um ciclo são definidos quando o ciclo é iniciado.
- Cada participante pode pagar uma única quota por ciclo.
- O valor pago deve respeitar o montante mínimo definido para a contribuição.
- A data do pagamento deve estar entre o início do ciclo e a data atual.
- O ciclo é concluído quando todos os participantes cumprem as condições de pagamento definidas.
- O bolo só pode ser transferido depois de todos os membros pagarem; o responsável pode transferi-lo antes da data limite.
- Na data limite, a aplicação tenta transferir automaticamente por M-Pesa para o telefone cadastrado do beneficiário, desde que a aplicação esteja em execução. Os gateways disponíveis são simulados e não efetuam transferências financeiras reais.
- O responsável pode enviar um lembrete de contribuição ao membro pendente; o lembrete aparece nas notificações do utilizador.

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
- Conta de e-mail com acesso a um servidor SMTP (necessária apenas para a recuperação de palavra-passe).

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

Localiza a classe `view.Main` e executa a aplicação através do IDE.

Para ativar a recuperação de palavra-passe, configura também as variáveis de ambiente SMTP descritas em [Recuperação de Palavra-passe por E-mail](#-recuperação-de-palavra-passe-por-e-mail).

### 4. Começar a utilizar

1. Regista uma conta e inicia sessão.
2. Cria um grupo de poupança.
3. Adiciona participantes registados.
4. Abre um ciclo com pelo menos dois participantes.
5. Regista as contribuições e consulta os dados disponíveis na aplicação.

### Dados de demonstração

Para preparar uma base de dados local para apresentação, execute na raiz do projeto:

```bash
mvn -q org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=service.DemoDataSeeder
```

Para confirmar que a carga foi persistida, execute:

```bash
mvn -q org.codehaus.mojo:exec-maven-plugin:3.5.0:java -Dexec.mainClass=service.DemoDataSeeder -Dexec.args=--verify
```

A carga é aditiva e pode ser executada apenas uma vez: se já existir a conta `demo.admin`,
o processo termina sem alterar os dados. As contas de demonstração usam a palavra-passe
`Lulerk@12`; os e-mails terminam em `example.test` e não são endereços de entrega reais.
Os nomes são fictícios e as imagens KYC são marcadas como dados de demonstração.

Contas para apresentação:

| Utilizador | Cenário |
|---|---|
| `demo.admin` | Acesso ao painel administrativo. |
| `demo.amelia` | Responsável do grupo ativo e notificações de atrasos. |
| `demo.ernesto` | Contribuição pendente, grupo público e notificações. |
| `demo.lidia` | Contribuição em atraso, membro e responsável de grupo com convites. |
| `demo.joaquim` | Membro de ciclo concluído, com strike registado. |
| `demo.sara` | Verificação KYC pendente com imagens fictícias e convites recebidos. |
| `demo.paulo` | Conta desativada. |
| `demo.rosa` | Verificação KYC recusada e convite recusado. |
| `demo.david` | Conta sem KYC submetido e convite recebido. |
| `demo.mateus` | Conta com strikes e pontualidade negativa. |
| `demo.helena` | Responsável de grupo público e ciclo concluído. |

Todos usam a palavra-passe de demonstração indicada acima. A base recebe quatro grupos
(ativo com pagamentos em estados diferentes, público aberto, convites pendentes e ciclo
concluído), denúncias abertas e resolvida, notificações e um ciclo com histórico financeiro.
Não use estas credenciais fora da apresentação nem numa base de dados de produção.

## 📧 Recuperação de Palavra-passe por E-mail

A recuperação de palavra-passe é feita por e-mail: a aplicação envia um código de 6 dígitos através de um servidor SMTP, configurado por variáveis de ambiente. O envio utiliza **Jakarta Mail**, com autenticação e **STARTTLS**.

### Como utilizar

1. No ecrã de início de sessão, seleciona **Esqueceu a senha?**.
2. Introduz o e-mail associado à conta. O endereço tem de corresponder ao e-mail registado.
3. Recebe uma mensagem com um código de 6 dígitos, válido durante **10 minutos**.
4. Introduz o código e define a nova palavra-passe, com pelo menos **8 caracteres**.
5. Inicia sessão com a nova palavra-passe.

O código é guardado apenas em memória, associado ao e-mail, e é removido depois de ser utilizado. Se a aplicação for encerrada ou for pedido um novo código, o código anterior deixa de ser válido.

### Variáveis de ambiente

O serviço SMTP é configurado por variáveis de ambiente, para evitar guardar credenciais no código-fonte.

| Variável | Obrigatória | Predefinição | Descrição |
|---|---|---|---|
| `IXITIQUE_SMTP_HOST` | Sim | — | Endereço do servidor SMTP, por exemplo `smtp.gmail.com`. |
| `IXITIQUE_SMTP_PORT` | Não | `587` | Porta SMTP. A aplicação ativa STARTTLS e exige autenticação. |
| `IXITIQUE_SMTP_USER` | Sim | — | Utilizador usado para autenticar no servidor SMTP. |
| `IXITIQUE_SMTP_PASSWORD` | Sim | — | Palavra-passe SMTP ou palavra-passe de aplicação, conforme o fornecedor. |
| `IXITIQUE_SMTP_FROM` | Não | Valor de `IXITIQUE_SMTP_USER` | Endereço apresentado como remetente da mensagem. |

Se faltar uma variável obrigatória, a aplicação apresenta uma mensagem a indicar qual deve ser configurada. A porta escolhida tem de suportar **STARTTLS** (normalmente a `587`); ligações SSL diretas na porta `465` não são suportadas por esta configuração.

### Configurar no IntelliJ IDEA

1. Abre **Run > Edit Configurations...**.
2. Seleciona a configuração de execução da classe `view.Main`.
3. No campo **Environment variables**, adiciona as variáveis SMTP.
4. Aplica a configuração e executa a aplicação.

As variáveis definidas no IDE ficam disponíveis para a aplicação sem ser necessário alterar o código.

### Configurar no terminal

Em Linux ou macOS, define as variáveis antes de iniciar a aplicação:

```bash
export IXITIQUE_SMTP_HOST="smtp.gmail.com"
export IXITIQUE_SMTP_PORT="587"
export IXITIQUE_SMTP_USER="exemplo@gmail.com"
export IXITIQUE_SMTP_PASSWORD="palavra-passe-de-aplicacao"
export IXITIQUE_SMTP_FROM="exemplo@gmail.com"
```

Em Windows (PowerShell):

```powershell
$env:IXITIQUE_SMTP_HOST = "smtp.gmail.com"
$env:IXITIQUE_SMTP_PORT = "587"
$env:IXITIQUE_SMTP_USER = "exemplo@gmail.com"
$env:IXITIQUE_SMTP_PASSWORD = "palavra-passe-de-aplicacao"
$env:IXITIQUE_SMTP_FROM = "exemplo@gmail.com"
```

Estas variáveis aplicam-se apenas à sessão atual do terminal. Se o IDE já estiver aberto, inicia-o a partir desse mesmo terminal para que herde a configuração, ou define as variáveis diretamente na configuração de execução.

### Exemplo com Gmail

1. Ativa a verificação em duas etapas na conta Google.
2. Em **Segurança**, cria uma **palavra-passe de aplicação**.
3. Usa `smtp.gmail.com` como `IXITIQUE_SMTP_HOST`, a porta `587` e a palavra-passe de aplicação gerada em `IXITIQUE_SMTP_PASSWORD`.

Não uses a palavra-passe normal da conta Google: o Gmail exige uma palavra-passe de aplicação para autenticação SMTP. Outros fornecedores podem ter requisitos próprios, como palavras-passe de aplicação ou autenticação OAuth.

### Resolução de problemas

| Mensagem ou sintoma | Causa provável | Resolução |
|---|---|---|
| `Configure a variável de ambiente IXITIQUE_SMTP_HOST.` (ou outra variável) | A variável não está definida ou o processo não a recebeu. | Define a variável na configuração de execução do IDE ou no terminal e inicia a aplicação novamente. |
| `Não foi possível enviar o email. Verifique a configuração SMTP.` | Credenciais inválidas, servidor ou porta incorretos, STARTTLS indisponível ou bloqueio do fornecedor. | Confirma o host, a porta e as credenciais; no Gmail, usa uma palavra-passe de aplicação. |
| `Não existe uma conta com esse email.` | O e-mail não corresponde a nenhuma conta registada. | Usa o e-mail exatamente como foi registado na conta. |
| `Código inválido ou expirado. Peça um novo código.` | Código incorreto, passaram mais de 10 minutos, a aplicação foi reiniciada ou foi pedido um código novo. | Pede um novo código e introduz o mais recente. |
| O e-mail não chega à caixa de entrada. | A mensagem foi marcada como spam ou o servidor rejeitou o remetente. | Verifica a pasta de spam e os filtros; confirma `IXITIQUE_SMTP_FROM` e os registos do fornecedor. |

### Segurança

- Não coloques credenciais reais no código-fonte nem no repositório; usa variáveis de ambiente.
- Prefere palavras-passe de aplicação e mantém-nas fora de ficheiros versionados.
- O código de recuperação é guardado com hash, expira após 10 minutos e é invalidado depois de utilizado.

## 🔐 Organização de Acessos

O sistema distingue os papéis de acesso à aplicação das responsabilidades associadas à participação num grupo.

- **Administrador:** papel de acesso ao sistema, de acordo com as permissões implementadas.
- **Líder do grupo:** responsável pela gestão do grupo dentro das permissões atribuídas.
- **Membro:** participa no grupo e acompanha as operações permitidas.

As permissões devem respeitar o contexto do grupo e as regras de acesso implementadas. A simulação de pagamentos não implica a existência de um papel de tesoureiro.

## 👩🏽‍💻 Equipa

O Konexion é um projeto desenvolvido em equipa.

<div align="center" id="equipa">

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
