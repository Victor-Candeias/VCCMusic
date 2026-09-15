# Podcast Index em Android + Android Auto (Kotlin)

## Objetivo

Este documento mostra uma forma prática de integrar a **Podcast Index API** numa aplicação Android escrita em **Kotlin**, com reprodução de episódios através de **Jetpack Media3 / ExoPlayer** e suporte a **Android Auto** através de `MediaLibraryService`.

A arquitetura proposta é:

```text
Android App
   |
   +-- UI (Compose ou Views)
   |
   +-- PodcastRepository
   |      |
   |      +-- Retrofit
   |      +-- Podcast Index API
   |
   +-- PlaybackService : MediaLibraryService
          |
          +-- MediaLibrarySession
          +-- ExoPlayer
          +-- Android Auto
```

---

# 1. Pré-requisitos

Já deves ter:

- API Key do Podcast Index
- API Secret do Podcast Index
- Android Studio
- projeto Android em Kotlin
- acesso à Internet na app
- `minSdk` adequado ao teu projeto

Para Android Auto, a abordagem recomendada atualmente é usar **Jetpack Media3** com:

- `ExoPlayer`
- `MediaLibraryService`
- `MediaLibrarySession`

O `MediaLibraryService` permite que o Android Auto consulte a biblioteca da aplicação e apresente uma interface própria, adequada ao condutor.

---

# 2. Autenticação do Podcast Index

A Podcast Index API exige estes headers em cada pedido:

```text
User-Agent
X-Auth-Key
X-Auth-Date
Authorization
```

O `Authorization` é calculado assim:

```text
SHA1(API_KEY + API_SECRET + UNIX_TIMESTAMP)
```

Exemplo:

```text
API_KEY    = abc123
API_SECRET = xyz456
TIMESTAMP  = 1789500000

Authorization = SHA1("abc123xyz4561789500000")
```

O timestamp deve ser Unix Time em segundos.

---

# 3. Segurança da API Secret

## Durante desenvolvimento

Podes guardar temporariamente as credenciais em:

```properties
local.properties
```

Exemplo:

```properties
PODCAST_INDEX_API_KEY=xxxxxxxxxxxxxxxx
PODCAST_INDEX_API_SECRET=yyyyyyyyyyyyyyyy
```

Não faças commit deste ficheiro para um repositório público.

## Produção

**Não é seguro colocar uma API Secret diretamente dentro de um APK.**

Mesmo usando:

- `BuildConfig`
- resources
- ProGuard/R8
- obfuscation

a chave pode ser extraída.

Para uma aplicação publicada, a arquitetura preferível é:

```text
Android App
     |
     v
Teu Backend / Proxy
     |
     v
Podcast Index API
```

O backend guarda a API Key e Secret e a aplicação móvel nunca recebe a Secret.

Para desenvolvimento ou uma app pessoal, podes fazer a chamada diretamente a partir da app.

---

# 4. Dependências

No `build.gradle.kts` do módulo `app`:

```kotlin
dependencies {

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:<versao>")
    implementation("com.squareup.retrofit2:converter-gson:<versao>")

    // OkHttp
    implementation("com.squareup.okhttp3:okhttp:<versao>")
    implementation("com.squareup.okhttp3:logging-interceptor:<versao>")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:<versao>")

    // Media3 / ExoPlayer
    implementation("androidx.media3:media3-exoplayer:<versao>")
    implementation("androidx.media3:media3-session:<versao>")
    implementation("androidx.media3:media3-ui:<versao>")
}
```

Usa as versões estáveis atuais disponíveis no teu projeto.

---

# 5. Permissão de Internet

No `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

Para reprodução em background:

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

---

# 6. Disponibilizar as credenciais através do BuildConfig

No `build.gradle.kts`:

```kotlin
import java.util.Properties

val localProperties = Properties()

val localPropertiesFile = rootProject.file("local.properties")

if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

