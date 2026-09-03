# Relatório — Aplicação de música para Android e Android Auto

**Data:** 3 de setembro de 2026  
**Projeto:** `VCCMusic`

## 1. Objetivo

Desenvolver uma aplicação para Android e Android Auto que reproduza ficheiros de música existentes numa pasta escolhida pelo utilizador.

A aplicação deverá permitir:

- Ler ficheiros de música numa pasta e em todas as suas subpastas.
- Apresentar as pastas existentes na raiz selecionada.
- Ao abrir uma pasta, mostrar as músicas e subpastas nela contidas.
- Mostrar numa lista todas as músicas existentes abaixo da raiz.
- Reproduzir aleatoriamente todas as músicas.
- Reproduzir as músicas da pasta atualmente aberta.
- Criar listas de reprodução personalizadas.
- Reproduzir apenas as músicas de uma lista selecionada.
- Ativar reprodução aleatória nas filas anteriores.
- Continuar a reprodução em segundo plano e com o ecrã desligado.
- Ser controlada por Bluetooth e pelo Android Auto.

## 2. Opção de desenvolvimento recomendada

A opção recomendada é o desenvolvimento Android nativo, utilizando:

- **Kotlin** como linguagem principal.
- **Jetpack Compose** para a interface no telemóvel.
- **AndroidX Media3 e ExoPlayer** para reprodução de áudio.
- **MediaLibraryService** e **MediaLibrarySession** para reprodução em segundo plano, controlos externos e Android Auto.
- **Room** para guardar o índice das músicas, playlists e preferências.
- **Storage Access Framework (SAF)** para permitir que o utilizador escolha a pasta de música.
- **WorkManager** para reindexações periódicas ou operações mais demoradas em segundo plano.

Esta solução é preferível a Flutter, React Native ou .NET MAUI para este projeto porque a reprodução em segundo plano, o acesso ao armazenamento e a integração com Android Auto dependem diretamente das APIs nativas do Android.

## 3. Arquitetura proposta

```text
Aplicação
├── Interface Jetpack Compose
│   ├── Pastas
│   ├── Todas as músicas
│   ├── Playlists
│   └── Em reprodução
│
├── Biblioteca de música
│   ├── Scanner de diretórios
│   ├── Índice Room
│   └── Leitura de metadados
│
└── PlaybackService
    ├── MediaLibraryService
    ├── MediaLibrarySession
    └── ExoPlayer
```

O serviço de reprodução deve ser independente da interface. Assim, a música pode continuar a tocar quando a aplicação deixa de estar visível, e o Android Auto, a notificação do sistema ou um dispositivo Bluetooth podem controlar o mesmo player.

## 4. Acesso aos ficheiros

### Solução recomendada: Storage Access Framework

Na primeira utilização, a aplicação apresenta o seletor de pastas do Android. O utilizador escolhe, por exemplo:

```text
Music/MinhaColecao
```

A aplicação guarda uma autorização persistente para o URI da pasta e percorre os seus ficheiros e subpastas.

Os ficheiros devem ser identificados por URIs no formato `content://`, e não por caminhos tradicionais como `/storage/emulated/0/...`.

### Limitação importante

A partir do Android 11, o seletor de pastas não permite conceder acesso através de `ACTION_OPEN_DOCUMENT_TREE`:

- À raiz completa do armazenamento interno.
- À raiz de determinados cartões SD.
- À pasta `Download` completa.
- Às pastas protegidas `Android/data` e `Android/obb`.

Uma subpasta dedicada, como `Music/MinhaColecao`, é a escolha mais adequada.

### Alternativa: MediaStore

Se o objetivo mudar para encontrar toda a música reconhecida pelo dispositivo, independentemente de uma raiz escolhida, pode ser utilizado o `MediaStore`. A hierarquia de pastas poderá ser reconstruída através do campo `RELATIVE_PATH`.

Para os requisitos atuais, recomenda-se **SAF para selecionar a raiz e Room para indexar o seu conteúdo**.

