# Relatório de alterações do iXitique

## Para que serve este documento

Este relatório apresenta, em linguagem simples, as principais funcionalidades que passaram a fazer parte do projeto nas últimas três semanas de trabalho. A ideia é ajudar a perceber não só quais foram as alterações, mas também como elas se ligam numa utilização normal da aplicação.

O período considerado começa em **15 de outubro de 2026**. A branch consultada é **`feature/termsNconditions`** e o último commit de código nela registado é de **28 de outubro de 2026** (`0a680eb`). Assim, o relatório descreve o que está incluído nessa versão da branch; não afirma que esses recursos estejam ligados a serviços financeiros reais ou prontos para produção.

## Resumo geral

O projeto evoluiu de uma aplicação com os elementos essenciais de um xitique para um protótipo desktop mais completo. Agora é possível acompanhar um percurso mais próximo do uso real: criar uma conta, passar por uma verificação de identidade, organizar um grupo com regras definidas, registar contribuições, acompanhar os ciclos e pedir ajuda dentro da aplicação.

Também foi desenvolvido um espaço administrativo próprio. Nele, um administrador pode acompanhar indicadores, analisar pedidos de verificação, gerir utilizadores e grupos, tratar denúncias e responder a pedidos de suporte.

## Funcionalidades acrescentadas e melhoradas

### 1. Registo, validação e acesso à conta

O registo foi organizado em passos, tornando mais claro o preenchimento dos dados pessoais e das credenciais. A aplicação valida os dados antes de criar a conta, em vez de aceitar qualquer valor:

- verifica campos obrigatórios, e-mail, nome de utilizador, telefone moçambicano e palavra-passe;
- valida a data de nascimento e aceita idades entre 15 e 90 anos;
- evita registar novamente dados que têm de ser únicos, como nome de utilizador, e-mail, telefone e documento;
- guarda a palavra-passe protegida por hash, em vez de a guardar como texto simples;
- não permite que alguém crie uma conta de administrador através do formulário normal.

O utilizador pode iniciar sessão com o nome de utilizador, o e-mail ou o número de telefone. Foi também acrescentada a aceitação dos **Termos e Condições** ao registo: sem aceitar, a conta não é criada.

### 2. Verificação de identidade (KYC)

Foi acrescentado um processo de verificação de identidade. O utilizador pode submeter o número do documento, uma fotografia do documento e uma fotografia de confirmação. A aplicação verifica se os dados estão preenchidos, se as imagens foram anexadas e se cada imagem respeita o limite de tamanho definido.

O pedido fica pendente até ser analisado por um administrador, que pode aprová-lo ou recusá-lo. O resultado fica associado à conta. Esta verificação também tem efeito nas permissões: uma conta normal pode participar em grupos, mas precisa de aprovação KYC para criar um grupo. Administradores não criam nem integram grupos de xitique.

### 3. Criação e organização dos grupos

A criação de grupos passou a incluir mais regras para que a rotação de poupança possa ser acompanhada:

- definição do valor da contribuição, do prazo e da frequência dos ciclos;
- definição da capacidade total do grupo;
- escolha entre grupo privado e grupo público;
- escolha de uma ordem manual ou aleatória para a fila de recebimento.

Quem cria o grupo passa a ser o seu responsável e o primeiro membro. O grupo só inicia o ciclo quando atinge a capacidade definida. O responsável pode adicionar ou remover membros antes de começar o histórico de ciclos; depois de iniciado, a composição do grupo fica protegida para preservar esse histórico.

Para grupos públicos, foi acrescentada a possibilidade de os utilizadores descobrirem grupos com vagas e pedirem para entrar, sem depender de um convite direto.

### 4. Convites com escolha do utilizador

Um convite já não adiciona automaticamente a pessoa ao grupo. O utilizador recebe uma notificação e pode **aceitar** ou **recusar** o convite. Esta alteração deixa a entrada no grupo dependente da decisão da pessoa convidada e torna o processo visível na área de notificações.

### 5. Ciclos, contribuições e recebimento do bolo