android {

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {

        buildConfigField(
            "String",
            "PODCAST_INDEX_API_KEY",
            "\"${localProperties["PODCAST_INDEX_API_KEY"] ?: ""}\""
        )

        buildConfigField(
            "String",
            "PODCAST_INDEX_API_SECRET",
            "\"${localProperties["PODCAST_INDEX_API_SECRET"] ?: ""}\""
        )
    }
}
```

Depois:

```kotlin
val apiKey = BuildConfig.PODCAST_INDEX_API_KEY
val apiSecret = BuildConfig.PODCAST_INDEX_API_SECRET
```

---

# 7. Função SHA-1 em Kotlin

Cria:

```text
network/PodcastIndexAuth.kt
```

Código:

```kotlin
package com.example.vccmusic.network

import java.security.MessageDigest

object PodcastIndexAuth {

    fun sha1(value: String): String {

        val digest = MessageDigest.getInstance("SHA-1")
            .digest(value.toByteArray(Charsets.UTF_8))

        return digest.joinToString("") {
            "%02x".format(it)
        }
    }
}
```

---

# 8. Criar um Interceptor de autenticação

Esta é uma das partes mais importantes.

Cria:

```text
network/PodcastIndexAuthInterceptor.kt
```

```kotlin
package com.example.vccmusic.network

import okhttp3.Interceptor
import okhttp3.Response

class PodcastIndexAuthInterceptor(
    private val apiKey: String,
    private val apiSecret: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {

        val timestamp =
            (System.currentTimeMillis() / 1000L).toString()

        val authorization = PodcastIndexAuth.sha1(
            apiKey + apiSecret + timestamp
        )

        val request = chain.request()
            .newBuilder()
            .header(
                "User-Agent",
                "VCCMusic/1.0"
            )
            .header(
                "X-Auth-Key",
                apiKey
            )
            .header(
                "X-Auth-Date",
                timestamp
            )
            .header(
                "Authorization",
                authorization
            )
            .build()

        return chain.proceed(request)
    }
}
```

A vantagem desta implementação é que **todas as chamadas Retrofit recebem automaticamente os headers corretos**.

---

# 9. Criar o cliente OkHttp

```kotlin
val authInterceptor =
    PodcastIndexAuthInterceptor(
        BuildConfig.PODCAST_INDEX_API_KEY,
        BuildConfig.PODCAST_INDEX_API_SECRET
    )

val okHttpClient =
    OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()
```

Durante desenvolvimento podes adicionar logging:

```kotlin
val loggingInterceptor =
    HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }
```

e depois:

```kotlin
val okHttpClient =
    OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .build()
```

Evita usar `BODY` em produção se existir informação que não deve aparecer nos logs.

---

# 10. Configurar Retrofit

Cria:

```text
network/PodcastIndexClient.kt
```

```kotlin
package com.example.vccmusic.network

import com.example.vccmusic.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object PodcastIndexClient {

    private const val BASE_URL =
        "https://api.podcastindex.org/api/1.0/"

    private val authInterceptor =
        PodcastIndexAuthInterceptor(
            BuildConfig.PODCAST_INDEX_API_KEY,
            BuildConfig.PODCAST_INDEX_API_SECRET
        )

    private val okHttpClient =
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .build()

    private val retrofit =
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()

    val api: PodcastIndexApi =
        retrofit.create(PodcastIndexApi::class.java)
}
```

---

# 11. Interface Retrofit

Cria:

```text
network/PodcastIndexApi.kt
```

```kotlin
package com.example.vccmusic.network

import retrofit2.http.GET
import retrofit2.http.Query

interface PodcastIndexApi {

    @GET("search/byterm")
    suspend fun searchPodcasts(
        @Query("q") query: String,
        @Query("max") max: Int = 25
    ): PodcastSearchResponse

    @GET("episodes/byfeedid")
    suspend fun getEpisodesByFeedId(
        @Query("id") feedId: Long,
        @Query("max") max: Int = 100
    ): EpisodeResponse
}
```

---

# 12. Modelos dos podcasts

Uma resposta de pesquisa contém normalmente uma lista `feeds`.

Exemplo de modelo simplificado:

```kotlin
data class PodcastSearchResponse(
    val status: String?,
    val count: Int?,
    val feeds: List<PodcastFeed> = emptyList()
)

