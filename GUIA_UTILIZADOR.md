# Guia de Utilizador — Konexion

**Konexion / iXitique** é uma aplicação desktop para organizar grupos de poupança rotativa, inspirados no xitique. Este guia explica como instalar, executar e utilizar a aplicação passo a passo.

> **Importante:** os pagamentos da aplicação são **simulados**. Nenhum valor real é movimentado, nem existe integração com operadores de pagamento reais. A aplicação foi concebida no contexto académico do projeto.

## Índice

- [1. Antes de começar](#1-antes-de-começar)
- [2. Instalar e executar a aplicação](#2-instalar-e-executar-a-aplicação)
  - [2.1 Obter o código](#21-obter-o-código)
  - [2.2 Criar a base de dados e o utilizador](#22-criar-a-base-de-dados-e-o-utilizador)
  - [2.3 Preparar os dados iniciais](#23-preparar-os-dados-iniciais)
  - [2.4 Configurar a recuperação de palavra-passe](#24-configurar-a-recuperação-de-palavra-passe)
  - [2.5 Iniciar a aplicação](#25-iniciar-a-aplicação)
- [3. Primeiro acesso](#3-primeiro-acesso)
  - [3.1 Criar uma conta](#31-criar-uma-conta)
  - [3.2 Iniciar sessão](#32-iniciar-sessão)
  - [3.3 Recuperar a palavra-passe](#33-recuperar-a-palavra-passe)
  - [3.4 Contas de demonstração](#34-contas-de-demonstração)
- [4. Conhecer a interface](#4-conhecer-a-interface)
- [5. Verificação KYC](#5-verificação-kyc)
- [6. Grupos de xitique](#6-grupos-de-xitique)
  - [6.1 Criar um grupo](#61-criar-um-grupo)
  - [6.2 Convidar membros](#62-convidar-membros)
  - [6.3 Entrar num grupo público](#63-entrar-num-grupo-público)
  - [6.4 Reordenar a fila de recebimento](#64-reordenar-a-fila-de-recebimento)
  - [6.5 Abrir um ciclo](#65-abrir-um-ciclo)
  - [6.6 Pagar a contribuição](#66-pagar-a-contribuição)
  - [6.7 Transferir o saldo e fechar o ciclo](#67-transferir-o-saldo-e-fechar-o-ciclo)
  - [6.8 Gerir membros e o grupo](#68-gerir-membros-e-o-grupo)
- [7. Visão geral e histórico](#7-visão-geral-e-histórico)
- [8. Perfil](#8-perfil)
- [9. Notificações](#9-notificações)
- [10. Ajuda e suporte](#10-ajuda-e-suporte)
- [11. Painel de administração](#11-painel-de-administração)
- [12. Regras e prazos](#12-regras-e-prazos)
- [13. Resolução de problemas](#13-resolução-de-problemas)
- [14. Limitações e boas práticas](#14-limitações-e-boas-práticas)
- [15. Repor a base de dados](#15-repor-a-base-de-dados)
- [16. Checklist de demonstração](#16-checklist-de-demonstração)
- [Glossário](#glossário)

---

## 1. Antes de começar

### 1.1 Requisitos

| Requisito | Detalhe |
|---|---|
| **JDK 25** | O projeto está configurado para Java 25. Confirma a versão em `pom.xml` (`<release>25</release>`). |
| **MySQL Server 8.x** | A aplicação usa MySQL através de Hibernate/JPA. O dump local foi gerado com MySQL 8.0.46. |
| **IntelliJ IDEA** | Recomendado, porque o projeto usa JavaFX e dependências Maven. Também podes usar outro IDE com suporte para JavaFX. |
| **Maven** | Necessário para descarregar as dependências. O IntelliJ traz uma integração Maven; também podes usar o Maven instalado no sistema. |
| **Conta SMTP** | Opcional. Só é necessária para a recuperação de palavra-passe por e-mail. |

Não é necessário instalar o JavaFX à parte: as dependências JavaFX são resolvidas pelo Maven. Se o IDE pedir uma configuração adicional de JavaFX, consulta a secção [13. Resolução de problemas](#13-resolução-de-problemas).

### 1.2 O que vais precisar de saber

- **Xitique:** grupo de poupança rotativa em que os membros contribuem com uma quota e, em cada ciclo, um membro recebe o valor acumulado.
- **Quota:** valor que cada membro paga por ciclo.
- **Ciclo:** uma rodada de contribuições com um beneficiário definido.
- **Bolo:** expressão usada na aplicação para o saldo acumulado do ciclo/grupo.
- **Responsável do grupo:** quem criou o grupo. Pode convidar membros, reordenar a fila, abrir ciclos e transferir o saldo.
- **Administrador:** conta com acesso ao painel de administração. Não pode criar nem participar em grupos.

---

## 2. Instalar e executar a aplicação

### 2.1 Obter o código

Clona o repositório e abre a pasta do projeto no IntelliJ IDEA:

```bash
git clone https://github.com/luisnhabangue/iXitique.git
```

Se já tens o projeto localmente, abre diretamente a pasta `iXitique`.

### 2.2 Criar a base de dados e o utilizador

1. Inicia o servidor MySQL.
2. Abre uma consola MySQL como administrador:

   ```bash
   mysql -u root -p
   ```

3. Cria a base de dados e o utilizador usado pela aplicação. O exemplo abaixo usa os valores atuais do projeto; se alterares algum valor, atualiza também o ficheiro `src/main/resources/META-INF/persistence.xml`.

   ```sql
   CREATE DATABASE IF NOT EXISTS ixitique
     CHARACTER SET utf8mb4
     COLLATE utf8mb4_0900_ai_ci;

   CREATE USER IF NOT EXISTS 'ixitique'@'localhost'
     IDENTIFIED BY 'deumaoito';

   GRANT ALL PRIVILEGES ON ixitique.* TO 'ixitique'@'localhost';
   FLUSH PRIVILEGES;
   ```

4. Confirma os valores de ligação em `src/main/resources/META-INF/persistence.xml`:

   | Propriedade | Valor de exemplo |
   |---|---|
   | URL | `jdbc:mysql://localhost:3306/ixitique?...` |
   | Utilizador | `ixitique` |
   | Palavra-passe | `deumaoito` |

   O URL completo inclui `allowPublicKeyRetrieval=true`, `useSSL=false` e `serverTimezone=Africa/Maputo`. Se o teu MySQL usar outra porta, outro utilizador ou outra palavra-passe, ajusta estes valores.

> **Nota:** não uses a conta `root` na aplicação. Cria um utilizador próprio, como no exemplo, com permissões apenas sobre a base de dados `ixitique`.

### 2.3 Preparar os dados iniciais

Podes começar com uma base de dados vazia e criar uma conta pela aplicação. Para uma apresentação com contas e grupos já preparados, importa o dump local, se estiver disponível:

```bash
mysql -u root -p < database/ixitique_dump.sql
```

O dump:

- cria a base de dados `ixitique`, se ainda não existir;
- recria as tabelas com `DROP TABLE` + `CREATE TABLE` (**substitui os dados existentes**);
- insere os dados guardados no ficheiro.

> **Atenção:** a importação substitui os dados existentes. Confirma que estás a usar uma base local e faz uma cópia de segurança antes de continuar. Não partilhes um dump que contenha dados pessoais ou imagens KYC.

Se o import falhar por causa de caracteres ou de collation, confirma que estás a usar MySQL 8 e importa com:

```bash
mysql --default-character-set=utf8mb4 -u root -p < database/ixitique_dump.sql
```

### 2.4 Configurar a recuperação de palavra-passe

Este passo é opcional. Sem SMTP configurado, o resto da aplicação funciona, mas o link **Esqueceu a senha?** não consegue enviar o código.

A aplicação lê as seguintes variáveis de ambiente:

| Variável | Obrigatória | Predefinição | Descrição |
|---|---|---|---|
| `IXITIQUE_SMTP_HOST` | Sim | — | Servidor SMTP, por exemplo `smtp.gmail.com`. |
| `IXITIQUE_SMTP_PORT` | Não | `587` | Porta SMTP. A aplicação usa autenticação e **STARTTLS**. |
| `IXITIQUE_SMTP_USER` | Sim | — | Utilizador de autenticação SMTP. |
| `IXITIQUE_SMTP_PASSWORD` | Sim | — | Palavra-passe SMTP ou palavra-passe de aplicação. |
| `IXITIQUE_SMTP_FROM` | Não | Valor de `IXITIQUE_SMTP_USER` | Endereço do remetente. |

No IntelliJ:

1. Abre **Run > Edit Configurations...**.
2. Seleciona a configuração de execução de `view.Main`.
3. Em **Environment variables**, adiciona as variáveis SMTP.
4. Aplica e executa.

Em Linux/macOS, podes defini-las antes de iniciar:

```bash
export IXITIQUE_SMTP_HOST="smtp.gmail.com"
export IXITIQUE_SMTP_PORT="587"
export IXITIQUE_SMTP_USER="exemplo@gmail.com"
export IXITIQUE_SMTP_PASSWORD="a-tua-palavra-passe-de-aplicacao"
export IXITIQUE_SMTP_FROM="exemplo@gmail.com"
```

> **Gmail:** ativa a verificação em dois passos e cria uma **palavra-passe de aplicação**. A aplicação usa STARTTLS na porta `587`; ligações SSL diretas na porta `465` não estão configuradas. Para mais detalhes, consulta a secção de recuperação de palavra-passe no `README.md`.

### 2.5 Iniciar a aplicação

1. No IntelliJ, confirma que o projeto está configurado com **JDK 25**.
2. Atualiza as dependências Maven (**Maven > Reload Project**).
3. Abre `src/view/Main.java` e executa a classe `view.Main`.
4. A janela de início de sessão deve aparecer.

Na primeira execução, o Hibernate cria ou atualiza as tabelas da base de dados (`hibernate.hbm2ddl.auto=update`). Se a ligação estiver correta e a base estiver vazia, a aplicação abre na mesma; cria uma conta pela opção **Criar conta**. Para usar contas já preparadas, importa primeiro o dump indicado na secção [2.3](#23-preparar-os-dados-iniciais).

> **Encerrar:** usa **Terminar sessão** para sair da conta e fecha a janela para encerrar a aplicação. O monitor de prazos e as tentativas de pagamento automático só funcionam enquanto a aplicação estiver aberta.

---

## 3. Primeiro acesso

### 3.1 Criar uma conta

No ecrã de início de sessão, seleciona **Criar conta**. O registo tem dois passos.

#### Passo 1 — Dados pessoais

| Campo | O que introduzir |
|---|---|
| **Primeiros nomes** | Nome próprio. Usa 2 a 60 letras; espaços, apóstrofos e hífenes são permitidos. |
| **Apelidos** | Apelido, com as mesmas regras. |
| **NUIT / BI (opcional)** | Podes deixar em branco e preencher depois no KYC. Se preencheres: BI com **12 dígitos + 1 letra** ou NUIT com **9 dígitos**. |
| **Género** | Seleciona uma opção. |
| **Data de nascimento** | A idade tem de estar entre **15 e 90 anos**. |
| **Telefone (M-Pesa / E-Mola)** | Número moçambicano com 9 dígitos e prefixo `82`, `83`, `84`, `85`, `86` ou `87`. O `+258` é opcional. |

Seleciona **Continuar →**. Se faltar algum campo obrigatório, a aplicação mostra uma mensagem e não avança.

#### Passo 2 — Credenciais

| Campo | Regra |
|---|---|
| **Nome de utilizador** | 4 a 20 caracteres; começa por uma letra; usa letras, números, pontos ou underscores. Não uses `..`. É convertido para minúsculas. |
| **Email** | Endereço válido e único. |
| **Senha** | 8 a 64 caracteres, com pelo menos uma maiúscula, uma minúscula, um número e um símbolo. |
| **Confirmar senha** | Tem de coincidir com a senha. |

Lê os **Termos e Condições** e marca a caixa de aceitação. Sem essa marcação, a conta não é criada.

Depois de selecionares **Criar conta**:

- a conta fica **ativa**, mas **por verificar**;
- podes participar em grupos e pagar contribuições;
- para **criar grupos**, precisas de aprovação KYC por um administrador;
- a aplicação volta ao ecrã de início de sessão.

### 3.2 Iniciar sessão

No ecrã **Bem-vindo de volta**:

1. Introduz o **username ou e-mail** no primeiro campo. Apesar do rótulo, o código também aceita um **número de telefone** moçambicano.
2. Introduz a **senha**.
3. Seleciona **Entrar →**.

Após a autenticação:

- um utilizador normal entra na **Visão geral**;
- um administrador entra no **Painel de administração**.

Se a conta estiver desativada, o início de sessão é recusado. Nesse caso, pede a um administrador para reativar a conta.

### 3.3 Recuperar a palavra-passe

1. No ecrã de início de sessão, seleciona **Esqueceu a senha?**.
2. Introduz o **e-mail associado à conta**.
3. Recebe um **código de 6 dígitos** por e-mail.
4. Introduz o código no diálogo **Verificar código**.
5. Define a nova senha (mínimo **8 caracteres**) e confirma.
6. Volta a iniciar sessão com a nova senha.

Regras importantes:

- O código é válido durante **10 minutos**.
- O código é guardado apenas **em memória**. Se fechares a aplicação, o código deixa de ser válido.
- Pedir um novo código invalida o código anterior.
- O código só é enviado se o e-mail existir na base de dados.
- É necessário ter o SMTP configurado (secção [2.4](#24-configurar-a-recuperação-de-palavra-passe)).
- As contas de demonstração usam e-mails terminados em `.test`, que não recebem mensagens reais. Para testar a recuperação, cria uma conta com um e-mail real.

### 3.4 Contas de demonstração

As contas seguintes ficam disponíveis depois de importares o dump local. **Todas usam a palavra-passe `Lulerk@12`.**

| Utilizador | Cenário |
|---|---|
| `demo.admin` | Acesso ao painel administrativo. |
| `demo.amelia` | Responsável do grupo ativo e com notificações. |
| `demo.ernesto` | Contribuição pendente no grupo ativo, grupo público e notificações. |
| `demo.lidia` | Contribuição em atraso, membro e responsável de grupo com convites. |
| `demo.joaquim` | Membro de ciclo concluído, com strike registado. |
| `demo.sara` | Verificação KYC pendente, com imagens fictícias, e convites recebidos. |
| `demo.paulo` | Conta desativada. |
| `demo.rosa` | Verificação KYC recusada e convite recusado. |
| `demo.david` | Conta sem KYC submetido e convite recebido. |
| `demo.mateus` | Conta com strikes e pontualidade negativa. |
| `demo.helena` | Responsável de grupo público e ciclo concluído. |

Os nomes, e-mails e imagens são fictícios. Não uses estas credenciais fora de uma demonstração.

---

## 4. Conhecer a interface

Depois de iniciares sessão, a aplicação apresenta uma barra lateral, um cabeçalho e uma área de conteúdo.

### 4.1 Menu lateral — utilizador normal

| Item | Função |
|---|---|
| **Visão geral** | Resumo das poupanças, próximos recebimentos, grupos e atividade recente. |
| **Meus Xitiques** | Lista dos teus grupos, filtros e descoberta de grupos públicos. |
| **Histórico** | Contribuições registadas e exportação em CSV. |
| **Meu perfil** | Dados da conta, edição de perfil, KYC e histórico pessoal. |
| **Precisa de ajuda?** | Abre o Centro de Ajuda. |
| **Terminar sessão** | Encerra a sessão e volta ao ecrã de início de sessão. |

### 4.2 Cabeçalho

- **Pesquisar grupos, transações...**: pesquisa grupos por nome. O atalho **Ctrl+K** coloca o cursor na pesquisa.
- **Criar grupo**: atalho para criar um novo grupo. A criação só é permitida a utilizadores com KYC aprovado; sem verificação, a aplicação recusa a operação.
- **Campainha**: notificações e convites pendentes. O número vermelho indica a quantidade.
- **Nome e papel**: mostra o utilizador autenticado e o respetivo papel.

### 4.3 Menu lateral — administrador

O administrador vê um menu diferente:

| Item | Função |
|---|---|
| **Painel de administração** | Métricas, gráficos e resumo da plataforma. |
| **Relatórios** | Exportação financeira em CSV/PDF e membros com mais atrasos. |
| **Regras globais** | Penalizações, lembretes e notificações. |
| **Denúncias** | Análise de denúncias e aplicação de strikes. |
| **Verificação KYC** | Aprovação ou recusa de documentos. |
| **Utilizadores** | Perfis, reposição de palavra-passe, ativação e eliminação. |
| **Grupos** | Consulta e arquivo de grupos. |
| **Suporte** | Resposta a pedidos de suporte. |
| **Terminar sessão** | Encerra a sessão. |

O administrador **não** vê as opções de grupos, histórico, perfil ou notificações. Também **não pode criar nem participar em grupos**.

---

## 5. Verificação KYC

A verificação KYC (Know Your Customer) confirma a identidade do utilizador. É necessária para **criar grupos**; não impede participar em grupos existentes.

1. Abre **Meu perfil**.
2. Na secção **Verificação de identidade (KYC)**, confirma ou introduz o número do documento.
3. Seleciona **Foto do documento** e escolhe a imagem do BI ou NUIT.
4. Seleciona **Selecionar fotografia** e escolhe uma selfie de confirmação.
5. Seleciona **Enviar para aprovação**.

Requisitos das imagens:

- formatos `png`, `jpg`, `jpeg` ou `webp`;
- máximo de **8 MB** por imagem;
- são necessárias **as duas imagens** (documento e selfie).

Estados possíveis:

| Estado | Significado |
|---|---|
| **POR VERIFICAR** | Ainda não foi submetido KYC ou a conta não está verificada. |
| **EM ANÁLISE** | O pedido foi enviado e aguarda decisão do administrador. |
| **VERIFICADO** | O KYC foi aprovado. A conta pode criar grupos. |
| **RECUSADO · REENVIE** | O pedido foi recusado. Podes corrigir os dados/imagens e submeter novamente. |

Enquanto o KYC está pendente, podes continuar a participar em grupos, aceitar convites e pagar contribuições. A criação de grupos fica bloqueada até à aprovação.

---

## 6. Grupos de xitique

### 6.1 Criar um grupo

Só um utilizador **ativo**, **verificado por KYC** e **não administrador** pode criar grupos.

1. Seleciona **Criar grupo** no cabeçalho ou em **Meus Xitiques**.
2. Preenche o formulário:

| Campo | Regras |
|---|---|
| **Nome do grupo** | 3 a 80 caracteres. O nome tem de ser único. |
| **Descrição** | Obrigatória, até 150 caracteres. |
| **Valor por membro (MT)** | Quota por ciclo; tem de ser maior do que zero. |
| **Dia de pagamento** | Dia do mês entre 1 e 31. |
| **Frequência** | Diário (1 dia), Semanal (7 dias), Quinzenal (15 dias) ou Mensal (30 dias). |
| **Total de membros (incluindo o criador)** | Mínimo de 2 membros. |
| **Ordem de recebimento** | **Manual** ou **Aleatória**. |

3. Consulta a estimativa apresentada: cada ciclo arrecada a quota multiplicada pelo número de membros; ao longo dos ciclos, cada membro recebe uma vez.
4. Seleciona **Criar grupo**.

O criador passa a ser:

- o **primeiro membro** do grupo;
- o **responsável** do grupo;
- a **primeira posição** da fila de recebimento.

O grupo é criado como **privado** por predefinição. O responsável pode torná-lo público mais tarde.

### 6.2 Convidar membros

1. Abre o grupo em **Meus Xitiques**.
2. Seleciona **Adicionar membro**.
3. Pesquisa o utilizador por **username** ou **NUIT**.
4. Confirma a posição na fila e seleciona **Adicionar membro**.

O botão envia um **convite**, não adiciona imediatamente o utilizador. O convite fica pendente na lista de membros e reserva uma vaga enquanto não for respondido. O responsável pode selecionar **Revogar** para cancelar um convite pendente.

O convidado deve:

1. Iniciar sessão na sua conta.
2. Abrir a **campainha**.
3. Selecionar **Aceitar** ou **Recusar**.

Se aceitar, entra no fim da fila de recebimento. Se o grupo ficar completo e a ordem for **Aleatória**, o sistema sorteia a fila e inicia automaticamente o primeiro ciclo.

Restrições:

- só o responsável (ou um administrador) pode convidar;
- não é possível convidar depois de o primeiro ciclo ter começado;
- não é possível convidar se o grupo já estiver cheio;
- um utilizador não pode entrar duas vezes no mesmo grupo;
- administradores não podem participar em grupos;
- convites pendentes contam para a capacidade do grupo.

### 6.3 Entrar num grupo público

Na página **Meus Xitiques**, a secção **Descobrir grupos públicos** lista grupos públicos. O botão **Participar** fica indisponível quando o grupo já iniciou ciclos ou não tem vaga.

1. Seleciona **Participar** no grupo pretendido.
2. A aplicação confirma a entrada e atualiza a lista.

Só é possível entrar se:

- o grupo for **público**;
- o grupo ainda **não tiver iniciado ciclos**;
- houver capacidade disponível;
- o utilizador não for administrador e não pertencer já ao grupo.

Grupos privados só aceitam membros por convite. A verificação KYC **não** é exigida para entrar num grupo público ou aceitar um convite.

O responsável pode alternar a visibilidade com **Tornar público** / **Tornar privado** no detalhe do grupo.

### 6.4 Reordenar a fila de recebimento

A reordenação só está disponível quando:

- o grupo usa a ordem **Manual**;
- ainda não começou nenhum ciclo;
- o grupo tem mais de um membro.

No detalhe do grupo, seleciona **Reordenar fila** (ou **Reordenar**). Escolhe o membro que deve passar para o **1.º lugar**. A operação troca as posições necessárias para colocar esse membro em primeiro.

Na ordem **Aleatória**, a fila é definida automaticamente pelo sistema quando o grupo atinge a capacidade; não é possível reordenar manualmente.

### 6.5 Abrir um ciclo

Um ciclo só pode começar quando:

- o grupo tiver **a totalidade dos membros** definida na criação;
- não existir outro ciclo ativo;
- o saldo do ciclo anterior estiver a zero (já transferido);
- existir pelo menos um membro elegível para receber.

#### Ordem aleatória

Quando o grupo atinge a capacidade, o sistema:

1. sorteia a ordem de recebimento;
2. cria automaticamente o primeiro ciclo;
3. define o primeiro membro da fila como beneficiário.

#### Ordem manual

Depois de o grupo estar completo, o responsável vê o botão **Abrir novo ciclo**. Ao selecioná-lo, o sistema escolhe o **próximo membro elegível da fila** como beneficiário. Se quiseres outro membro em primeiro, reordena a fila **antes** de abrir o ciclo.

Regras do ciclo:

- só pode existir **um ciclo ativo** de cada vez;
- a data de fim é calculada a partir da data de início e da frequência;
- o beneficiário é o destinatário do saldo acumulado, que é transferido depois de o ciclo ser concluído;
- cada membro recebe **uma vez** por rotação;
- quando todos os membros já receberam, não há mais ciclos.

### 6.6 Pagar a contribuição

1. Abre o grupo em **Meus Xitiques**.
2. Com um ciclo ativo, seleciona **Registrar pagamento**.
3. No diálogo **Simulação de Pagamento**, confirma:
   - o valor a contribuir (igual à quota do grupo);
   - o ciclo;
   - o grupo;
   - o número de telefone (preenchido com o teu número, se existir).
4. Escolhe **M-Pesa** ou **e-Mola**.
5. Seleciona **Simular Pagamento →**.

O processamento demora cerca de **800 ms** e devolve uma referência em caso de sucesso. O pagamento é simulado e **não movimenta dinheiro real**.

Regras:

- cada membro paga **uma vez por ciclo**;
- o valor pago é a quota definida no grupo;
- a data de pagamento é a data atual;
- o pagamento fica registado na tabela de contribuições do ciclo;
- se todos os membros pagarem, o ciclo é concluído automaticamente.

#### Regras da simulação de pagamento

| Método | Número aceite |
|---|---|
| **M-Pesa** | 9 dígitos, começa por `84` ou `85` |
| **e-Mola** | 9 dígitos, começa por `86` ou `87` |

Para testar, evita números terminados em:

- **0** — a simulação devolve **“Saldo insuficiente”**;
- **9** — a simulação devolve **“Tempo de resposta excedido”**.

Um número como `841234567` (M-Pesa) ou `861234567` (e-Mola) termina em `7` e tende a ser aprovado.

#### Pontualidade

- Pagar **até à data limite** dá **+10 pontos** de pontualidade.
- Pagar depois do prazo pode descontar os pontos definidos nas regras globais (por predefinição, **10 pontos**).
- Os pontos e as penalizações são registados pelo serviço de prazos enquanto a aplicação estiver aberta.

### 6.7 Transferir o saldo e fechar o ciclo

Quando todos os membros pagam, o ciclo passa a **CONCLUÍDO** e o beneficiário fica marcado como **Já recebeu**. O saldo acumulado fica disponível no grupo.

O responsável pode então:

1. Selecionar **Transferir saldo ao beneficiário**.
2. Escolher **M-Pesa** ou **e-Mola**.
3. Confirmar ou corrigir o número do beneficiário.
4. Confirmar a transferência.

A transferência também é simulada. Se for bem-sucedida:

- o saldo do grupo volta a zero;
- o beneficiário recebe uma notificação;
- o sistema cria automaticamente o **ciclo seguinte**, se houver um próximo membro elegível.

Se todos os membros já receberam, não é criado outro ciclo.

#### Transferência automática

A aplicação tenta transferir automaticamente o saldo quando:

- o ciclo está **concluído**;
- a data limite já foi atingida;
- a aplicação está **aberta**;
- o beneficiário tem um número de telefone;
- ainda não houve tentativa de transferência para esse ciclo.

Se a transferência automática falhar, o responsável do grupo recebe uma notificação e pode tentar manualmente. A transferência manual continua disponível no detalhe do grupo.

> **Importante:** depois de receber o saldo, o grupo só aceita um novo ciclo quando o saldo estiver a zero. Se a transferência não for feita, o botão **Abrir novo ciclo** não aparece.

### 6.8 Gerir membros e o grupo

No detalhe do grupo, o responsável dispõe de ações adicionais. As condições são aplicadas pela aplicação.

| Ação | Quem pode | Quando |
|---|---|---|
| **Adicionar membro** (convite) | Responsável | Antes do primeiro ciclo e com vaga. |
| **Revogar** convite | Responsável | Convite pendente. |
| **Remover** membro | Responsável | Antes do primeiro ciclo; o criador não pode ser removido. |
| **Lembrar** | Responsável | Ciclo ativo e membro ainda sem pagamento. |
| **Denunciar** | Qualquer membro | Sobre outro membro do mesmo grupo; motivo com pelo menos 10 caracteres. |
| **Tornar público / Tornar privado** | Responsável | A qualquer momento. |
| **Eliminar grupo** | Responsável | Antes de qualquer ciclo; remove o grupo definitivamente. |
| **Reordenar fila** | Responsável | Ordem manual e antes do primeiro ciclo. |
| **Transferir saldo** | Responsável | Ciclo concluído, todos pagaram e existe saldo. |

Notas:

- um membro que não seja responsável **não tem**, nesta versão, um botão para sair do grupo; deve pedir ao responsável para o remover antes do primeiro ciclo;
- o criador não pode ser removido enquanto for responsável;
- não é possível remover membros depois de o histórico de ciclos começar;
- a eliminação do grupo também fica bloqueada depois do primeiro ciclo, para preservar o histórico.

---

## 7. Visão geral e histórico

### 7.1 Visão geral

A página **Visão geral** apresenta:

- **Total investido:** soma das contribuições pagas pelo utilizador.
- **Próximo recebimento:** soma potencial dos ciclos ativos em que o utilizador é o beneficiário (quota × número de membros). Se não for beneficiário de nenhum ciclo ativo, mostra `0,00 MT`.
- **Xitiques ativos:** quantidade de grupos a que o utilizador pertence.
- **Evolução das minhas poupanças:** gráfico dos últimos seis meses, com o total acumulado das contribuições pagas.
- **Meus grupos de Xitique:** até três grupos, com ciclo, progresso e próximo pagamento.
- **Atividade recente:** últimas contribuições registadas.

Os atalhos **Ver todos** e **Ver todas** abrem, respetivamente, **Meus Xitiques** e **Histórico**.

### 7.2 Meus Xitiques

A lista de grupos permite:

- pesquisar por nome;
- filtrar por **Todos**, **Ativos** ou **Concluídos**;
- abrir o detalhe de um grupo;
- descobrir e aderir a grupos públicos.

Cada cartão mostra o nome, número de membros, ciclo e progresso de pagamentos.

### 7.3 Histórico

A página **Histórico** lista as contribuições do utilizador, com:

- data;
- grupo;
- valor;
- estado.

Seleciona **Exportar CSV** para guardar o histórico num ficheiro CSV. O ficheiro é criado no local escolhido no diálogo de gravação.

---

## 8. Perfil

Em **Meu perfil** podes consultar:

- iniciais e nome;
- estado da conta;
- estado da verificação KYC;
- username, telefone, e-mail e documento.

### 8.1 Editar perfil

Seleciona **Editar perfil** para alterar:

- primeiro nome;
- apelido;
- número de telefone.

O username, o e-mail e o documento são apresentados apenas para consulta nesta caixa de diálogo. Guarda as alterações para atualizar os dados.

### 8.2 Histórico pessoal

O perfil tem duas abas:

- **Histórico de contribuições** — contribuições do utilizador;
- **Xitiques finalizados** — grupos concluídos.

### 8.3 KYC

A secção de KYC permite submeter ou reenviar documentos, conforme descrito na secção [5](#5-verificação-kyc).

---

## 9. Notificações

A campainha do cabeçalho está disponível para utilizadores normais (não para administradores).

Ao abri-la, vês:

- **convites pendentes**, com botões **Aceitar** e **Recusar**;
- **notificações** de prazos, atrasos, pagamentos e transferências.

Cada notificação pode ter:

- um botão para abrir o grupo relacionado;
- um botão para marcar como lida.

O contador vermelho mostra a soma de convites pendentes e notificações não lidas. A aplicação atualiza o contador periodicamente e executa a verificação de prazos enquanto está aberta.

Tipos de avisos que podem aparecer:

- contribuição próxima do prazo;
- contribuição em atraso;
- pagamento efetuado depois do prazo;
- pagamento tardio comunicado ao responsável;
- transferência automática falhada ou concluída;
- convite para um grupo.

---

## 10. Ajuda e suporte

### 10.1 Centro de Ajuda

Seleciona **Precisa de ajuda?** na barra lateral para abrir o **Centro de Ajuda**. A página tem quatro cartões:

| Cartão | Função |
|---|---|
| **Guia de utilização** | Passos resumidos, do registo ao pagamento. |
| **Perguntas frequentes** | Respostas às dúvidas comuns, com pesquisa. |
| **Contactar suporte** | Formulário para enviar um pedido. |
| **Os meus pedidos** | Lista dos pedidos enviados e respetivos estados. |

A pesquisa do Centro de Ajuda procura nas perguntas frequentes. Se não houver resultados, a aplicação sugere contactar o suporte.

### 10.2 Enviar um pedido de suporte

1. Abre **Contactar suporte**.
2. Preenche **Assunto** (até 120 caracteres).
3. Escreve a **Mensagem** (até 2000 caracteres).
4. Seleciona **Enviar pedido**.

O pedido fica com o estado **Aberto**. Um administrador pode respondê-lo e alterar o estado para **Em andamento** ou **Resolvido**.

### 10.3 Consultar pedidos

Em **Os meus pedidos**:

- vês data, assunto e estado;
- clica num pedido para mostrar a mensagem e a resposta do suporte;
- se ainda não houver resposta, aparece **“Ainda não há resposta do suporte.”**.

Estados apresentados ao utilizador:

| Estado | Significado |
|---|---|
| **ABERTO** | Pedido recebido, ainda sem resposta. |
| **EM CURSO** | Pedido em análise. |
| **RESOLVIDO** | Pedido tratado. |

---

## 11. Painel de administração

O administrador inicia sessão com uma conta com papel de administrador (por exemplo, `demo.admin`). O menu lateral muda e dá acesso às ferramentas de gestão.

### 11.1 Painel de administração

O painel apresenta indicadores como:

- utilizadores totais, ativos e verificados;
- grupos ativos e públicos;
- total arrecadado;
- pagamentos confirmados;
- valores pendentes e em atraso;
- denúncias abertas e KYC pendentes;
- gráficos de evolução e distribuição.

### 11.2 Verificação KYC

Em **Verificação KYC**:

1. vês a lista de pedidos pendentes;
2. seleciona **Ver fotos** para consultar o documento e a selfie;
3. seleciona **Aprovar** ou **Recusar**.

Ao aprovar, o utilizador passa a **VERIFICADO** e pode criar grupos. Ao recusar, o utilizador vê **RECUSADO · REENVIE** e pode submeter novamente.

### 11.3 Denúncias

Em **Denúncias**:

- vês o membro denunciado, o grupo e o motivo;
- seleciona **Aplicar strike** para penalizar o utilizador; ou
- seleciona **Resolver sem strike** para encerrar a denúncia sem penalização.

### 11.4 Utilizadores

Em **Utilizadores** podes pesquisar contas e consultar:

- nome, username, papel, estado;
- número de strikes;
- pontos de pontualidade.

Ações disponíveis para contas não administradoras:

| Ação | Efeito |
|---|---|
| **Perfil** | Mostra um resumo dos dados da conta. |
| **Redefinir password** | Gera uma palavra-passe temporária, mostrada **apenas uma vez**. |
| **Desativar** | Impede o início de sessão. |
| **Reativar** | Volta a permitir o início de sessão. |
| **Apagar definitivamente** | Remove a conta, se não estiver associada a grupos. |

Restrições:

- não é possível redefinir a palavra-passe de outro administrador;
- não é possível desativar, reativar ou apagar a própria conta de administrador;
- contas com grupos ou participações não podem ser apagadas; usa **Desativar** para preservar o histórico.

### 11.5 Grupos

Em **Grupos** podes:

- pesquisar por nome ou responsável;
- consultar o detalhe do grupo;
- arquivar grupos **sem ciclo em andamento**.

O arquivo preserva o histórico. Um grupo arquivado deixa de aparecer nas listas normais e não aceita novas operações de participação.

### 11.6 Regras globais

Em **Regras globais** podes definir:

| Regra | Intervalo |
|---|---|
| **Penalização por atraso (pontos)** | 0 a 100 |
| **Lembrete antes do prazo (dias)** | 0 a 30 |
| **Enviar lembretes de prazo** | Ativado/desativado |
| **Notificar responsável sobre atrasos** | Ativado/desativado |
| **Notificar pagamentos tardios** | Ativado/desativado |

Seleciona **Guardar regras** para aplicar. As regras são globais e afetam os grupos da plataforma.

### 11.7 Relatórios

Em **Relatórios**:

1. escolhe a data **Desde** e **Até**;
2. seleciona **Exportar CSV** ou **Exportar PDF**;
3. escolhe onde guardar o ficheiro.

A página também mostra os membros com mais atrasos, incluindo a taxa de atraso e o valor em atraso.

### 11.8 Suporte

Em **Suporte**:

1. seleciona um pedido na lista;
2. consulta a mensagem e a resposta anterior;
3. escreve a resposta;
4. escolhe o estado (**Aberto**, **Em andamento** ou **Resolvido**);
5. seleciona **Guardar resposta e estado**.

O utilizador vê a resposta em **Os meus pedidos**.

---

## 12. Regras e prazos

### 12.1 Conta e registo

- Username, e-mail, telefone e documento têm de ser únicos.
- O username é convertido para minúsculas.
- O documento é convertido para maiúsculas.
- A conta é criada ativa, mas não verificada.
- O registo não permite criar contas de administrador.

### 12.2 Grupos

- Nome entre 3 e 80 caracteres e único.
- Descrição obrigatória, até 150 caracteres.
- Quota maior do que zero.
- Dia de pagamento entre 1 e 31.
- Capacidade mínima de 2 membros, incluindo o criador.
- Frequência de 1, 7, 15 ou 30 dias.
- Ordem de recebimento manual ou aleatória.
- O criador é o primeiro membro e o responsável.
- Só é possível adicionar, convidar, remover ou reordenar antes do primeiro ciclo.
- Um grupo com ciclos iniciados não pode ser eliminado.
- Administradores não podem criar nem participar em grupos.

### 12.3 Ciclos e contribuições

- O grupo tem de estar completo para o primeiro ciclo começar.
- Na ordem aleatória, o primeiro ciclo começa automaticamente quando o grupo fica completo.
- Na ordem manual, o responsável abre o primeiro ciclo.
- Só existe um ciclo ativo por grupo.
- Cada membro paga uma vez por ciclo.
- O valor pago é a quota definida no grupo.
- A data de pagamento tem de estar entre o início do ciclo e o dia atual.
- O ciclo é concluído quando todos os membros pagam.
- O beneficiário é marcado como **Já recebeu**.
- O saldo só é transferido depois de o ciclo estar concluído e com todos os pagamentos.
- Depois da transferência, o sistema pode iniciar o ciclo seguinte automaticamente.

### 12.4 Prazos e pontualidade

- A data limite é, em regra, o **dia de pagamento** definido no grupo. Para frequência diária, é o fim do ciclo.
- Se o ciclo começar depois do dia de pagamento, o prazo passa para o mês seguinte.
- O sistema pode enviar lembretes antes do prazo e avisos de atraso.
- Pagar até ao prazo dá **+10 pontos**.
- Pagar depois do prazo pode descontar a penalização configurada (por predefinição, **10 pontos**).
- A verificação de prazos é executada enquanto a aplicação está aberta.

### 12.5 Pagamentos

- Todos os pagamentos são simulados.
- M-Pesa: números com 9 dígitos e prefixo `84` ou `85`.
- e-Mola: números com 9 dígitos e prefixo `86` ou `87`.
- Números terminados em `0` simulam saldo insuficiente.
- Números terminados em `9` simulam tempo de resposta excedido.
- A aplicação não se liga a carteiras móveis reais.

---

## 13. Resolução de problemas

| Problema | Causa provável | O que fazer |
|---|---|---|
| **“JavaFX runtime components are missing”** ou erro ao abrir a janela | JavaFX não está configurado no IDE ou o JDK não é o 25. | Confirma o JDK 25, atualiza as dependências Maven e executa `view.Main` pelo IntelliJ. Se o IDE usar JavaFX SDK separado, configura o module path com `javafx.controls` e `javafx.fxml`. |
| **“Access denied for user 'ixitique'@'localhost'”** | Utilizador ou palavra-passe diferentes dos configurados. | Confirma `persistence.xml` e cria/ajusta o utilizador MySQL com as permissões necessárias. |
| **“Unknown database 'ixitique'”** | A base de dados não existe. | Executa o `CREATE DATABASE` da secção 2.2 ou importa o dump. |
| **“Communications link failure”** | O MySQL não está a correr ou a porta está errada. | Inicia o MySQL e confirma a porta `3306` no URL. |
| **Tabelas inexistentes ou erros de schema** | A base está vazia ou o `persistence.xml` aponta para outro schema. | Confirma o URL e inicia a aplicação uma vez para o Hibernate criar/atualizar as tabelas. |
| **Login recusado** | Palavra-passe errada, utilizador inexistente ou conta desativada. | Confirma as credenciais. Se a conta estiver desativada, pede a reativação a um administrador. |
| **“Não foi possível enviar o email”** | SMTP mal configurado, credenciais erradas ou porta sem STARTTLS. | Confirma as variáveis `IXITIQUE_SMTP_*`, usa a porta `587` e uma palavra-passe de aplicação. |
| **“Configure a variável de ambiente...”** | Falta uma variável SMTP obrigatória. | Define `IXITIQUE_SMTP_HOST`, `IXITIQUE_SMTP_USER` e `IXITIQUE_SMTP_PASSWORD` e reinicia a aplicação. |
| **“Código inválido ou expirado”** | O código passou dos 10 minutos, a aplicação foi fechada ou foi pedido outro código. | Pede um novo código e introduz-o sem fechar a aplicação. |
| **“A criação de grupos requer verificação KYC...”** | A conta ainda não foi aprovada. | Submete o KYC em **Meu perfil** e aguarda a aprovação de um administrador. |
| **“O ciclo será iniciado quando o grupo atingir...”** | O grupo ainda não tem a totalidade dos membros. | Convida os membros em falta ou aumenta a capacidade apenas antes de criar o grupo. |
| **“Já existe um ciclo em andamento”** | Já existe um ciclo ativo. | Conclui o ciclo atual e transfere o saldo antes de abrir outro. |
| **“Transfira o bolo do ciclo anterior...”** | O saldo do ciclo anterior ainda não foi transferido. | Usa **Transferir saldo ao beneficiário** e confirma que o saldo ficou a zero. |
| **“Não existe nenhum ciclo em andamento”** | Tentaste pagar sem ciclo ativo. | O responsável deve abrir o primeiro ciclo; nas ordens aleatórias, o ciclo começa quando o grupo fica completo. |
| **“O membro ... já pagou a cota do ciclo atual”** | Cada membro só paga uma vez por ciclo. | Aguarda o ciclo seguinte. |
| **“Número M-Pesa inválido” / “Número e-Mola inválido”** | Prefixo ou quantidade de dígitos errada. | M-Pesa: `84`/`85` + 7 dígitos. e-Mola: `86`/`87` + 7 dígitos. |
| **“Saldo insuficiente” na simulação** | O número termina em `0`. | Usa outro número terminado em `1` a `8`. |
| **“Tempo de resposta excedido” na simulação** | O número termina em `9`. | Usa outro número terminado em `1` a `8`. |
| **“O bolo só pode ser transferido depois...”** | O ciclo ainda não está concluído ou faltam pagamentos. | Confirma que todos os membros pagaram e que o ciclo está concluído. |
| **“Este utilizador está associado a grupos...”** | Tentaste apagar uma conta com grupos ou participações. | Usa **Desativar** em vez de apagar, para preservar o histórico. |
| **Não consigo abrir um grupo privado** | O grupo é privado e não tens convite. | Pede ao responsável para te convidar ou entra num grupo público. |
| **Não consigo adicionar um membro** | O primeiro ciclo já começou, o grupo está cheio ou o utilizador já pertence ao grupo. | Confirma o estado do grupo e a capacidade. |
| **A exportação falha** | O ficheiro está aberto noutro programa ou o local não tem permissões. | Escolhe outro nome/local e fecha o ficheiro de destino. |

---

## 14. Limitações e boas práticas

### 14.1 Limitações desta versão

- **Pagamentos simulados:** não há dinheiro real, integração bancária ou confirmação de operadores.
- **Transferência automática:** só é tentada enquanto a aplicação estiver aberta e depois de o prazo passar. Se falhar, a transferência manual continua disponível.
- **Recuperação de palavra-passe:** os códigos ficam em memória; fechar a aplicação invalida-os.
- **Um ciclo ativo por grupo:** não é possível abrir vários ciclos ao mesmo tempo.
- **Grupo completo:** o primeiro ciclo exige a totalidade dos membros definida na criação.
- **Sem saída autónoma:** um membro não responsável não tem botão para sair do grupo; o responsável remove-o antes do primeiro ciclo.
- **Administradores:** não podem criar nem participar em grupos.
- **E-mails de demonstração:** terminam em `.test` e não recebem mensagens reais.

### 14.2 Boas práticas

- Usa uma base de dados local e uma conta MySQL dedicada à aplicação.
- Não uses as credenciais de demonstração em produção.
- Não edites diretamente as tabelas enquanto a aplicação estiver a correr; usa a interface.
- Faz cópias de segurança antes de importar o dump ou reiniciar a base.
- Não versiones o dump com dados pessoais nem imagens KYC.
- Confirma sempre o número de telefone antes de simular um pagamento.
- Para apresentações, usa números terminados em `1` a `8` para evitar falhas simuladas.

---

## 15. Repor a base de dados

Para apagar os dados existentes e voltar a começar com uma base vazia, elimina e recria a base de dados. **Isto elimina todas as contas, grupos e contribuições existentes.**

> **Atenção:** executa apenas numa base local. Não faças isto numa base com dados reais. Para preservar os dados, faz uma cópia de segurança antes de continuar.

1. Apaga e recria a base:

   ```sql
   DROP DATABASE IF EXISTS ixitique;

   CREATE DATABASE ixitique
     CHARACTER SET utf8mb4
     COLLATE utf8mb4_0900_ai_ci;

   GRANT ALL PRIVILEGES ON ixitique.* TO 'ixitique'@'localhost';
   FLUSH PRIVILEGES;
   ```

2. Inicia a aplicação. O Hibernate cria as tabelas e podes registar uma conta nova.

Se quiseres voltar a ter os dados da demonstração, importa o dump depois de recriar a base:

```bash
mysql -u root -p < database/ixitique_dump.sql
```

---

## 16. Checklist de demonstração

Um percurso sugerido para apresentar a aplicação:

1. **Preparar:** inicia o MySQL, importa o dump local (secção [2.3](#23-preparar-os-dados-iniciais)) e executa `view.Main`.
2. **Entrar como utilizador:** inicia sessão com `demo.amelia` / `Lulerk@12` e mostra a **Visão geral**.
3. **Ver um grupo ativo:** abre **Meus Xitiques** > **DEMO - Xitique Activo**; mostra a fila, o ciclo, as contribuições e o saldo.
4. **Testar um pagamento:** sai e entra como `demo.ernesto`; abre o mesmo grupo, seleciona **Registrar pagamento**, escolhe M-Pesa e usa um número como `841234567`; mostra a referência de sucesso.
5. **Ver notificações e convites:** sai e entra como `demo.sara`; abre a campainha e aceita ou recusa o convite pendente; mostra o contador e o grupo atualizado.
6. **Mostrar um grupo público:** em **Meus Xitiques**, abre **Descobrir grupos públicos** e entra num grupo com vaga.
7. **Mostrar KYC:** inicia sessão como `demo.sara` e abre **Meu perfil**; mostra o estado **EM ANÁLISE** e as imagens fictícias.
8. **Aprovar KYC:** entra como `demo.admin`, abre **Verificação KYC**, consulta as fotos e aprova o pedido.
9. **Mostrar administração:** percorre **Utilizadores**, **Denúncias**, **Grupos**, **Regras globais**, **Relatórios** e **Suporte**.
10. **Mostrar suporte:** com um utilizador normal, envia um pedido no **Centro de Ajuda**; com o administrador, responde e altera o estado; volta ao utilizador e consulta **Os meus pedidos**.

> **Dica:** os utilizadores de demonstração não têm telefone preenchido. No diálogo de pagamento, introduce um número válido manualmente. Para M-Pesa, `841234567`; para e-Mola, `861234567`.

---

## Glossário

| Termo | Definição |
|---|---|
| **Xitique** | Poupança rotativa em grupo. |
| **Quota** | Valor que cada membro paga em cada ciclo. |
| **Ciclo** | Rodada de contribuições com um beneficiário definido. |
| **Bolo** | Saldo acumulado do ciclo/grupo. |
| **Beneficiário** | Membro que recebe o bolo no ciclo. |
| **Responsável** | Criador do grupo, com permissões de gestão. |
| **KYC** | Verificação de identidade do utilizador. |
| **Strike** | Penalização registada por um administrador. |
| **Pontualidade** | Pontos atribuídos por pagamentos dentro do prazo. |
| **M-Pesa / e-Mola** | Carteiras móveis simuladas na aplicação. |

---

**Fim do guia.** Para instruções técnicas adicionais, consulta o [`README.md`](README.md).