Um grupo pode ter ciclos numerados, cada um com um beneficiário. O ciclo acompanha o estado das contribuições e o valor acumulado — o **bolo** — até que possa ser transferido ao membro escolhido.

As regras implementadas incluem:

- não abrir um segundo ciclo enquanto houver outro em andamento;
- não iniciar ciclos antes de o grupo alcançar a capacidade definida;
- respeitar a ordem de recebimento configurada e impedir que um membro receba novamente antes de os restantes terem a sua vez;
- só permitir a transferência do bolo depois de todas as contribuições esperadas terem sido pagas;
- registar o resultado da transferência e, em caso de sucesso, atualizar o saldo e permitir o início do ciclo seguinte.

O objetivo destas regras é evitar situações como ciclos incompletos, pagamentos duplicados ou transferências antes de a rodada estar concluída.

### 6. Pagamentos simulados e histórico de transações

Foi criada uma estrutura para processar contribuições e transferências através de métodos associados a **M-Pesa, e-Mola, BIM e numerário**. Os pagamentos digitais usam gateways simulados: a aplicação valida os dados, devolve um resultado de teste e regista a transação, incluindo o estado e a referência quando aplicável.

Há também proteção contra duas tentativas simultâneas de pagamento para o mesmo membro e grupo. Se o gateway simulado indicar sucesso, a contribuição é registada; se indicar falha, o resultado fica disponível para consulta, sem marcar a contribuição como paga.

**Importante:** não há ligação real a M-Pesa, e-Mola ou BIM, nem movimentação de dinheiro. São simulações para demonstrar o fluxo e testar cenários. O pagamento em numerário é registado na aplicação, mas também não representa uma cobrança automática.

### 7. Prazos, notificações e pontualidade

O acompanhamento dos ciclos foi alargado com notificações para lembrar os membros de prazos próximos e informar sobre pagamentos em atraso. O responsável também pode ser avisado quando um membro ainda não pagou ou paga depois do prazo.

O sistema mantém pontos de pontualidade e pode aplicar uma penalização configurável a pagamentos atrasados. Foram acrescentadas notificações relativas a convites e a acontecimentos do ciclo, incluindo falhas de tentativa de transferência automática. A transferência automática é uma tentativa feita pela própria aplicação e não substitui a possibilidade de o responsável fazer a transferência manualmente.

### 8. Recuperação da palavra-passe por e-mail

Foi acrescentado um fluxo para recuperar a conta sem intervenção direta do administrador:

1. o utilizador indica o e-mail associado à conta;
2. recebe um código numérico de seis dígitos;
3. confirma o código e define uma nova palavra-passe.

O código tem validade de dez minutos e é verificado antes da alteração da palavra-passe. O envio depende de uma conta SMTP configurada. Por opção desta implementação, os códigos ficam apenas na memória da aplicação: se a aplicação for fechada, os códigos deixam de estar disponíveis e é necessário pedir outro.

### 9. Painel e ferramentas de administração

O acesso administrativo foi separado da navegação normal dos utilizadores. O menu de administração não é apresentado a contas comuns, e as operações administrativas verificam se quem as solicita tem o papel de administrador.

O painel inclui:

- **Indicadores e gráficos:** contas, contas ativas e verificadas, grupos, contribuições e valores pagos, pendentes ou em atraso, pedidos KYC e denúncias abertas;
- **Verificação KYC:** consulta dos pedidos pendentes e das imagens submetidas, com ações para aprovar ou recusar;
- **Gestão de utilizadores:** consulta, pesquisa, ativação ou desativação de contas, registo de penalizações e outras ações administrativas, com restrições para proteger contas administrativas e o histórico associado a grupos;
- **Gestão de grupos:** consulta de grupos, respetiva informação e estado; um grupo não pode ser arquivado enquanto tiver um ciclo em curso;
- **Regras globais:** configuração de lembretes, avisos de atraso e pontos descontados por falta de pontualidade;
- **Denúncias:** consulta e resolução de denúncias submetidas na plataforma;
- **Relatórios financeiros:** seleção de um período e exportação dos dados em CSV ou PDF, além de uma lista de membros com mais atrasos.

