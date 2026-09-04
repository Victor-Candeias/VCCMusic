# WI-05 — Construir o scanner e indexar a biblioteca

**Estado:** Em validação

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

## Implementação atual

- `SafMusicScanner` percorre a árvore SAF iterativamente, com conjunto de URIs visitadas para evitar ciclos anómalos.
- MIME types de áudio são preferidos; extensões conhecidas são usadas apenas quando o provider não fornece um MIME útil.
- Metadados são lidos com `MediaMetadataRetriever`, com fallback para o nome do ficheiro e artwork embutido limitado a 512 KiB.
- O I/O ocorre fora da transação Room; os resultados só são aplicados numa transação após uma enumeração concluída.
- Reindexações preservam IDs existentes por URI, removem apenas entradas ausentes após sucesso e devolvem progresso/erros parciais.
- O scanner está ligado ao `AppContainer` e pode ser cancelado por coroutine.
- `assembleDebug`, testes unitários e lint executam com sucesso após a implementação.
- O teste instrumentado de navegação (`connectedDebugAndroidTest`) executa com sucesso no emulador `Pixel_10a (AVD) - 17`.

## Dependências

WI-03 e WI-04.
