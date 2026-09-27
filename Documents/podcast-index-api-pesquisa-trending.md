# Podcast Index API --- Pesquisa e descoberta de podcasts

## Objetivo

Este documento explica como funciona a pesquisa na **Podcast Index
API**, porque algumas pesquisas podem não devolver os resultados
esperados e como obter listas de podcasts em tendência para usar, por
exemplo, numa aplicação Android ou Android Auto.

> **Nota:** a lista `trending` não representa necessariamente os
> podcasts com maior número absoluto de audições no Spotify, Apple
> Podcasts ou YouTube Music. O Podcast Index não possui as estatísticas
> privadas de reprodução dessas plataformas.

------------------------------------------------------------------------

## 1. Pesquisa geral por termo

O endpoint principal para pesquisa é:

``` http
GET /api/1.0/search/byterm?q=TERMO
```

Exemplo:

``` http
GET /api/1.0/search/byterm?q=vasco+pereira+coutinho
```

Este tipo de pesquisa utiliza sobretudo os metadados disponíveis nos
feeds de podcast, como título, autor e proprietário do feed.

### Porque pode parecer que a pesquisa falha?

Os resultados dependem da qualidade dos metadados existentes no RSS.

Um nome pode:

-   não existir no título do podcast;
-   aparecer apenas na descrição de um episódio;
-   estar escrito de forma diferente;
-   existir apenas como convidado de um episódio;
-   não estar corretamente identificado nos metadados;
-   pertencer a um feed ainda não indexado ou desatualizado.

Por isso, não é aconselhável depender exclusivamente de
`/search/byterm`.

------------------------------------------------------------------------

## 2. Pesquisa por pessoa

Quando o utilizador procura uma pessoa, é interessante utilizar:

``` http
GET /api/1.0/search/byperson?q=TERMO
```

Exemplo:

``` http
GET /api/1.0/search/byperson?q=vasco+pereira+coutinho
```

Este endpoint é particularmente útil quando estamos à procura de
apresentadores, autores ou convidados.

A pesquisa pode considerar informação relacionada com:

-   pessoas identificadas através de tags `person`;
-   títulos de episódios;
-   descrições de episódios;
-   autor do feed;
-   proprietário do feed.

### Estratégia recomendada

Quando a pesquisa aparenta ser o nome de uma pessoa:

``` text
1. /search/byperson
       ↓
2. /search/byterm
       ↓
3. combinar/remover duplicados
       ↓
4. ordenar os resultados
```

Esta abordagem tende a encontrar conteúdos que uma pesquisa
exclusivamente pelo nome do podcast poderia não encontrar.

------------------------------------------------------------------------

## 3. Pesquisa pelo título do podcast

Também existe pesquisa orientada ao título:

``` http
GET /api/1.0/search/bytitle?q=TERMO
```

Exemplo:

``` http
GET /api/1.0/search/bytitle?q=geracao+80
```

É útil quando o utilizador conhece o nome do podcast e pretende
encontrá-lo diretamente.

------------------------------------------------------------------------

## 4. Estratégia de pesquisa para uma aplicação

Em vez de executar apenas:

``` text
/search/byterm
```

uma aplicação pode implementar uma pesquisa em várias fases.

### Pesquisa genérica

``` text
Utilizador escreve:
"Vasco Pereira Coutinho"

             ↓

      /search/byperson

             ↓

       /search/byterm

             ↓

       /search/bytitle

             ↓

    juntar resultados

             ↓

    remover duplicados

             ↓

   calcular relevância

             ↓

     apresentar lista
```

Não é obrigatório executar sempre os três endpoints. A aplicação pode
tentar primeiro o endpoint mais adequado e utilizar os restantes como
fallback.

------------------------------------------------------------------------

## 5. Ranking dos resultados

Depois de receber resultados de vários endpoints, é útil criar um
ranking local.

Por exemplo:

  Critério                                Prioridade sugerida
  ------------------------------------- ---------------------
  Título exatamente igual                          Muito alta
  Título começa pelo termo                               Alta
  Nome do autor corresponde                              Alta
  Resultado encontrado por `byperson`                    Alta
  Título contém todas as palavras                       Média
  Descrição contém o termo                              Média
  Correspondência parcial                               Baixa

Uma aplicação portuguesa também pode aumentar a relevância de resultados
cujo idioma seja português (`pt`), sem eliminar resultados noutros
idiomas.

------------------------------------------------------------------------

## 6. Podcasts em tendência

Para descoberta de podcasts, o Podcast Index disponibiliza:

``` http
GET /api/1.0/podcasts/trending
```

Por exemplo:

``` http
GET /api/1.0/podcasts/trending?max=50
```

Isto permite obter uma lista de podcasts em tendência.

### Podcasts em português

Pode ser aplicado um filtro de idioma:

``` http
GET /api/1.0/podcasts/trending?max=50&lang=pt
```

Isto é especialmente interessante para uma aplicação destinada ao
mercado português.

------------------------------------------------------------------------

## 7. Trending não significa "mais ouvidos"

É importante distinguir:

``` text
Podcast Index Trending
        ≠
Ranking global absoluto de audições
```

O Podcast Index não recebe necessariamente o número total de reproduções
efetuadas em:

-   Spotify;
-   Apple Podcasts;
-   YouTube Music;
-   outras aplicações privadas.

Portanto, `trending` deve ser apresentado ao utilizador como algo do
género:

-   **Em tendência**
-   **Podcasts populares**
-   **A descobrir**

Evitaria apresentar essa lista como **"Os podcasts mais ouvidos"** se
não existir uma métrica de audições que suporte essa afirmação.

------------------------------------------------------------------------

## 8. Podcasts recentes

Também é possível utilizar os feeds recentes:

``` http
GET /api/1.0/recent/feeds
```

Por exemplo:

``` http
GET /api/1.0/recent/feeds?max=30&lang=pt
```

Isto permite criar uma secção de novos conteúdos.

------------------------------------------------------------------------

## 9. Estrutura sugerida para a Home da aplicação

Uma aplicação Android poderia ter:

``` text
🎧 Podcasts

🔥 Em tendência
   /podcasts/trending?max=30

🇵🇹 Em português
   /podcasts/trending?max=30&lang=pt

🆕 Novos podcasts
   /recent/feeds?max=30&lang=pt

🔎 Pesquisar
   /search/byperson
   /search/byterm
   /search/bytitle
```

Para Android Auto, convém manter a navegação simples, com poucas
categorias e listas curtas.

------------------------------------------------------------------------

## 10. Fluxo recomendado para pesquisa

Uma estratégia robusta pode ser:

``` text
                   ┌──────────────────────┐
                   │ Texto do utilizador  │
                   └──────────┬───────────┘
                              │
                              ▼
                   ┌──────────────────────┐
                   │ Normalizar pesquisa  │
                   └──────────┬───────────┘
                              │
               ┌──────────────┼──────────────┐
               ▼              ▼              ▼
          byperson         byterm         bytitle
               │              │              │
               └──────────────┼──────────────┘
                              ▼
                   ┌──────────────────────┐
                   │ Combinar resultados │
                   └──────────┬───────────┘
                              ▼
                   ┌──────────────────────┐
                   │ Remover duplicados  │
                   └──────────┬───────────┘
                              ▼
                   ┌──────────────────────┐
                   │ Ranking/relevância  │
                   └──────────┬───────────┘
                              ▼
                   ┌──────────────────────┐
                   │ Mostrar resultados  │
                   └──────────────────────┘
```

------------------------------------------------------------------------

## 11. Melhorias adicionais

Para melhorar a experiência da aplicação:

### Cache

Guardar temporariamente pesquisas e listas `trending`.

Exemplo:

``` text
Trending: 30–60 minutos
Pesquisa: 5–15 minutos
Detalhes de podcast: algumas horas
```

### Debounce

Não chamar a API a cada tecla.

Por exemplo:

``` text
v
va
vas
vasc
vasco
```

Em vez de cinco pedidos, esperar aproximadamente 300--500 ms depois de o
utilizador parar de escrever.

### Número mínimo de caracteres

Executar pesquisa apenas a partir de 2 ou 3 caracteres.

### Deduplicação

Se o mesmo podcast aparecer através de `byperson`, `byterm` e `bytitle`,
mostrar apenas uma entrada.

Sempre que possível, utilizar o identificador do Podcast Index para
fazer a deduplicação.

### Fallback

Se uma pesquisa não devolver resultados:

``` text
byperson
   ↓
byterm
   ↓
bytitle
   ↓
mostrar "Nenhum resultado encontrado"
```

------------------------------------------------------------------------

## 12. Arquitetura sugerida

Para uma aplicação Android:

``` text
Android / Android Auto
          │
          ▼
PodcastRepository
          │
          ├── search()
          ├── trending()
          ├── recent()
          └── podcastDetails()
          │
          ▼
PodcastIndexApi
          │
          ▼
Podcast Index
```

O `PodcastRepository` pode ser responsável por:

-   escolher os endpoints;
-   combinar resultados;
-   remover duplicados;
-   aplicar ranking;
-   gerir cache;
-   tratar erros e timeouts.

Assim, a interface Android não precisa de conhecer os detalhes
específicos da Podcast Index API.

------------------------------------------------------------------------

## 13. Resumo

Para pesquisa:

``` text
/search/byperson
/search/byterm
/search/bytitle
```

Para descoberta:

``` text
/podcasts/trending
/recent/feeds
```

Uma implementação robusta não deve depender apenas de `/search/byterm`.

Para uma aplicação portuguesa, uma boa Home seria:

``` text
🔥 Em tendência
🇵🇹 Em português
🆕 Novidades
🔎 Pesquisa
```

e a pesquisa deve combinar diferentes estratégias para reduzir os casos
em que um podcast ou uma pessoa existe, mas não aparece através da
pesquisa genérica.

------------------------------------------------------------------------

## Referência

Documentação oficial da Podcast Index API:

https://podcastindex-org.github.io/docs-api/
