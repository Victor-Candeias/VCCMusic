# WI-03 — Implementar o modelo de domínio e Room

## Objetivo

Criar a fonte local indexada para pastas, faixas, playlists e preferências estruturadas.

## Trabalho

- Modelar `MusicRoot`, `MusicFolder`, `Track`, `Playlist` e `PlaylistTrack`.
- Guardar URIs como texto e definir índices/constraints: URI única por raiz, relação pai-filho, posição única por playlist.
- Decidir se a raiz é uma pasta normal ou entidade própria; preparar o esquema para uma única raiz ativa sem impedir evolução futura.
- Criar DAOs para árvore de pastas, conteúdo de uma pasta, todas as faixas, pesquisa por IDs e CRUD de playlists.
- Assegurar eliminação transacional e regras claras para referências de faixas desaparecidas.
- Expor `Flow` para ecrãs reativos e operações suspensas para escrita.
- Criar mapeamentos entre entidades Room, modelos de domínio e `MediaItem` sem colocar tipos de UI na camada de dados.
- Guardar configurações simples (URI da raiz, ordenação, última seleção) em DataStore quando não pertençam ao modelo relacional.
- Documentar versão do esquema e estratégia de migração.

## Critérios de aceitação

- A base de dados representa subpastas arbitrariamente profundas sem ciclos criados pela aplicação.
- A ordem de uma playlist permanece estável e pode ser alterada numa transação.
- Eliminar uma playlist não elimina as faixas da biblioteca.
- Reindexar a biblioteca não recria nem duplica playlists.
- Consultas principais têm índices adequados e ordenação determinística.

## Testes

- Testes Room in-memory para inserção/atualização/remoção, relações, cascatas e ordenação.
- Testes de playlist com a mesma faixa em playlists diferentes.
- Teste de URI duplicado e de metadados nulos.

## Dependências

WI-02.