data class PodcastFeed(
    val id: Long,
    val title: String?,
    val url: String?,
    val originalUrl: String?,
    val link: String?,
    val description: String?,
    val author: String?,
    val ownerName: String?,
    val image: String?,
    val artwork: String?,
    val language: String?,
    val categories: Map<String, String>?
)
```

Não precisas de mapear todos os campos da API.

Retrofit/Gson pode ignorar os restantes.

---

# 13. Modelos dos episódios

```kotlin
data class EpisodeResponse(
    val status: String?,
    val count: Int?,
    val items: List<PodcastEpisode> = emptyList()
)

data class PodcastEpisode(
    val id: Long,
    val title: String?,
    val link: String?,
    val description: String?,
    val datePublished: Long?,
    val datePublishedPretty: String?,
    val enclosureUrl: String?,
    val enclosureType: String?,
    val enclosureLength: Long?,
    val duration: Int?,
    val image: String?,
    val feedId: Long?,
    val feedTitle: String?
)
```

O campo mais importante para reprodução é normalmente:

```text
enclosureUrl
```

Este contém o URL do MP3, AAC ou outro conteúdo de áudio publicado pelo podcast.

---

# 14. Repository

Cria:

```text
repository/PodcastRepository.kt
```

```kotlin
package com.example.vccmusic.repository

import com.example.vccmusic.network.PodcastIndexApi

class PodcastRepository(
    private val api: PodcastIndexApi
) {

    suspend fun search(
        query: String
    ) = api.searchPodcasts(query)

    suspend fun getEpisodes(
        feedId: Long
    ) = api.getEpisodesByFeedId(feedId)
}
```

Instância:

```kotlin
val repository =
    PodcastRepository(
        PodcastIndexClient.api
    )
```

---

# 15. Pesquisar podcasts

Exemplo:

```kotlin
lifecycleScope.launch {

    try {

        val result =
            repository.search(
                "Metallica"
            )

        result.feeds.forEach {

            Log.d(
                "PODCAST",
                "${it.id} - ${it.title}"
            )
        }

    } catch (e: Exception) {

        Log.e(
            "PODCAST",
            "Erro",
            e
        )
    }
}
```

---

# 16. Obter episódios

Depois de selecionar um podcast:

```kotlin
lifecycleScope.launch {

    val result =
        repository.getEpisodes(
            feedId = podcast.id
        )

    result.items.forEach {

        Log.d(
            "EPISODE",
            "${it.title} -> ${it.enclosureUrl}"
        )
    }
}
```

---

# 17. Reprodução com Media3 / ExoPlayer

Para reproduzir um episódio:

```kotlin
val player =
    ExoPlayer.Builder(context)
        .build()
```

Cria um `MediaItem`:

```kotlin
val mediaItem =
    MediaItem.Builder()
        .setMediaId(
            episode.id.toString()
        )
        .setUri(
            episode.enclosureUrl
        )
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(
                    episode.title
                )
                .setArtist(
                    episode.feedTitle
                )
                .setArtworkUri(
                    episode.image?.let {
                        Uri.parse(it)
                    }
                )
                .build()
        )
        .build()
```

Depois:

```kotlin
player.setMediaItem(mediaItem)

player.prepare()

player.play()
```

---

# 18. Porque usar MediaLibraryService

Para Android Auto não é suficiente ter apenas um `ExoPlayer` dentro de uma Activity.

O recomendado é colocar:

```text
ExoPlayer
     +
MediaLibrarySession
     +
MediaLibraryService
```

O Android Auto liga-se ao serviço e consulta a biblioteca da aplicação.

Exemplo:

```text
Android Auto
     |
     v
MediaLibraryService
     |
     v
MediaLibrarySession
     |
     v
ExoPlayer
```

---

# 19. Criar o PlaybackService

Cria:

```text
playback/PlaybackService.kt
```

Estrutura base:

```kotlin
package com.example.vccmusic.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibrarySession
import androidx.media3.session.MediaSession

