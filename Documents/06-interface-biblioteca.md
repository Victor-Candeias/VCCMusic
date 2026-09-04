# WI-06 — Criar a interface da biblioteca

**Estado:** Concluído em 4 de setembro de 2026

## Objetivo

Permitir navegar pela hierarquia indexada e consultar todas as faixas no telefone.

## Trabalho

- Criar o onboarding/estado sem raiz com ação para escolher uma pasta.
- Mostrar progresso da primeira indexação e opção segura para cancelar/tentar novamente.
- Criar ecrã de pastas com breadcrumbs/up navigation, subpastas e faixas da pasta atual.
- Criar ecrã “Todas as músicas” com lista eficiente e ordenação decidida no WI-01.
- Apresentar título, artista, duração e artwork/fallback sem decodificar imagens no tamanho original.
- Implementar estados loading, vazio, erro, permissão perdida e conteúdo parcialmente indexado.
- Usar ViewModels com estado imutável e `Flow`; sobreviver a rotação e recriação do processo.
- Preparar ações “Tocar”, “Tocar pasta” e “Adicionar à playlist”, mesmo que algumas sejam ligadas nos itens seguintes.
- Adicionar semântica de acessibilidade e layouts para diferentes dimensões/font scales.

## Critérios de aceitação

- O utilizador chega a qualquer pasta indexada e regressa corretamente à raiz.
- “Todas as músicas” contém exatamente o conjunto indexado abaixo da raiz.
- Listas grandes fazem scroll sem ANR e usam chaves estáveis.
- Alterações em Room refletem-se na UI sem refresh manual.
- Todos os estados definidos podem ser reproduzidos e não apresentam ecrã em branco.

## Testes

- Testes de ViewModel para cada estado.
- Compose UI: navegação, pasta vazia, biblioteca vazia e lista com nomes longos.
- Acessibilidade com TalkBack e escala de fonte aumentada.

## Dependências

WI-05.

## Implementação atual

- A biblioteca observa a raiz ativa e apresenta um estado recuperável quando nenhuma raiz foi escolhida.
- A raiz mostra subpastas e faixas indexadas, com acesso a “Todas as músicas”.
- A navegação percorre subpastas arbitrariamente profundas e mantém o caminho para voltar corretamente.
- As listas usam `Flow` do Room, chaves estáveis e `LazyColumn`; alterações no índice refletem-se sem refresh manual.
- Os itens apresentam título, artista e álbum quando disponíveis, com estados vazios para biblioteca, pasta e lista global.
- O estado da biblioteca é mantido num `LibraryViewModel` e a seleção da raiz continua a usar SAF.

**Conclusão:** WI-06 finalizado.
