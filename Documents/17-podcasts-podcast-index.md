# WI-17 — Implementar podcasts com Podcast Index

## Estado

Implementado (com limitações documentadas).

## Objetivo

Integrar podcasts através da Podcast Index API, permitindo pesquisar
programas, consultar episódios, reproduzir áudio em background e apresentar
uma biblioteca navegável no Android Auto.

A implementação deve reutilizar o `MusicPlaybackService`, o ExoPlayer e a
`MediaLibrarySession` existentes. Não deve ser criado um segundo player ou
outro serviço multimédia.

## Segurança das credenciais

Durante o desenvolvimento, a API Key e a API Secret ficam em
`local.properties`, que está excluído pelo `.gitignore`:

```properties
PODCAST_INDEX_API_KEY=...
PODCAST_INDEX_API_SECRET=...
```

O Gradle deve disponibilizar estes valores através de `BuildConfig`. Não
colocar credenciais no código-fonte, no `AndroidManifest.xml` ou na
documentação. A API Secret embutida num APK pode ser extraída; uma versão
publicada deve usar um backend/proxy.

## Dependências

- WI-07 — serviço de reprodução Media3.
- WI-10 — ecrã e controlos de reprodução.
- WI-12 — integração Android Auto.
- Credenciais válidas da Podcast Index em `local.properties`.

## Fases de implementação

### Fase 1 — Configuração e autenticação

- Adicionar Retrofit e OkHttp ao módulo `app`.
- Ativar `BuildConfig` no `app/build.gradle.kts`.
- Ler `PODCAST_INDEX_API_KEY` e `PODCAST_INDEX_API_SECRET` de
  `local.properties`.
- Criar o cálculo SHA-1 exigido pela Podcast Index.
- Criar um interceptor que envie `User-Agent`, `X-Auth-Key`, `X-Auth-Date` e
  `Authorization`.
- Configurar timeouts e tratamento explícito de respostas HTTP.
- Confirmar a permissão `INTERNET` no manifest.

### Fase 2 — Cliente API e modelos

Criar a camada:

```text
podcast/
├── model/
│   ├── PodcastFeed.kt
│   └── PodcastEpisode.kt
├── network/
│   ├── PodcastIndexApi.kt
│   ├── PodcastIndexAuth.kt
│   ├── PodcastIndexAuthInterceptor.kt
│   └── PodcastIndexClient.kt
└── repository/
    └── PodcastRepository.kt
```

Implementar inicialmente:

- `search/byterm`;
- `episodes/byfeedid`;
- validação de `feedId`;
- seleção de episódios com `enclosureUrl` válido;
- tratamento de feeds sem imagem e episódios sem duração.

### Fase 3 — Pesquisa e detalhe na aplicação

Adicionar a navegação Compose:

```text
Podcasts
├── Pesquisa
├── Resultados
└── Detalhe do podcast
    └── Episódios
```

Cada resultado deve apresentar nome, autor, imagem disponível e descrição
resumida. Cada episódio deve apresentar título, data, duração, imagem e estado
de reprodução quando disponível.

### Fase 4 — Reprodução dos episódios

- Converter episódios em `MediaItem`.
- Usar IDs estáveis e distintos:

```text
track:<id>                  música local
radio:<url>                 rádio online
podcast:feed:<feedId>      podcast
podcast:episode:<id>       episódio
```

- Reutilizar o `MusicPlaybackService` existente.
- Reproduzir o `enclosureUrl` com Media3/ExoPlayer.
- Suportar pause, resume, seek, anterior e seguinte.
- Tratar URL inválido, erro de rede e episódio sem áudio sem fechar a
  aplicação.
- Manter metadata e artwork do episódio na sessão multimédia.

### Fase 5 — Biblioteca Media3 e Android Auto

Adicionar Podcasts à árvore da `MediaLibrarySession`:

```text
ROOT
├── Música
├── Rádios
└── Podcasts
    ├── Favoritos
    ├── Continuar a ouvir
    ├── Episódios recentes
    └── Pesquisar
```

O Android Auto deve conseguir navegar até um episódio e iniciar a reprodução
sem depender da Activity aberta.

### Fase 6 — Persistência

Adicionar entidades Room para:

- podcasts favoritos;
- episódios favoritos, se o produto os suportar;
- progresso dos episódios;
- data da última reprodução;
- posição atual e duração conhecida.

O progresso deve ser atualizado sem bloquear a reprodução e restaurado quando
o episódio voltar a ser selecionado.

### Fase 7 — Testes e robustez

- Testar o cálculo SHA-1 com timestamp conhecido.
- Testar headers e autenticação do interceptor.
- Testar desserialização de feeds e episódios incompletos.
- Testar pesquisa, detalhe e reprodução de um episódio real.
- Testar reprodução em background e após recriação da Activity.
- Testar erro de rede, timeout, URL sem áudio e imagem ausente.
- Testar navegação e reprodução através do Android Auto.
- Confirmar que música local, rádio e podcast continuam a usar um único player.

## Critérios de aceitação

- A API Key e a Secret são lidas apenas de `local.properties` durante o
  desenvolvimento e não aparecem no repositório.
- A pesquisa por termo devolve podcasts válidos ou um estado de erro
  recuperável.
- Um podcast permite consultar e reproduzir os seus episódios.
- Episódios reproduzem em background através do serviço existente.
- Os IDs de música, rádio e podcast não colidem.
- A artwork disponível é apresentada na UI e na sessão multimédia.
- Episódios sem imagem ou duração continuam utilizáveis.
- O Android Auto apresenta Podcasts e permite iniciar episódios.
- Favoritos e progresso persistem entre reinícios da aplicação.
- Erros de rede e da API são visíveis e não produzem estados de sucesso falsos.

## Ordem recomendada

```text
Credenciais/BuildConfig
    ↓
Autenticação SHA-1
    ↓
Cliente Retrofit/OkHttp
    ↓
Pesquisa e episódios
    ↓
Reprodução Media3
    ↓
UI de podcasts
    ↓
MediaLibrarySession/Android Auto
    ↓
Favoritos e progresso
    ↓
Testes de dispositivo
```

## Primeiro incremento verificável

O primeiro incremento deve limitar-se a:

1. ler as credenciais;
2. autenticar uma chamada;
3. executar `search/byterm`;
4. obter `episodes/byfeedid`;
5. reproduzir um `enclosureUrl` através do player existente.

Só depois deste percurso funcionar devem ser adicionados Android Auto,
favoritos e progresso.

## Implementação atual

- As credenciais de desenvolvimento são lidas de `local.properties` e
  disponibilizadas através de `BuildConfig`.
- Retrofit e OkHttp estão configurados com autenticação Podcast Index,
  timeouts e headers obrigatórios.
- Existem modelos, API e repository para pesquisa e episódios por `feedId`.
- Foi criado um ecrã de pesquisa/detalhe de podcasts com reprodução de
  episódios através do `PlaybackViewModel` existente.
- Os episódios usam IDs `podcast:episode:<id>` e não provocam a sincronização
  da fila de música local.
- Android Auto apresenta a categoria de podcasts, os podcasts favoritos e os
  episódios disponíveis, usando os mesmos IDs e o mesmo serviço de reprodução.
- Podcasts favoritos persistem em Room e o progresso é gravado periodicamente
  durante a reprodução e restaurado ao selecionar novamente o episódio.
- Continua pendente a pesquisa de podcasts diretamente no Android Auto e o
  carregamento de artwork remoto; a UI mantém a imagem fornecida pela API
  quando já estiver disponível localmente.