class PlaybackService :
    MediaLibraryService() {

    private lateinit var player: ExoPlayer

    private lateinit var session:
        MediaLibrarySession

    override fun onCreate() {

        super.onCreate()

        val audioAttributes =
            AudioAttributes.Builder()
                .setUsage(
                    C.USAGE_MEDIA
                )
                .setContentType(
                    C.AUDIO_CONTENT_TYPE_SPEECH
                )
                .build()

        player =
            ExoPlayer.Builder(this)
                .setAudioAttributes(
                    audioAttributes,
                    true
                )
                .build()

        session =
            MediaLibrarySession.Builder(
                this,
                player,
                PodcastLibraryCallback()
            )
                .build()
    }

    override fun onGetSession(
        controllerInfo:
            MediaSession.ControllerInfo
    ): MediaLibrarySession {

        return session
    }

    override fun onDestroy() {

        session.release()

        player.release()

        super.onDestroy()
    }
}
```

---

# 20. Biblioteca apresentada ao Android Auto

O Android Auto trabalha melhor quando os conteúdos são organizados numa árvore.

Exemplo:

```text
ROOT
 |
 +-- Podcasts
 |      |
 |      +-- Favoritos
 |      |
 |      +-- Recentes
 |      |
 |      +-- Tecnologia
 |      |
 |      +-- Música
 |
 +-- Rádios
```

Ao selecionar:

```text
Podcasts
   |
   +-- Podcast A
   |      |
   |      +-- Episódio 1
   |      +-- Episódio 2
   |
   +-- Podcast B
```

Os podcasts são itens `browsable`.

Os episódios são itens `playable`.

---

# 21. Criar MediaItems navegáveis

Podcast:

```kotlin
fun podcastToMediaItem(
    podcast: PodcastFeed
): MediaItem {

    return MediaItem.Builder()
        .setMediaId(
            "podcast:${podcast.id}"
        )
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(
                    podcast.title
                )
                .setArtist(
                    podcast.author
                )
                .setIsBrowsable(true)
                .setIsPlayable(false)
                .setArtworkUri(
                    podcast.image?.let {
                        Uri.parse(it)
                    }
                )
                .build()
        )
        .build()
}
```

---

# 22. Criar MediaItems reproduzíveis

Episódio:

```kotlin
fun episodeToMediaItem(
    episode: PodcastEpisode
): MediaItem {

    return MediaItem.Builder()
        .setMediaId(
            "episode:${episode.id}"
        )
        .setUri(
            episode.enclosureUrl
        )
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(
                    episode.title
                )
                .setArtist(
                    episode.feedTitle
                )
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setArtworkUri(
                    episode.image?.let {
                        Uri.parse(it)
                    }
                )
                .build()
        )
        .build()
}
```

---

# 23. Callback da MediaLibrary

O Android Auto chama callbacks para navegar na biblioteca.

Os principais são:

```text
onGetLibraryRoot()
onGetChildren()
onSearch()
onGetSearchResult()
```

Estrutura base:

```kotlin
class PodcastLibraryCallback :
    MediaLibrarySession.Callback {

    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser:
            MediaSession.ControllerInfo,
        params:
            LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {

        // devolver ROOT
    }

    override fun onGetChildren(
        session: MediaLibrarySession,
        browser:
            MediaSession.ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params:
            LibraryParams?
    ): ListenableFuture<
        LibraryResult<
            ImmutableList<MediaItem>
        >
    > {

        // devolver filhos
    }
}
```

A assinatura exata pode variar ligeiramente entre versões de Media3.

Confirma os imports e assinaturas da versão Media3 que estiveres a utilizar.

---

# 24. Estrutura recomendada para IDs

Usa IDs que permitam distinguir facilmente cada tipo:

```text
root

podcasts
podcast:1234
podcast:5678

episode:99111
episode:99112

radios
radio:radio_comercial
radio:m80
```

Depois:

```kotlin
when {

    parentId == "root" -> {
    }

    parentId == "podcasts" -> {
    }

    parentId.startsWith(
        "podcast:"
    ) -> {

        val feedId =
            parentId
                .substringAfter(
                    "podcast:"
                )
                .toLong()
    }
}
```

---

# 25. AndroidManifest.xml

Declara o serviço:

```xml
<service
    android:name=".playback.PlaybackService"
    android:foregroundServiceType="mediaPlayback"
    android:exported="true">

    <intent-filter>

        <action
            android:name="androidx.media3.session.MediaLibraryService" />

        <action
            android:name="android.media.browse.MediaBrowserService" />

    </intent-filter>

