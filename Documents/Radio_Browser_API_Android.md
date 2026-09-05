# Radio Browser API para uma App Android

## Visão geral

Para uma aplicação Android destinada a reproduzir rádios online, uma
opção muito interessante é a **Radio Browser API**.

A **Radio Browser API** é uma API pública e open-source que
disponibiliza informação sobre estações de rádio online de vários
países.

## Principais vantagens

-   **Pública**
-   **Gratuita**
-   **Open-source**
-   **Não necessita de API key**
-   Pode ser utilizada em aplicações gratuitas ou comerciais
-   Disponibiliza milhares de estações de rádio
-   Permite pesquisar rádios por país, nome, idioma, tags e outros
    critérios
-   Fornece o URL do stream que pode ser reproduzido numa aplicação
    Android

## Utilização numa aplicação Android

A API pode ser utilizada para obter dinamicamente a lista de rádios,
evitando manter manualmente todos os URLs dos streams dentro da
aplicação.

Uma arquitetura possível é:

``` text
Radio Browser API
       |
       v
Retrofit / HTTP Client
       |
       v
Repository
       |
       v
ViewModel
       |
       v
Jetpack Compose
       |
       v
Lista de rádios
       |
       v
Media3 / ExoPlayer
       |
       v
Stream da rádio
```

## Exemplo: rádios portuguesas

Para obter estações de rádio de Portugal:

``` http
GET https://de1.api.radio-browser.info/json/stations/bycountrycodeexact/PT
```

Também é possível pesquisar apenas estações consideradas operacionais:

``` http
GET https://de1.api.radio-browser.info/json/stations/search?countrycode=PT&hidebroken=true
```

## Exemplo de pesquisa por nome

Por exemplo, para procurar estações cujo nome contenha `comercial`:

``` http
GET https://de1.api.radio-browser.info/json/stations/byname/comercial
```

## Informação disponibilizada

Uma estação pode conter informação semelhante a:

``` json
{
  "stationuuid": "...",
  "name": "Radio Name",
  "url": "...",
  "url_resolved": "https://servidor.exemplo/stream.mp3",
  "homepage": "https://...",
  "favicon": "https://.../logo.png",
  "tags": "pop,music,hits",
  "country": "Portugal",
  "countrycode": "PT",
  "language": "portuguese",
  "codec": "MP3",
  "bitrate": 128,
  "hls": 0,
  "lastcheckok": 1
}
```

## Campos mais úteis para a aplicação

  Campo            Utilização
  ---------------- ----------------------------------------------------
  `stationuuid`    Identificador da estação; útil para favoritos
  `name`           Nome da rádio
  `url_resolved`   URL resolvido do stream para reprodução
  `favicon`        Logótipo/imagem da estação
  `homepage`       Site oficial da rádio
  `tags`           Géneros ou categorias
  `country`        País
  `countrycode`    Código ISO do país, por exemplo `PT`
  `language`       Idioma
  `codec`          Codec do stream, como MP3 ou AAC
  `bitrate`        Bitrate do stream
  `hls`            Indica se o stream utiliza HLS
  `lastcheckok`    Indica o resultado da última verificação do stream

## Reprodução no Android

O campo particularmente importante para o player é:

``` text
url_resolved
```

Este endereço pode ser fornecido ao **AndroidX Media3 / ExoPlayer**:

``` kotlin
val mediaItem = MediaItem.fromUri(station.urlResolved)

player.setMediaItem(mediaItem)
player.prepare()
player.play()
```

## Tecnologias sugeridas

Para uma aplicação Android moderna:

-   **Kotlin**
-   **Jetpack Compose** para a interface
-   **Retrofit** ou outro cliente HTTP para consumir a API
-   **ViewModel**
-   **Repository Pattern**
-   **AndroidX Media3 / ExoPlayer** para reprodução
-   **MediaSessionService** para reprodução em background
-   **Room/DataStore** para favoritos e preferências locais

## Nota sobre os servidores da API

Não é recomendável depender permanentemente de um único servidor, como:

``` text
de1.api.radio-browser.info
```

A Radio Browser funciona através de vários servidores. Para uma
implementação mais robusta, a aplicação deve considerar o mecanismo de
descoberta de servidores recomendado pelo projeto.

Também é recomendável enviar um `User-Agent` que identifique a
aplicação.

## Referências

-   Radio Browser API: https://api.radio-browser.info/
-   Documentação: https://docs.radio-browser.info/
-   Projeto open-source: https://github.com/segler-alex/radiobrowser-api

## Conclusão

A **Radio Browser API** é uma boa solução para uma aplicação Android de
rádios online porque permite descobrir estações e obter os respetivos
streams sem ser necessário manter manualmente uma lista extensa de URLs.

Uma implementação típica pode usar a Radio Browser API para obter as
estações e **Media3 / ExoPlayer** para reproduzir o stream selecionado
pelo utilizador.
