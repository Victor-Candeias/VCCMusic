# WI-01 — Fechar decisões e âmbito do MVP

**Estado:** Concluído em 3 de setembro de 2026  
**Decisões:** [ADR-001 — Decisões iniciais do produto](ADR-001-decisoes-iniciais.md)

## Objetivo

Eliminar ambiguidades que influenciam toda a implementação antes de criar o projeto Android.

## Trabalho concluído

- [x] Nome público e nome do projeto: **VCCMusic**.
- [x] `applicationId`: `pt.vcc.vccmusic`.
- [x] Removida do relatório a dependência de um caminho local antigo.
- [x] `minSdk`: API 26; `compileSdk` e `targetSdk` serão fixados no WI-02 para a versão estável compatível com as ferramentas selecionadas.
- [x] Formatos de áudio: os suportados pelo Media3 e pelos codecs presentes no dispositivo; diferenças por formato/fabricante serão documentadas.
- [x] Âmbito incluído e excluído do MVP definido.
- [x] Ordenação e fallbacks de metadados definidos.
- [x] “Tocar pasta” limitado às músicas diretamente contidas na pasta aberta.
- [x] URI `content://` adotado como identidade da faixa.
- [x] Ficheiros removidos, movidos ou inacessíveis são omitidos da biblioteca e das playlists após sincronização válida.
- [x] Privacidade local e ausência de telemetria definidas.

## Entregáveis

- Pequeno registo de decisões (ADR ou secção de documentação) aprovado.
- Lista explícita de requisitos incluídos e excluídos do MVP.

## Critérios de aceitação

- Nome, package, SDK mínimo e âmbito não contêm placeholders.
- Existe uma resposta documentada para metadados ausentes, URIs inválidos e perda da permissão SAF.
- Nenhuma decisão obriga a usar caminhos de filesystem em vez de `content://`.

Todos os critérios foram satisfeitos no ADR associado. O desenho detalhado do painel “Em reprodução” pertence ao WI-10 e não bloqueia este item.

## Dependências

Nenhuma.