</service>
```

Permissões:

```xml
<uses-permission
    android:name="android.permission.INTERNET" />

<uses-permission
    android:name="android.permission.FOREGROUND_SERVICE" />

<uses-permission
    android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
```

O registo das duas actions ajuda a suportar clientes modernos Media3 e clientes que usam a interface de media browser da plataforma.

---

# 26. Reprodução em background

O `PlaybackService` continua responsável pelo player mesmo quando:

- a Activity é fechada
- o ecrã é desligado
- o utilizador muda de app
- Android Auto controla a reprodução

Não coloques a instância principal do ExoPlayer diretamente na Activity se queres reprodução consistente em background e Android Auto.

A Activity deve funcionar como um cliente do `MediaLibraryService`.

---

# 27. Fluxo no Android normal

Exemplo:

```text
MainActivity
     |
     +-- Pesquisar Podcast
     |
     v
PodcastRepository
     |
     v
Podcast Index
     |
     v
Lista de Podcasts
     |
     v
Lista de Episódios
     |
     v
MediaController
     |
     v
PlaybackService
     |
     v
ExoPlayer
```

---

# 28. Fluxo Android Auto

```text
Android Auto
     |
     v
MediaLibraryService
     |
     +-- onGetLibraryRoot()
     |
     +-- onGetChildren()
     |
     +-- onSearch()
     |
     v
MediaLibrarySession
     |
     v
ExoPlayer
```

O Android Auto cria a interface.

A tua aplicação fornece:

- árvore de conteúdos
- títulos
- imagens
- metadata
- URLs de reprodução
- comandos de reprodução

---

# 29. Pesquisa no Android Auto

O `MediaLibrarySession.Callback` permite implementar pesquisa.

A ideia pode ser:

```text
Android Auto
    |
    | "Metallica"
    v
onSearch()
    |
    v
PodcastRepository.search("Metallica")
    |
    v
Podcast Index
    |
    v
resultados
    |
    v
Android Auto
```

Deves manter a pesquisa simples e rápida.

Também podes armazenar resultados localmente para não consultar constantemente a API.

---

# 30. Cache recomendado

É boa prática não chamar a API em cada navegação do Android Auto.

Podes usar:

- Room
- SQLite
- memória
- DataStore para preferências simples

Exemplo:

```text
Podcast Index
      |
      v
PodcastRepository
      |
      +----> Room
      |
      v
UI / Android Auto
```

Podes guardar:

- podcasts favoritos
- últimos podcasts consultados
- episódios recentes
- progresso de reprodução
- último episódio reproduzido

---

# 31. Guardar favoritos

Exemplo de entidade Room:

```kotlin
@Entity(
    tableName = "favorite_podcasts"
)
data class FavoritePodcastEntity(

    @PrimaryKey
    val feedId: Long,

    val title: String,

    val author: String?,

    val image: String?
)
```

Desta forma o Android Auto pode mostrar imediatamente:

```text
Podcasts
   |
   +-- Favoritos
```

sem precisar de efetuar primeiro uma pesquisa remota.

---

# 32. Guardar progresso dos episódios

Podes guardar:

```text
episodeId
positionMs
durationMs
lastPlayed
```

Quando o utilizador voltar:

```kotlin
player.seekTo(
    savedPositionMs
)
```

Isto é particularmente útil para podcasts longos.

---

# 33. Tratamento de URLs sem imagem

Nem todos os podcasts têm imagens corretas.

Podes usar:

```kotlin
val artwork =
    episode.image
        ?: podcast.image
