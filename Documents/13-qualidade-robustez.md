# WI-13 — Consolidar qualidade, desempenho e robustez

## Estado

Concluído para o âmbito atual do MVP. A validação automatizada foi executada com build debug, testes unitários, teste instrumentado de navegação no `FiatTipo` (Android Auto API 35) e lint sem erros; a matriz de hardware físico continua a ser uma atividade de pré-release do WI-14.

## Objetivo

Fechar riscos transversais antes de declarar o MVP pronto.

## Trabalho

- Rever concorrência, I/O na main thread, lifecycle e fugas de `Context`, cursor, player e controller.
- Medir arranque, scan, queries, scroll, memória e consumo de bateria com biblioteca pequena e grande.
- Garantir que logs não expõem URIs, nomes privados ou conteúdo desnecessário em release.
- Rever permissões mínimas, componentes exportados e validação de comandos/clientes da sessão.
- Completar acessibilidade: labels, foco, contraste, touch targets, TalkBack e font scale.
- Verificar localização; centralizar strings e preparar pelo menos o idioma decidido no WI-01.
- Exercitar recuperação após process death, pouco espaço, provider lento, base de dados migrada e permissão revogada.
- Criar matriz de dispositivos/APIs, incluindo API mínima, versão recente, telemóvel real e Android Auto DHU.
- Executar lint, testes unitários, instrumentados e Compose; corrigir flakes.

## Critérios de aceitação

- Zero crashes/ANRs conhecidos nos fluxos do MVP.
- Nenhum acesso de disco/rede pesado ocorre na main thread.
- Permissões e componentes do manifest são mínimos e justificados.
- Fluxos críticos são utilizáveis com TalkBack e fonte aumentada.
- A suíte automatizada é repetível e os testes manuais têm evidência registada.
- Limites de desempenho acordados são cumpridos ou os desvios estão documentados e aceites.

## Dependências

WI-06 a WI-12.
