Xitique Digital
Sistema de Gestão de Poupanças Rotativas

Sobre o Projeto
O Xitique Digital é uma aplicação desenvolvida com o objetivo de facilitar a gestão de grupos de poupança rotativa.
Inspirado na prática tradicional do xitique, o sistema pretende ajudar na organização dos participantes, no controlo das contribuições financeiras e na gestão dos ciclos e das rondas de recebimento.
A aplicação procura tornar a gestão dos grupos mais organizada e transparente, permitindo acompanhar as contribuições, identificar quem deve receber e consultar o histórico das operações realizadas.

Objetivos do Projeto

- Facilitar a criação e a gestão de grupos de poupança rotativa.
- Organizar os participantes e as respetivas responsabilidades.
- Registar e acompanhar as contribuições financeiras.
- Gerir os ciclos de poupança e as rondas de recebimento.
- Controlar os papéis e os estados dos participantes em cada grupo.
- Manter a consistência dos dados e das operações.
- Disponibilizar um histórico das operações relevantes.

Funcionalidades
O sistema prevê as seguintes funcionalidades:
-Gestão de utilizadores: registo e gestão das contas de acesso.
-Gestão de grupos: criação, consulta, atualização e gestão dos grupos de xitique.
-Gestão de participantes: associação de utilizadores aos grupos e gestão das respetivas participações.
-Gestão de papéis: atribuição e alteração de responsabilidades, como líder, tesoureiro e membro.
-Gestão de contribuições: registo e acompanhamento dos valores contribuídos pelos participantes.
-Gestão de ciclos: criação e acompanhamento dos ciclos de poupança.
-Gestão de rondas: organização da ordem de recebimento dos participantes.
-Gestão de recebimentos: registo e acompanhamento dos valores entregues aos participantes.
-Histórico de operações: consulta dos acontecimentos relevantes relacionados com os grupos e as suas operações.
-Validação das regras de negócio: prevenção de operações inválidas e manutenção da consistência dos dados.

As principais telas da aplicação já usam os dados persistidos: o utilizador pode registar-se e entrar, criar grupos, consultar os grupos de que participa, gerir membros, abrir ciclos, registar pagamentos, consultar o histórico e editar os dados de perfil.

### Executar e utilizar

1. Configure uma base de dados MySQL local e ajuste a URL e o utilizador da ligação em `src/main/resources/META-INF/persistence.xml`. O Hibernate cria/atualiza as tabelas da aplicação.
2. Execute `mvn test` para compilar o projeto. No IDE, inicie a classe `view.Application`.
3. No ecrã de entrada, crie uma conta. Depois de entrar, crie um grupo e adicione os utilizadores registados pelo respetivo username ou documento.
4. Abra um ciclo com pelo menos dois membros e registe as contribuições na tela de detalhes. O histórico, o painel inicial e o perfil apresentam os registos da base de dados, sem depender dos exemplos visuais antigos.
5. Para experimentar sem inserir manualmente vários registos, use **Carregar dados de demonstração** na tela de grupos. A operação é explícita e idempotente por conta; adiciona um grupo, dois membros fictícios, um ciclo e duas contribuições à base de dados.

### Regras de negócio da interface

- O username, email, telefone e, quando submetido, documento não podem repetir-se; emails, telefones e palavras-passe são validados no registo.
- Um grupo exige nome entre 3 e 80 caracteres, descrição até 150 caracteres, quota positiva, dia de pagamento entre 1 e 31 e frequência válida. O criador entra automaticamente como primeiro membro.
- Só criadores/admins podem convidar membros, abrir ciclos e reordenar a fila. Chefes de grupo nomeados pelo administrador podem privatizar/publicar, remover membros e eliminar grupos. Não se adicionam/removem membros nem se elimina um grupo depois de começar o histórico de ciclos.
- Um ciclo exige pelo menos dois membros e não pode ser aberto enquanto outro estiver em andamento. A rotação reinicia depois de todos os membros receberem.
- Cada membro pode pagar uma única quota por ciclo; o valor deve ser pelo menos a quota do grupo e a data deve estar entre o início do ciclo e o dia atual. O ciclo conclui quando todos os membros pagam.
- Um grupo com ciclos iniciados não pode ser eliminado, para preservar o histórico financeiro.