```

e, se ainda for `null`, apresentar um ícone local da aplicação.

---

# 34. Tratamento de áudio

`enclosureUrl` pode apontar para:

```text
audio/mpeg
audio/mp4
audio/aac
audio/ogg
```

O Media3/ExoPlayer suporta os formatos mais comuns, mas a disponibilidade final depende também dos codecs suportados pelo dispositivo.

---

# 35. Timeouts

É conveniente configurar OkHttp:

```kotlin
val okHttpClient =
    OkHttpClient.Builder()
        .connectTimeout(
            15,
            TimeUnit.SECONDS
        )
        .readTimeout(
            30,
            TimeUnit.SECONDS
        )
        .addInterceptor(
            authInterceptor
        )
        .build()
```

---

# 36. User-Agent

Não uses um User-Agent genérico como:

```text
Android
```

Usa algo identificável:

```text
VCCMusic/1.0
```

ou:

```text
VCCMusic/1.0 Android
```

Exemplo:

```kotlin
.header(
    "User-Agent",
    "VCCMusic/1.0 Android"
)
```

---

# 37. Erros da API

Trata pelo menos:

```text
401 - credenciais/autenticação
403 - acesso recusado
429 - limite de pedidos
500 - erro do serviço
timeout
sem Internet
```

Com Retrofit:

```kotlin
try {

    val response =
        repository.search(query)

} catch (
    e: HttpException
) {

    Log.e(
        "API",
        "HTTP ${e.code()}"
    )

} catch (
    e: IOException
) {

    Log.e(
        "API",
        "Erro de rede"
    )
}
```

---

# 38. Sincronização do relógio

A autenticação depende do timestamp.

Se o relógio do dispositivo estiver muito incorreto, a autenticação pode falhar.

Usa sempre:

```kotlin
System.currentTimeMillis() / 1000L
```

e evita guardar/reutilizar um hash antigo.

O hash deve ser criado para cada pedido.

---

# 39. Estrutura de projeto recomendada

```text
app/
 |
 +-- data/
 |    |
 |    +-- model/
 |    |     PodcastFeed.kt
 |    |     PodcastEpisode.kt
 |    |
 |    +-- network/
 |    |     PodcastIndexApi.kt
 |    |     PodcastIndexClient.kt
 |    |     PodcastIndexAuth.kt
 |    |     PodcastIndexAuthInterceptor.kt
 |    |
 |    +-- repository/
 |          PodcastRepository.kt
 |
 +-- playback/
 |      PlaybackService.kt
 |      PodcastLibraryCallback.kt
 |      MediaItemMapper.kt
 |
 +-- ui/
 |    |
 |    +-- podcasts/
 |    |     PodcastSearchScreen.kt
 |    |     PodcastDetailScreen.kt
 |    |
 |    +-- player/
 |          PlayerScreen.kt
 |
 +-- database/
 |      AppDatabase.kt
 |      FavoritePodcastEntity.kt
 |      EpisodeProgressEntity.kt
 |
 +-- MainActivity.kt
```

---

# 40. Arquitetura recomendada para a VCCMusic

Se a aplicação já contém música local e rádios online, podes usar uma única biblioteca Media3:

```text
ROOT
 |
 +-- Música
 |      |
 |      +-- Álbuns
 |      +-- Artistas
 |      +-- Pastas
 |
 +-- Rádios
 |      |
 |      +-- Favoritas
 |      +-- Portugal
 |      +-- Rock
 |
 +-- Podcasts
        |
        +-- Favoritos
        +-- Recentes
        +-- Pesquisa
```

Para o Android Auto, cada secção torna-se navegável.

---

# 41. Podcasts favoritos no Android Auto

Uma abordagem especialmente boa para utilização no automóvel é mostrar prioritariamente:

```text
Podcasts
 |
 +-- Favoritos
 |
 +-- Continuar a ouvir
 |
 +-- Episódios recentes
