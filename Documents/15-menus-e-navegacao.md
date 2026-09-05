# WI-15 — Criar menus e navegação conforme o layout

## Estado

Concluído para o âmbito atual do MVP. Foi implementado o menu principal, submenu Música, navegação para Biblioteca/Playlists/Em reprodução, Configurações, ação Fechar e estados explícitos para funcionalidades fora do MVP.

**Estado:** Planeado  
**Referência:** `Documents/Layout.pptx`

## Objetivo

Implementar a hierarquia de menus, os ecrãs e as transições representados no layout fornecido, preservando uma navegação previsível com back, estado de seleção e acesso consistente ao player.

## Âmbito

### Menu principal

Criar um menu inicial com:

- **Música** — abre o menu de exploração da biblioteca.
- **Rádios Online** — abre a lista de rádios, caso esta funcionalidade seja incluída.
- **Configurações** — abre as preferências da aplicação.
- **Fechar** — encerra a aplicação de forma explícita.

O destino inicial após abrir a app deve ser o menu principal. A última seleção de pasta, lista ou faixa não deve substituir o destino inicial sem uma decisão de produto específica.

### Menu Música

Apresentar as entradas:

- **Faixas**
- **Pastas**
- **Artistas**
- **Álbuns**
- **Listas**
- **Voltar**

Cada entrada abre uma lista própria e mantém a possibilidade de regressar ao menu Música sem perder o estado de scroll quando isso for suportado pelo Navigation Compose.

### Listas de conteúdo

- **Faixas:** cartões com imagem/capa, título, artista e álbum; incluir contagem de faixas, ação de reprodução e ação de aleatório.
- **Pastas:** mostrar o nome das pastas e permitir navegar para subpastas e respetivas faixas.
- **Artistas:** mostrar imagem quando disponível, nome do artista, número de faixas e número de álbuns.
- **Álbuns:** mostrar capa quando disponível, nome do álbum, artista e número de músicas.
- **Listas:** mostrar nome da playlist e número de faixas; selecionar uma playlist abre as suas faixas.

As listas devem usar os dados indexados em Room e respeitar as ordenações e fallbacks definidos no WI-01. Uma faixa indisponível não deve aparecer como reproduzível.

### Reprodução

Ao selecionar uma faixa, iniciar a reprodução dessa faixa. O botão **Reproduzir** inicia a fila da lista atual; **Aleatório** cria a mesma fila com shuffle sem alterar a ordem persistida.

O ecrã Em reprodução deve incluir:

- capa ou imagem de fundo da faixa;
- título e artista;
- indicador visual de reprodução/equalizador;
- linha temporal;
- controlos anterior, reproduzir/pausar e próximo;
- gesto horizontal para a esquerda (próxima faixa) e direita (faixa anterior).

O player continua a ser controlado pelo `MediaLibraryService`, conforme a arquitetura existente.

### Configurações

Criar opções para:

- selecionar/trocar a pasta de músicas através do SAF;
- guardar a alteração e indicar o impacto na reindexação;
- analisar a biblioteca ao abrir a aplicação;
- alternar modo claro/escuro;
- selecionar ou remover uma imagem de fundo;
- configurar rádios online, se a funcionalidade for aprovada.

As preferências simples devem usar DataStore. A imagem escolhida deve manter uma URI `content://` persistente e não um caminho físico.

### Rádios Online

O layout inclui um ecrã com cartões de rádio contendo imagem, nome e estado da estação. Ao tocar numa rádio, o cartão deve indicar visualmente a estação ativa e o player deve apresentar a informação da emissão.

Esta funcionalidade não está incluída no âmbito MVP definido no WI-01. Antes da implementação deve ser aceite uma alteração de âmbito que defina fontes permitidas, persistência das estações, reprodução de streams, tratamento de erro e requisitos de privacidade. Até essa decisão, a entrada pode ficar oculta ou apresentar um estado explicitamente indisponível.

## Navegação

Implementar a seguinte sequência:

```text
Abrir app
└── Menu principal
    ├── Música
    │   ├── Faixas
    │   │   └── selecionar faixa/reproduzir → Em reprodução
    │   ├── Pastas
    │   │   └── selecionar pasta → Faixas
    │   ├── Artistas
    │   │   └── selecionar artista → Álbuns/Faixas
    │   ├── Álbuns
    │   │   └── selecionar álbum → Faixas
    │   └── Listas
    │       └── selecionar lista → Faixas → Em reprodução
    ├── Rádios Online
    └── Configurações
```

Regras de navegação:

- voltar fecha o ecrã atual e preserva o estado anterior;
- trocar de raiz exige confirmação e remove do índice os registos da raiz anterior apenas através de uma sincronização válida;
- abrir novamente a app com uma permissão SAF revogada conduz às Configurações para escolher uma raiz;
- a barra/ação de acesso a Em reprodução deve estar disponível a partir das listas quando existir uma fila ativa;
- estados de carregamento, vazio, erro e conteúdo indisponível devem ser explícitos e recuperáveis.

## Imagens de fundo e identidade visual

Permitir uma imagem de fundo global escolhida pelo utilizador, com pré-visualização e remoção. A imagem deve:

- ser obtida por `ACTION_OPEN_DOCUMENT`;
- conservar a permissão persistente de leitura;
- ser apresentada com `ContentScale.Crop` e uma camada de contraste para manter a legibilidade;
- respeitar o modo claro/escuro e não esconder controlos ou texto;
- falhar de forma segura quando a URI deixar de estar disponível.

As capas de álbuns e imagens das rádios devem permanecer independentes da imagem de fundo global. Não incluir imagens binárias no código nem depender de URLs externas sem uma decisão explícita.

## Trabalho técnico

- Substituir a navegação placeholder por destinos tipados e rotas aninhadas.
- Criar ViewModels por fluxo para separar estado de navegação, dados e reprodução.
- Ligar as listas aos repositórios Room e a reprodução ao controller/serviço Media3.
- Implementar componentes reutilizáveis para cartões de faixa, pasta, artista, álbum, playlist e rádio.
- Adicionar suporte de acessibilidade para rótulos, gestos, contraste e ações equivalentes ao swipe.
- Centralizar strings e estados de erro nos recursos Android.
- Atualizar o teste instrumentado para cobrir o fluxo Menu principal → Música → Faixas e o retorno.

## Critérios de aceitação

- O fluxo documentado pode ser percorrido sem ecrãs órfãos ou ciclos de navegação inesperados.
- Todas as entradas do layout têm destino, estado indisponível explícito ou estão marcadas como fora do MVP.
- A seleção de uma faixa, pasta ou playlist encaminha para a fila correta sem misturar raízes.
- O back preserva o estado do ecrã anterior e a fila ativa não é perdida.
- A configuração de imagem de fundo persiste por URI SAF, pode ser removida e mantém texto/controlos legíveis.
- Os modos claro e escuro funcionam em todos os ecrãs.
- Estados vazio, carregamento, erro e permissão SAF revogada são recuperáveis.
- Testes Compose/instrumentados cobrem os destinos principais e a navegação de ida e volta.

## Dependências

WI-03, WI-04, WI-05, WI-06, WI-07, WI-08, WI-09 e WI-10.  
A parte de rádios online depende ainda de uma decisão de produto que altere o âmbito do WI-01.
