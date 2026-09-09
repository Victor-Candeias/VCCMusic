# Bug — Android Auto mostra que a VCCMusic não está a funcionar

## Descrição

Ao ligar o telemóvel ao veículo através do Android Auto, a VCCMusic aparece no
launcher do carro, mas ao abrir deixa de responder e o Android Auto apresenta:

> VCCMusic parece não estar a funcionar neste momento.

No telemóvel a aplicação continua a funcionar normalmente, inclusive com o
telemóvel ligado ao carro. Evidência fornecida: fotografia do ecrã da unidade
do veículo (`copilot-image-23beb3.png`).

## Passos para reproduzir

1. Instalar a versão release da VCCMusic num telemóvel Android.
2. Abrir a aplicação no telemóvel e confirmar que a biblioteca funciona.
3. Ligar o telemóvel ao veículo com Android Auto.
4. Abrir VCCMusic no ecrã do veículo.
5. Tentar navegar na biblioteca ou iniciar uma faixa.

## Resultado atual

A unidade do veículo mostra a aplicação, mas apresenta o erro “VCCMusic parece
não estar a funcionar neste momento” e a interface fica sem resposta.

## Resultado esperado

O Android Auto deve conseguir iniciar o `MediaLibraryService`, consultar a raiz
da biblioteca, navegar por pastas, faixas e playlists e iniciar a reprodução
através da `MediaLibrarySession`, sem bloquear a aplicação no telefone.

## Validação técnica inicial

- `AndroidManifest.xml` declara `com.google.android.gms.car.application` e o
  serviço `MediaLibraryService`/`MediaBrowserService`.
- `automotive_app_desc.xml` declara a categoria `media`.
- `MusicPlaybackService` publica a árvore e responde de forma assíncrona aos
  callbacks de browsing.
- Não existe atualmente um teste instrumentado que valide o arranque do
  `MusicPlaybackService` e os callbacks através de um controlador Android Auto.
- O problema está isolado à integração com a unidade Android Auto, uma vez que
  a aplicação funciona no telefone.

## Hipóteses a investigar

1. Exceção ou timeout no arranque do serviço ou num callback de browsing quando
   o Android Auto cria o browser.
2. Estado da biblioteca sem raiz SAF ativa ou autorização inacessível no
   contexto do serviço.
3. Incompatibilidade entre a versão release instalada, a versão do Android
   Auto e a unidade do veículo.
4. Requisito adicional de compatibilidade, manifesto ou ciclo de vida do
   `MediaLibraryService` não coberto pelos testes atuais.

## Dados necessários para diagnóstico

- **Telemóvel:** Google Pixel 10a atualizado.
- **Aplicação:** versão atualmente disponível no branch.
- **Veículo:** Fiat Tipo, 2018/07.
- **Unidade:** sistema multimédia de 9".
- **Ligação:** Android Auto por cabo USB.
- **Biblioteca:** existe uma pasta de música autorizada e indexada antes da
  ligação.
- Versão exata do Android Auto.
- `adb logcat` reproduzindo o erro, filtrado por `pt.vcc.vccmusic`,
  `MediaLibraryService`, `Media3` e `AndroidAuto`.
- Resultado no Android Auto Desktop Head Unit (DHU), para separar problema da
  app de problema específico da unidade física.

## Critérios de aceitação

- A app abre no Android Auto sem a mensagem de erro.
- A raiz e as categorias da biblioteca são navegáveis com biblioteca
  preenchida e vazia.
- É possível iniciar, pausar e mudar de faixa a partir do veículo.
- Reconectar o Android Auto não cria uma segunda sessão nem deixa o serviço
  bloqueado.
- O caso é coberto por teste automatizado/instrumentado e por validação no DHU.
