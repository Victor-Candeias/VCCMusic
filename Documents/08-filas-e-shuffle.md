# WI-08 — Implementar filas e reprodução aleatória

## Objetivo

Criar filas previsíveis para toda a biblioteca e para a pasta aberta, com shuffle apenas na sessão ativa.

## Trabalho

- Definir um `QueueBuilder` testável para “todas as músicas”, “pasta atual” e seleção explícita.
- Esclarecer que “pasta atual” inclui apenas as faixas diretamente contidas ou também descendentes; aplicar a decisão do WI-01.
- Manter uma ordem base determinística e iniciar pela faixa selecionada quando aplicável.
- Ativar/desativar shuffle através do player, sem reordenar registos Room nem posições de playlists.
- Preservar item corrente e posição ao alternar shuffle sempre que o Media3 o permitir.
- Definir repeat off/one/all, mesmo que a UI exponha inicialmente apenas a opção escolhida para o MVP.
- Atualizar comandos e metadata da sessão quando a fila mudar.
- Tratar fila vazia, entradas que desapareceram e alterações da biblioteca durante playback.

## Critérios de aceitação

- “Tocar tudo” usa todas e apenas as faixas da biblioteca ativa.
- “Tocar pasta” respeita exatamente o âmbito documentado.
- Shuffle não altera qualquer ordem persistida.
- Com shuffle desligado, a ordem original volta a estar disponível.
- Filas vazias e URIs inválidas não causam crash.

## Testes

- Testes unitários de composição da fila, item inicial, ordem e deduplicação.
- Testes de alternância de shuffle e repeat.
- Teste de remoção do item corrente e de itens futuros.

## Dependências

WI-06 e WI-07.