## 5. Base de dados

Modelo inicial sugerido:

```text
MusicFolder
- id
- parentId
- name
- uri

Track
- id
- folderId
- title
- artist
- album
- duration
- uri
- artworkUri

Playlist
- id
- name

PlaylistTrack
- playlistId
- trackId
- position
```

O índice em Room evita percorrer toda a árvore de ficheiros cada vez que uma página é apresentada. Deve existir uma operação para atualizar ou reconstruir o índice quando o conteúdo da pasta for alterado.

## 6. Implementação das funcionalidades

| Funcionalidade | Implementação sugerida |
|---|---|
| Ler a raiz e as subpastas | Scanner baseado no SAF e índice em Room |
| Mostrar pastas e conteúdo | Relação `parentId` entre pastas |
| Mostrar todas as músicas | Consulta de todas as faixas indexadas em Room |
| Tocar todas as músicas | Criar uma fila com todas as faixas |
| Tocar a pasta atual | Criar uma fila filtrada pelo identificador da pasta |
| Criar playlists | Tabelas `Playlist` e `PlaylistTrack` |
| Tocar uma playlist | Criar uma fila com as faixas dessa playlist |
| Reprodução aleatória | Ativar o modo shuffle do ExoPlayer na fila atual |
| Reprodução em segundo plano | `MediaLibraryService` como foreground service |
| Controlos Bluetooth e do sistema | `MediaLibrarySession` |
| Android Auto | Biblioteca publicada pelo `MediaLibraryService` |

A ordem aleatória não deverá modificar permanentemente a ordem armazenada na base de dados. O shuffle deve ser aplicado à fila ativa do player.

## 7. Android Auto

Não é criada uma interface Jetpack Compose específica para o ecrã do automóvel. A aplicação publica uma hierarquia de conteúdos e o Android Auto constrói uma interface apropriada e segura para condução.

Exemplo de hierarquia:

```text
Biblioteca
├── Pastas
│   ├── Rock
│   ├── Jazz
│   └── Concertos
├── Todas as músicas
├── Playlists
│   ├── Viagem
│   └── Favoritas
└── Reprodução aleatória
```

As pastas e categorias são elementos navegáveis; as músicas são elementos reproduzíveis. O Android Auto liga-se ao `MediaLibraryService`, consulta esta árvore e envia os comandos de reprodução através da sessão multimédia.

## 8. Software necessário

### Indispensável

1. **Android Studio**, na versão estável.
2. **Android SDK**.
3. Uma plataforma Android recente.
4. **Android SDK Build-Tools**.
5. **Android SDK Platform-Tools**, que inclui o `adb`.
6. **Android SDK Command-line Tools**.
7. Um telemóvel Android ou o Android Emulator.

Durante a instalação do Android Studio devem ficar selecionados o Android SDK, Android SDK Platform, Android Virtual Device e Android Emulator.

Não é necessário instalar separadamente:

- JDK, porque o Android Studio inclui uma versão compatível.
- Kotlin, porque é configurado através dos plugins do projeto.
- Gradle, porque o projeto utiliza o Gradle Wrapper.

### Recomendado

- **Git for Windows** para controlo de versões.
- Um telemóvel Android real para testar ficheiros, permissões, áudio, Bluetooth e execução em segundo plano.
- Pelo menos **16 GB de RAM e 16 GB de espaço livre** para utilizar confortavelmente o Android Emulator.

## 9. Preparação de um telemóvel para desenvolvimento

1. Ativar as Opções de programador.
2. Ativar a Depuração USB ou a depuração por Wi-Fi.
3. Ligar o telemóvel ao computador.
4. Autorizar a chave de depuração apresentada no dispositivo.
5. No Windows, instalar o driver USB do fabricante caso o dispositivo não seja detetado.

A aplicação pode depois ser compilada e executada diretamente pelo Android Studio.

## 10. Ferramentas de teste para Android Auto

