# WI-05 — Construir o scanner e indexar a biblioteca

## Objetivo

Percorrer a raiz SAF recursivamente, extrair metadados e atualizar Room de forma segura e observável.

## Trabalho

- Listar documentos por `DocumentsContract` ou `DocumentFile`, isolando a implementação atrás da abstração do WI-04.
- Reconhecer ficheiros áudio por MIME type e, com prudência, extensão quando o provider não fornecer um MIME útil.
- Ler título, artista, álbum, duração e artwork embutido através de APIs compatíveis; definir fallbacks sem bloquear a indexação.
- Fazer traversal iterativo ou protegido contra profundidade/ciclos anómalos do provider.
- Produzir progresso, contagens e erros parciais; suportar cancelamento por coroutine.
- Gravar resultados em lotes/transações sem manter uma transação aberta durante todo o I/O.
- Distinguir sincronização concluída de sincronização parcial; só remover entradas ausentes depois de uma enumeração bem-sucedida.
- Evitar duplicados e preservar IDs estáveis quando a mesma URI é novamente encontrada.
- Não carregar bitmaps grandes durante o scan; guardar referência ou cache dimensionada.

## Critérios de aceitação

- São indexadas músicas em todos os níveis abaixo da raiz e a hierarquia é fiel.
- Ficheiros não áudio são ignorados.
- Um ficheiro ilegível gera erro parcial e os restantes continuam a ser processados.
- Executar duas vezes sem alterações não cria duplicados nem altera desnecessariamente os IDs.
- Cancelar não deixa o índice marcado como completo nem remove registos válidos.
- Uma biblioteca grande não causa ANR nem execução de I/O na main thread.

## Testes

- Scanner com árvore vazia, profunda, nomes repetidos, MIME desconhecido e documento inacessível.
- Metadados completos, parciais e corrompidos.
- Reindexação idempotente e remoção apenas após scan completo.
- Teste de desempenho com uma coleção representativa definida no WI-01.

## Dependências

WI-03 e WI-04.

