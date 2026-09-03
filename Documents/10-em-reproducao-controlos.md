# WI-10 — Criar “Em reprodução” e validar controlos externos

## Objetivo

Dar controlo e feedback consistentes no telefone, notificação, lock screen e dispositivos Bluetooth.

## Trabalho

- Criar mini-player persistente e ecrã “Em reprodução”.
- Mostrar artwork/fallback, título, artista, progresso, duração, fila e estados play/pause/buffering/error.
- Ligar ações exclusivamente ao `MediaController`: play/pause, seek, anterior, seguinte, shuffle e repeat definido.
- Sincronizar posição com frequência suficiente sem recomposições excessivas.
- Rever comandos expostos pela sessão e ações da notificação.
- Validar botões AVRCP Bluetooth, headset e lock screen.
- Tratar ligação tardia/perdida ao serviço e restauração da UI.

## Critérios de aceitação

- Todos os controlos apresentam o mesmo estado e item corrente.
- Seek e duração funcionam com faixas de duração conhecida; streams/valores desconhecidos degradam corretamente.
- Botões Bluetooth executam play/pause/anterior/seguinte conforme suportado pelo dispositivo.
- A UI recupera ao serviço estar temporariamente indisponível.
- Artwork ausente/corrompido não bloqueia o ecrã nem a notificação.

## Testes

- Testes de ViewModel com eventos da sessão.
- Compose UI para playing, paused, buffering, erro e metadata incompleta.
- Matriz manual: notificação, lock screen, headset com fio e pelo menos um dispositivo Bluetooth.

## Dependências

WI-07 e WI-08.

