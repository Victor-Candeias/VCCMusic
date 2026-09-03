# WI-02 — Criar o projeto Android base

**Estado:** Concluído em 3 de setembro de 2026  
**Arquitetura:** [Arquitetura inicial](ARQUITETURA.md)  
**Build:** [Compilar e executar](BUILD.md)

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

## Validação de fecho

Validado em 3 de setembro de 2026 contra o estado atual do projeto:

- `assembleDebug` e `assembleRelease` executam com sucesso.
- Os testes unitários, o lint e `connectedDebugAndroidTest` executam com sucesso no emulador `Pixel_10a (AVD) - 17`.
- A aplicação foi instalada e iniciada no emulador; a `MainActivity` apresentou a shell de navegação e o teste confirmou os destinos Biblioteca, Playlists e Em reprodução.
- O catálogo de versões fixa as versões das dependências e dos plugins.
- A arquitetura está separada por responsabilidades e usa injeção manual através de `AppContainer`, permitindo testar lógica JVM sem iniciar uma `Activity`.
- Não foram encontrados segredos, caminhos absolutos ou versões dinâmicas na configuração.

**Conclusão:** WI-02 finalizado e desbloqueia os WI-03 e WI-04.

## Dependências

WI-01.
