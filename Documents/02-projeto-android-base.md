# WI-02 — Criar o projeto Android base

## Objetivo

Obter uma aplicação Android nativa mínima, reproduzível e preparada para crescer sem acoplar interface, dados e reprodução.

## Trabalho

- Criar projeto Kotlin com Gradle Kotlin DSL, Compose e o SDK decidido no WI-01.
- Adicionar Gradle Wrapper e catálogo de versões.
- Configurar Compose, Navigation, coroutines/Flow, Room/KSP, Media3, WorkManager e DocumentFile.
- Separar responsabilidades, no mínimo, em pacotes `ui`, `domain`, `data`, `scanner` e `playback`; criar módulos adicionais apenas se o custo se justificar.
- Configurar injeção de dependências manual ou por biblioteca escolhida e documentar a opção.
- Criar temas claro/escuro e estrutura de navegação vazia para Biblioteca, Playlists e Em reprodução.
- Configurar testes unitários, instrumentados, Compose UI, lint e regras básicas de formatação.
- Adicionar `.gitignore`, configuração de build debug/release e instruções de compilação.
- Evitar segredos, caminhos absolutos e versões dinâmicas nas configurações.

## Critérios de aceitação

- `gradlew assembleDebug`, testes unitários e lint executam com sucesso num checkout limpo.
- A aplicação abre num emulador/dispositivo suportado e apresenta a shell de navegação.
- Dependências têm versões fixas e compatíveis.
- A arquitetura permite testar repositórios e regras de fila sem iniciar uma Activity.

## Testes

- Smoke test de arranque.
- Teste de navegação entre destinos vazios.
- Verificação de build debug e release sem assinatura de produção.

## Dependências

WI-01.

