# Plano de trabalho — VCCMusic

Este diretório transforma os requisitos do `README.md` num backlog ordenado para desenvolver a aplicação. Os itens estão organizados pela sua dependência técnica, e não apenas pela ordem em que as funcionalidades aparecem no relatório.

## Convenções

- Executar os work items pela numeração, salvo quando o campo **Dependências** indicar que podem avançar em paralelo.
- Um item só fica concluído quando todos os respetivos critérios de aceitação e verificações estiverem satisfeitos.
- Manter `content://` como identidade dos ficheiros; não converter URIs SAF em caminhos de filesystem.
- O `MediaLibraryService` é a autoridade única sobre o player e a fila ativa.
- Room é a fonte de dados da biblioteca indexada e das playlists; a árvore SAF é a fonte original dos ficheiros.

## Sequência

| Ordem | Work item | Resultado principal | Depende de |
|---:|---|---|---|
| 01 | [Decisões e âmbito do MVP](01-decisoes-e-ambito.md) | Decisões de produto e técnicas registadas | — |
| 02 | [Projeto Android base](02-projeto-android-base.md) | Projeto compilável e estrutura modular | 01 |
| 03 | [Modelo de domínio e Room](03-modelo-de-dados-room.md) | Persistência e consultas da biblioteca | 02 |
| 04 | [Seleção da raiz com SAF](04-selecao-raiz-saf.md) | Pasta escolhida e permissão persistente | 02 |
| 05 | [Scanner e indexação](05-scanner-indexacao.md) | Árvore e faixas indexadas em Room | 03, 04 |
| 06 | [Navegação da biblioteca](06-interface-biblioteca.md) | Pastas e todas as músicas no telefone | 05 |
| 07 | [Serviço de reprodução](07-servico-reproducao.md) | Áudio em background e sessão multimédia | 05 |
| 08 | [Filas e reprodução aleatória](08-filas-e-shuffle.md) | Tocar tudo/pasta e shuffle não destrutivo | 06, 07 |
| 09 | [Playlists](09-playlists.md) | CRUD e reprodução de playlists | 03, 07 |
| 10 | [Em reprodução e controlos](10-em-reproducao-controlos.md) | UI do player, notificação e Bluetooth | 07, 08 |
| 11 | [Atualização do índice](11-atualizacao-indice.md) | Reindexação manual e em background | 05 |
| 12 | [Android Auto](12-android-auto.md) | Catálogo navegável e reproduzível no carro | 08, 09, 10 |
| 13 | [Qualidade e robustez](13-qualidade-robustez.md) | Cobertura, acessibilidade e resiliência | 06–12 |
| 14 | [Entrega do MVP](14-entrega-mvp.md) | Build de release validado | 13 |
| 15 | [Menus e navegação](15-menus-e-navegacao.md) | Fluxo de ecrãs e menus conforme o layout | 03, 04, 05–10 |

## Caminho crítico

`01 → 02 → (03 + 04) → 05 → (06 + 07) → 08 → 10 → 12 → 13 → 14`

Os itens 03 e 04 podem ser feitos em paralelo. Depois do item 07, os itens 09 e 11 também podem avançar em paralelo com parte do trabalho de UI.

O WI-15 é um item transversal de UI e pode ser executado por fases depois de existirem dados, seleção de raiz e reprodução. As rádios online estão disponíveis com estações públicas configuradas e ativação individual em Configurações.

## Definição global de concluído

- O projeto compila com o Gradle Wrapper e não contém erros de lint introduzidos pela alteração.
- Existem testes automáticos para lógica nova que não dependa diretamente da framework Android.
- O comportamento foi verificado num dispositivo físico para SAF, background, Bluetooth e Android Auto.
- Estados de carregamento, vazio e erro são apresentados sem bloquear ou fechar a aplicação.
- Alterações ao esquema Room incluem migração ou uma decisão explícita de desenvolvimento para recriar a base de dados.
- Código, nomes e documentação não assumem um caminho local específico do computador.