### 10. Centro de Ajuda e pedidos de suporte

Foi criado um Centro de Ajuda dentro da aplicação, com acesso a um guia, perguntas frequentes e contacto com o suporte. As perguntas frequentes podem ser pesquisadas e abertas para consultar as respostas.

Quando um utilizador envia um pedido, este fica associado à sua conta. Na área **Os meus pedidos**, pode acompanhar o estado, rever a mensagem e consultar a resposta recebida. Do lado administrativo, é possível ler o pedido, escrever uma resposta e alterar o estado entre **aberto**, **em curso** e **resolvido**.

### 11. Organização da interface e navegação

A interface foi reorganizada em ecrãs próprios para as áreas de utilizador e de administração, com navegação entre páginas de grupos, histórico, perfil, notificações, ajuda e ferramentas administrativas. Também foram revistos ecrãs de entrada, registo, criação de grupos, pagamentos e administração para apresentar melhor as ações disponíveis.

Na área de grupos foi retirada a ação de carregar dados de demonstração pela interface. O acesso às funcionalidades deve ser feito pelo fluxo normal da aplicação, em vez de através desse botão de teste.

## Exemplo de percurso para apresentar ao docente

Um percurso simples que mostra como as funcionalidades se relacionam:

1. Criar uma conta de utilizador e aceitar os Termos e Condições.
2. Submeter documentos para KYC e, com uma conta de administrador, aprovar o pedido.
3. Criar um grupo, definir a capacidade e as regras de contribuição e acrescentar os restantes membros.
4. Abrir um ciclo, identificar o beneficiário e registar contribuições com um dos métodos simulados.
5. Consultar o estado do ciclo, o histórico e as notificações.
6. Concluir a rodada e demonstrar a transferência simulada do bolo.
7. Submeter um pedido de ajuda como utilizador e responder a esse pedido no painel administrativo.
8. Consultar no painel os indicadores, os pedidos KYC, os grupos ou o relatório financeiro.

Para observar esses fluxos, é preciso ter uma base de dados preparada e contas com os papéis apropriados. A criação de grupos exige uma conta normal com KYC aprovado; as ações administrativas exigem uma conta de administrador.

## Limitações que devem ser consideradas na avaliação

- **Os pagamentos são simulações.** Não existe integração financeira real nem movimentação de dinheiro.
- **O envio de códigos de recuperação depende de SMTP.** Sem essa configuração, o e-mail não é enviado.
- **Os códigos de recuperação existem apenas enquanto a aplicação está aberta.**
- **Os documentos KYC são submetidos para revisão administrativa.** Não há validação automática de identidade por um serviço externo.
- **As notificações e as tentativas automáticas dependem da execução da aplicação.** Não devem ser entendidas como um serviço de processamento sempre ativo.

Estas limitações fazem parte do âmbito do protótipo académico e são importantes para distinguir as funcionalidades demonstradas das que exigiriam serviços externos e infraestrutura de produção.

## Organização técnica, em poucas palavras

O projeto usa Java e JavaFX para a aplicação desktop, Hibernate/JPA para comunicar com a base de dados MySQL e uma separação entre ecrãs, controladores, serviços de regras de negócio e acesso a dados. Em termos simples, os ecrãs recolhem as ações do utilizador, os serviços aplicam as regras do xitique e os DAOs guardam ou consultam a informação persistente.

As alterações acrescentaram entidades para representar, entre outras coisas, transações, notificações, convites, denúncias e pedidos de suporte. Esta estrutura permite que os dados importantes — como o estado de uma contribuição ou a resposta a um pedido — continuem associados às respetivas contas e grupos.

## Nota final

Em conjunto, estas alterações dão ao iXitique um percurso de utilização mais completo: a aplicação ajuda a organizar quem participa, quando contribui, quem recebe o bolo e como se acompanham problemas ou dúvidas. A parte financeira continua deliberadamente simulada, mas o registo das operações e as regras dos ciclos permitem demonstrar o funcionamento esperado de um serviço de poupança rotativa.
