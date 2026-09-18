# WI-18 — Corrigir a desserialização e robustez do Podcast Index

## Estado

Concluído na implementação; falta validação no dispositivo com o feed que
originou o diagnóstico.

## Evidência

O log `logs\vccmusic-diagnostic-20260917210551.log.txt` regista `ClassCastException`
na pesquisa e no carregamento de episódios. A pesquisa já foi alterada para
processar o corpo JSON manualmente, mas o endpoint `episodes/byfeedid` ainda
usava desserialização automática para `List<PodcastEpisode>`, falhando com
respostas incompletas ou com tipos inesperados.

## Trabalho

- Processar manualmente as respostas de pesquisa e episódios.
- Validar a estrutura raiz e os arrays `feeds`/`items`.
- Ignorar feeds e episódios incompletos sem descartar os resultados válidos.
- Preservar campos opcionais ausentes ou nulos.
- Converter respostas HTTP não bem-sucedidas em erros explícitos.
- Cobrir os casos com testes unitários.

## Critérios de aceitação

- Uma resposta válida com itens incompletos não fecha a aplicação.
- O feed `5751673` pode ser consultado sem `ClassCastException`.
- Itens válidos continuam visíveis quando a mesma resposta contém itens inválidos.
- Respostas HTTP, JSON vazio, JSON inválido e arrays ausentes produzem erro
  recuperável na UI.
- A reprodução de música e rádio não é afetada.

## Verificação

- Testes unitários do parser para feeds incompletos, episódios com campos
  opcionais ausentes e itens inválidos.
- Build/testes Gradle do módulo `app`.
- Teste manual de pesquisa e abertura do feed `5751673` no dispositivo.

## Dependências

WI-17.