### Verificação, perfis e moderação

- Uma conta nova pode ser criada sem BI/NUIT e começa como `USER`, ativa para participar, mas não verificada. A criação de grupos só fica disponível depois de enviar o número do BI/NUIT e duas fotografias (documento e fotografia de confirmação) e um administrador aprovar o KYC.
- As fotografias são armazenadas como dados binários na base de dados, não como caminhos locais. Cada ficheiro pode ter no máximo 8 MB.
- Os perfis suportados são `ADMINISTRATOR`, `USER` e `GROUP_LEADER`. Um utilizador verificado pode pedir ao administrador a nomeação como chefe; a solicitação tem de ser aprovada.
- Convites ficam pendentes até o destinatário os aceitar ou recusar nas notificações. Uma conta não verificada pode aceitar convites e participar; grupos públicos também aparecem para adesão.
- Membros podem denunciar outros participantes do mesmo grupo. O administrador analisa denúncias, aplica strikes, desativa contas, aprova KYC e analisa pedidos de chefia. Pagamentos realizados até à data limite somam 10 pontos de pontualidade ao utilizador.
- O painel de administração apresenta contagens, gráficos, pagamentos e montantes, revisão KYC, denúncias e gestão de contas.

#### Configuração do primeiro administrador

O sistema não cria automaticamente uma conta administrativa nem permite que o registo público escolha esse perfil. Depois de criar a conta inicial e confirmar o seu ID na base de dados, atribua o papel por um canal administrativo seguro, por exemplo:

```sql
UPDATE users SET user_role = 'ADMINISTRATOR' WHERE id = <ID_DA_CONTA_INICIAL>;
```

Substitua `<ID_DA_CONTA_INICIAL>` pelo ID confirmado. As novas colunas e tabelas são adicionadas pelo Hibernate em `update`; faça uma cópia de segurança da base de dados antes de atualizar uma instalação existente.

Tecnologias Utilizadas
Java            Desenvolvimento da lógica da aplicação.                
JavaFX          Desenvolvimento da interface gráfica.                  
Hibernate / JPA Mapeamento objeto-relacional e persistência dos dados. 
MySQL           Armazenamento e gestão da base de dados.               
Maven           Gestão de dependências e construção do projeto.        
Git             Controlo de versões do código.                         
GitHub          Alojamento do repositório e colaboração da equipa.     


Arquitetura do Projeto
O código será organizado em camadas, separando as responsabilidades de cada componente para facilitar a manutenção e a evolução da aplicação.
-Model: contém as entidades e as classes que representam os conceitos do sistema.
-DAO: responsável pelas operações de acesso e persistência dos dados.
-Service: responsável pelas regras de negócio, validações e coordenação das operações.
-Controller: recebe e trata as ações da interface, comunicando com a camada de serviço.
-View: contém os componentes da interface gráfica desenvolvidos com JavaFX.



 Moelagem e Base de Dados
A modelagem do sistema será orientada pelos princípios da Programação Orientada a Objetos e representada através de diagramas UML.
A base de dados será implementada em MySQL, utilizando Hibernate/JPA para estabelecer o mapeamento entre as classes Java e as tabelas relacionais.
O modelo deverá representar os utilizadores, grupos, participações, ciclos, rondas e contribuições, incluindo os dados necessários para gerir os recebimentos e o histórico das operações.
As regras de negócio serão tratadas principalmente na camada de serviço, recorrendo também a mecanismos de integridade da base de dados sempre que necessário.


Contexto do Projeto
O Xitique Digital é um projeto semestral desenvolvido no âmbito da cadeira de Programação, com o propósito de aplicar conceitos de Programação Orientada a Objetos, modelagem UML, persistência de dados e desenvolvimento de aplicações Java.

Projeto académico desenvolvido para fins educacionais.