```

Isto reduz a necessidade de pesquisas e torna a utilização mais segura.

---

# 42. Sequência de implementação recomendada

Implementa por esta ordem:

1. configurar credenciais
2. criar autenticação SHA-1
3. criar interceptor
4. criar Retrofit
5. testar `search/byterm`
6. testar `episodes/byfeedid`
7. reproduzir um `enclosureUrl` com ExoPlayer
8. criar `PlaybackService`
9. criar `MediaLibrarySession`
10. criar árvore ROOT
11. adicionar podcasts
12. adicionar episódios
13. testar background playback
14. testar Android Auto
15. adicionar favoritos
16. adicionar histórico/progresso
17. adicionar pesquisa

---

# 43. Primeiro teste recomendado

Pesquisa:

```text
Metallica
```

Endpoint equivalente:

```text
GET /api/1.0/search/byterm?q=Metallica
```

Seleciona um `feedId`.

Depois:

```text
GET /api/1.0/episodes/byfeedid?id=<feedId>
```

Escolhe um episódio cujo:

```text
enclosureUrl != null
```

e testa:

```kotlin
player.setMediaItem(
    MediaItem.fromUri(
        episode.enclosureUrl!!
    )
)

player.prepare()

player.play()
```

Se isto funcionar, a integração base da Podcast Index está operacional.

---

# 44. Testar Android Auto

Podes testar através do ambiente de desenvolvimento para Android Auto / Desktop Head Unit disponibilizado pelo Android SDK, ou num veículo/unidade compatível.

O ponto essencial é validar que o Android Auto consegue:

```text
ligar ao MediaLibraryService
       |
       v
obter ROOT
       |
       v
obter Podcasts
       |
       v
obter um podcast
       |
       v
obter episódios
       |
       v
reproduzir episódio
```

---

# 45. Importante sobre Android Auto

Não cries uma UI Android Auto independente para uma app de áudio convencional.

O modelo normal é:

```text
A tua app fornece os conteúdos
           +
MediaLibraryService
           |
           v
Android Auto cria a interface
```

Isso permite que o sistema aplique as restrições de segurança e apresentação adequadas ao contexto automóvel.

---

# 46. Melhorias posteriores

Depois da primeira versão podes adicionar:

- favoritos
- downloads offline
- pesquisa por voz
- categorias
- episódios novos
- continuar a ouvir
- histórico
- velocidade de reprodução
- saltar 15/30 segundos
- temporizador
- fila de reprodução
- notificações de novos episódios
- sincronização entre dispositivos

---

# 47. Backend opcional

Para uma versão pública, considera:

```text
VCCMusic Android
       |
       | HTTPS
       v
VCCMusic Backend
       |
       | API Key + Secret
       v
Podcast Index
```

O backend pode ainda implementar:

- cache
- rate limiting
- métricas
- favoritos sincronizados
- pesquisas recentes
- proteção da API Secret

---

# 48. Resumo

Para integrar Podcast Index numa aplicação Android + Android Auto:

```text
Podcast Index
      |
      | Retrofit + OkHttp
      |
      | SHA1 authentication
      v
PodcastRepository
      |
      +-----------------------+
      |                       |
      v                       v
Android UI             MediaLibraryService
                              |
                              v
                      MediaLibrarySession
                              |
                              v
                          ExoPlayer
                              |
                              v
                         Android Auto
```

Tecnologias principais:

```text
Kotlin
Retrofit
OkHttp
Podcast Index API
Jetpack Media3
ExoPlayer
MediaLibraryService
MediaLibrarySession
Room (opcional)
```

---

# 49. Referências oficiais

Podcast Index:

- https://api.podcastindex.org/
- https://github.com/Podcastindex-org/docs-api
- https://github.com/Podcastindex-org/example-code

Android / Media3:

- https://developer.android.com/media/media3/session/serve-content
- https://developer.android.com/media/media3/session/background-playback
- https://developer.android.com/media/implement/surfaces/cars
- https://developer.android.com/media/media3/session/control-playback

---

# 50. Próximo passo sugerido

Depois de configurares a API Key e Secret, começa por implementar e testar apenas:

```text
PodcastIndexAuthInterceptor
        ↓
PodcastIndexApi
        ↓
searchPodcasts()
        ↓
getEpisodesByFeedId()
        ↓
ExoPlayer
```

Só depois adiciona `MediaLibraryService` e Android Auto.

Isto facilita bastante a identificação de erros e separa os problemas de API dos problemas de reprodução ou Android Auto.
