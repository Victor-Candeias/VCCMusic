# WI-12 — Publicar a biblioteca no Android Auto

## Objetivo

Expor uma hierarquia Media3 segura para condução usando o mesmo serviço e dados da aplicação.

## Trabalho

- Declarar compatibilidade e metadata de media app no manifest conforme as APIs Android Automotive/Auto aplicáveis.
- Implementar catálogo no callback do `MediaLibrarySession`: raiz, Pastas, Todas as músicas, Playlists e Reprodução aleatória.
- Marcar corretamente cada `MediaItem` como navegável, reproduzível ou ambos.
- Implementar paginação/parâmetros das chamadas de browsing e resultados assíncronos quando necessário.
- Usar IDs estáveis que não revelem URIs e resolver esses IDs para Room.
- Aplicar os mesmos `QueueBuilder`, shuffle, erros e estado da sessão usados no telefone.
- Fornecer ícones/artwork compatíveis, rápidos e com fallback.
- Garantir que navegação não exige interação táctil não suportada no automóvel.
- Testar com Desktop Head Unit e, quando possível, veículo/unidade real.

## Critérios de aceitação

- O DHU encontra a app, navega em todas as categorias e inicia faixas.
- Pastas e playlists vazias têm comportamento válido.
- Play/pause/anterior/seguinte e metadata permanecem sincronizados entre telefone e DHU.
- Catálogos grandes não bloqueiam o serviço e respeitam paginação.
- Reiniciar ligação ao Android Auto não cria outro player nem perde indevidamente a sessão.

## Testes

- Testes do callback para root, children, item e pedido de reprodução.
- Matriz DHU: ligação inicial, reconnect, biblioteca vazia, permissão perdida, shuffle e playlist.
- Validação das checklists oficiais para media apps em carros antes da release.

## Dependências

WI-08, WI-09 e WI-10.

