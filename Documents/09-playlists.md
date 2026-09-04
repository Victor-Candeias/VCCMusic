# WI-09 — Implementar playlists personalizadas

**Estado:** Concluído em 4 de setembro de 2026

## Objetivo

Permitir criar, editar e reproduzir listas ordenadas sem duplicar ficheiros de música.

## Trabalho

- Criar ecrã de playlists e detalhe de playlist.
- Implementar criar, renomear e eliminar com validação e confirmação quando destrutivo.
- Adicionar/remover faixas a partir da biblioteca e do detalhe; decidir se a mesma faixa pode repetir-se.
- Implementar reordenação com atualização transacional das posições.
- Construir fila pela ordem persistida e permitir shuffle apenas no player.
- Representar faixas indisponíveis e oferecer remoção, sem falhar toda a playlist.
- Garantir que troca/reindexação da raiz segue a política definida para playlists órfãs.

## Critérios de aceitação

- CRUD persiste após reiniciar a aplicação.
- A ordem escolhida é estável, sem posições duplicadas ou lacunas problemáticas.
- Reproduzir uma playlist respeita a ordem quando shuffle está desligado.
- Eliminar uma playlist não elimina `Track` nem ficheiros.
- Uma faixa indisponível é indicada e as restantes continuam utilizáveis.

## Testes

- Testes DAO/repositório para concorrência, transações e posições.
- Compose UI para criar, renomear, eliminar, reordenar e estado vazio.
- Reprodução normal e aleatória da mesma playlist.

## Dependências

WI-03 e WI-07; integração visual após WI-06.

## Implementação atual

- O separador Playlists apresenta CRUD persistente com Room.
- O detalhe permite adicionar/remover faixas e reordená-las com operações transacionais.
- A eliminação exige confirmação e não remove as faixas da biblioteca.
- A associação de uma faixa só pode ocorrer uma vez por playlist; as posições são reconstruídas sem lacunas.
- O estado vazio e a biblioteca sem raiz são tratados sem ecrã em branco.

**Conclusão:** WI-09 finalizado.
