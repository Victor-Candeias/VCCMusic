# WI-14 — Preparar e entregar o MVP

## Estado

Concluído para o MVP atual. A versão `1.0.0` tem build Release com shrinking de recursos/código ativo, bundle e APK gerados, sem credenciais no repositório. A assinatura de distribuição continua a ser fornecida externamente no pipeline de publicação.

## Objetivo

Produzir um artefacto de release rastreável, instalável e acompanhado pela documentação necessária.

## Trabalho

- Definir `versionCode`/`versionName`, changelog e identificação do commit.
- Configurar assinatura fora do repositório e garantir que credenciais nunca são committed.
- Ativar shrinking/obfuscation se aplicável e validar regras de Room, Media3 e serialização.
- Gerar Android App Bundle e, se útil para testes, APK assinado.
- Executar a matriz de regressão do WI-13 no build release, não apenas no debug.
- Documentar instalação, escolha da pasta, atualização da biblioteca, playlists, background e Android Auto.
- Preparar política de privacidade coerente com acesso local a ficheiros e eventual ficha da Play Store.
- Confirmar nome/disponibilidade de marca e requisitos atuais de publicação antes de submeter à loja.
- Criar tag/release notes apenas após aprovação dos testes.

## Critérios de aceitação

- O bundle de release compila, instala e executa nos dispositivos alvo.
- SAF, playback em background, Bluetooth e DHU foram validados no build assinado.
- Não existem segredos, caminhos locais, builds debug ou dados de teste no artefacto/repositório.
- Documentação do utilizador e limitações conhecidas estão atualizadas.
- O MVP satisfaz cada requisito incluído no WI-01 ou tem exceção explicitamente aceite.

## Dependências

WI-13.