No Android Studio, através de **Tools → SDK Manager → SDK Tools**, deve ser instalado:

- **Android Auto Desktop Head Unit Emulator (DHU)**.

O DHU simula no computador o ecrã da unidade principal do automóvel. Normalmente será também necessário:

- Um telemóvel compatível com o Android Auto atualizado.
- Ativar o modo de programador do Android Auto.
- Instalar a aplicação no telemóvel.
- Ligar o telemóvel ao computador por USB ou através de encaminhamento `adb`.

## 11. Configuração inicial do projeto

No Android Studio:

```text
New Project
→ Empty Activity
→ Language: Kotlin
→ Build configuration: Kotlin DSL
→ Use Jetpack Compose: sim
```

Configuração inicial sugerida:

```text
Nome: VCCMusic
Package name: pt.vcc.vccmusic
Minimum SDK: API 26
```

A API 26 é um ponto de partida pragmático, não uma exigência absoluta. A versão mínima definitiva deve ser escolhida considerando os dispositivos que se pretende suportar e os requisitos das bibliotecas utilizadas.

Dependências funcionais previstas:

- Jetpack Compose.
- Media3 ExoPlayer.
- Media3 Session.
- Room.
- WorkManager.
- DocumentFile ou consultas diretas ao `DocumentsContract`.

## 12. Nome da aplicação

O nome público e o nome do projeto Android escolhidos são **VCCMusic**. O identificador da aplicação é `pt.vcc.vccmusic`.

### Alternativas anteriormente consideradas

Foram considerados os seguintes nomes:

- **FolderBeat** — associa diretamente pastas e música e funciona em português e inglês.
- **SomLocal** — simples, claro e português.
- **PastaPlay** — descritivo e fácil de memorizar.
- **RootMusic** — relacionado com a pasta raiz.
- **LocalBeats** — moderno e internacional.
- **DriveTunes** — realça a utilização no automóvel.
- **MusicFolders** — muito explícito.
- **MyMusic Drive** — pessoal e orientado para condução.
- **RoadBeat** — nome mais comercial.
- **TuneFolder** — tecnológico e acessível.
- **SonicRoot** — distinto e adequado para criar uma marca.
- **RitmoLocal** — português e amigável.
- **AutoBeats** — associado ao Android Auto, mas mais limitador para a identidade geral.

As três alternativas anteriormente preferidas eram:

1. **FolderBeat** — melhor equilíbrio geral e recomendação principal.
2. **SomLocal** — melhor opção para uma identidade portuguesa.
3. **SonicRoot** — melhor opção para uma marca mais distinta.

Antes da publicação deverá ser verificado se **VCCMusic** já existe na Google Play Store e se entra em conflito com alguma marca registada.

## 13. Recomendação final

O projeto deverá começar como uma aplicação Android nativa em Kotlin, com Compose para a interface do telefone e Media3 desde a primeira versão. A seleção da raiz será feita através do SAF, o conteúdo será indexado em Room e um único `MediaLibraryService` será responsável por todas as filas e pela reprodução.

O Android Auto utilizará a mesma biblioteca, playlists e estado de reprodução da aplicação no telemóvel. Esta base permitirá acrescentar posteriormente pesquisa, favoritos, capas, histórico, retoma da última faixa, temporizador e outras funcionalidades.

## 14. Referências oficiais

- [Instalar o Android Studio](https://developer.android.com/studio/install)
- [Executar aplicações num dispositivo físico](https://developer.android.com/studio/run/device)
- [Android Emulator](https://developer.android.com/studio/run/emulator)
- [Storage Access Framework](https://developer.android.com/training/data-storage/shared/documents-files)
- [MediaLibraryService](https://developer.android.com/media/media3/session/serve-content)
- [Aplicações multimédia para automóveis](https://developer.android.com/training/cars/media)
- [Testar Android Auto com o Desktop Head Unit](https://developer.android.com/training/cars/testing/dhu)
