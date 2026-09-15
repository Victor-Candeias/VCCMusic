# WI-16 — Corrigir a consistência dos IDs de reprodução

## Estado

Concluído para a correção dos IDs. A investigação das ligações duplicadas não
identificou uma segunda criação explícita de controlador no código atual e
permanece documentada para validação no dispositivo.

## Objetivo

Garantir que a UI e o `MusicPlaybackService` usam o mesmo formato de `mediaId`
para uma faixa, evitando divergências ao sincronizar a fila, a faixa atual e o
estado do player depois de trocar de ecrã ou reiniciar a aplicação.

## Evidência

O log exportado em `Logs/vccmusic-diagnostic-20260911174508.log.txt` contém
IDs numéricos (`mediaId=1267`) e IDs com prefixo (`mediaId=track:1554`).
O serviço já usa `track:<id>`, enquanto o `PlaybackViewModel` construía
`MediaItem` com o ID numérico.

## Trabalho

- Centralizar a construção do `mediaId` de uma faixa em `MediaIds`.
- Usar `track:<id>` ao construir faixas no `PlaybackViewModel`.
- Usar o mesmo formato no estado publicado pela UI antes de o controlador
  confirmar a alteração.
- Rever a sequência de ligações `MediaController` duplicadas observada no log,
  sem esconder eventos reais através de deduplicação no logger.

## Critérios de aceitação

- Toda a faixa local publicada pela UI ou pelo serviço tem `mediaId=track:<id>`.
- O estado da UI usa o mesmo ID da faixa atualmente exposta pelo serviço.
- A seleção de uma faixa, a reconstrução da fila e a reabertura da aplicação
  não alternam entre IDs numéricos e IDs com prefixo.
- As ligações duplicadas do controlador ficam explicadas ou corrigidas; o log
  continua a registar ligações e desligamentos reais.
- A reprodução de rádios mantém os IDs `radio:<url>` sem alterações.

## Testes

- Teste unitário da construção do ID de faixa.
- Teste da fila/estado para confirmar a correspondência do `mediaId`.
- Verificação manual após abrir a aplicação, selecionar uma faixa, trocar de
  faixa e reabrir a aplicação.
- Verificação manual com a aplicação em background e com a sessão multimédia.

## Implementação atual

- `MediaIds.track` é a fonte partilhada para IDs de faixas.
- `PlaybackViewModel` usa `MediaIds.track` ao publicar o estado e ao criar
  `MediaItem`.
- `MusicPlaybackService` usa a mesma fonte ao construir a fila e ao resolver
  itens navegáveis.
- A investigação das ligações duplicadas permanece limitada ao diagnóstico:
  o código da aplicação cria um `MediaController` no `PlaybackViewModel`; não
  foi adicionada uma deduplicação que pudesse mascarar a causa.

**Conclusão:** WI-16 finalizado para a normalização dos IDs.
