# WI-07 — Implementar o serviço de reprodução

## Objetivo

Centralizar a reprodução em ExoPlayer/Media3 e mantê-la ativa fora da interface.

## Trabalho

- Criar `MediaLibraryService`, `MediaLibrarySession` e uma única instância de ExoPlayer gerida pelo serviço.
- Declarar service, permissões de foreground/media playback e tipos exigidos no manifest.
- Converter faixas indexadas em `MediaItem` com URI, metadata e IDs estáveis.
- Implementar prepare, play, pause, seek, anterior, seguinte e tratamento de fim/erro.
- Publicar metadata e estado corretos na sessão e notificação multimédia.
- Definir lifecycle: quando iniciar foreground, quando manter/parar serviço e libertar recursos.
- Ligar a aplicação por `MediaController`, sem expor o ExoPlayer diretamente à UI.
- Configurar audio focus, “becoming noisy”/desligar auscultadores e comportamento de interrupções.
- Tratar URI que deixou de existir, avançando ou parando com erro compreensível.

## Critérios de aceitação

- A música continua com a Activity em background e o ecrã desligado.
- Controlos de lock screen/notificação refletem e alteram o estado real.
- Não existem dois players ao recriar a Activity ou voltar à aplicação.
- Pausas por audio focus e desconexão de saída respeitam a política definida.
- Fechar deliberadamente a sessão liberta player, notificação e serviço.

## Testes

- Testes do callback da sessão e conversão de metadata.
- Teste manual com ecrã desligado, process recreation, chamada/interrupção e auscultadores.
- Teste de ficheiro removido enquanto está na fila.

## Dependências

WI-05.

