# Arquitetura inicial

O VCCMusic começa como uma aplicação Android de módulo único, organizada por responsabilidades. A modularização Gradle deve ser introduzida apenas quando existirem limites e tempos de build que a justifiquem.

## Pacotes

- `ui`: Compose, navegação, estado e ViewModels.
- `domain`: modelos e regras sem dependência da interface.
- `data`: contratos e implementações de persistência.
- `scanner`: acesso SAF e indexação de documentos.
- `playback`: Media3, sessão e controlo de reprodução.
- `di`: composition root e construção das dependências.

## Injeção de dependências

A versão inicial usa injeção manual através de `AppContainer`, propriedade da classe `Application`. Esta opção mantém a construção explícita e evita uma framework antes de existirem grafos de dependências complexos. A decisão pode ser revista se o número de componentes ou scopes tornar a manutenção manual onerosa.

## Toolchain

- `compileSdk` e `targetSdk`: API 37; `minSdk`: API 26.
- Android Gradle Plugin: 9.2.1; Gradle Wrapper: 9.4.1.
- Java bytecode e Kotlin JVM target: 17.
- O lint pode sugerir AGP 9.4 e Gradle 9.7.1. A atualização fica adiada porque o Android Studio 2026.1.1 instalado suporta oficialmente AGP até 9.2. O par AGP 9.2/Gradle 9.4.1 é o recomendado para API 37 nesta linha da IDE.

## Regras

- A UI comunica com o playback através de um controller, nunca diretamente com o ExoPlayer.
- A UI lê dados através de repositórios e não conhece DAOs nem `ContentResolver`.
- URIs SAF permanecem `content://` em todas as camadas.
- Regras de domínio devem poder ser testadas na JVM sem Activity ou emulador.
