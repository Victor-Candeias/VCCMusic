# WI-11 — Atualizar e reconstruir o índice

**Estado:** Concluído em 4 de setembro de 2026

## Objetivo

Manter a biblioteca coerente quando ficheiros são adicionados, alterados ou removidos.

## Trabalho

- Disponibilizar atualização manual com progresso e prevenção de execuções concorrentes.
- Guardar data, resultado e métricas da última sincronização.
- Implementar trabalho WorkManager com constraints adequadas e política única (`unique work`).
- Decidir no WI-01 ou numa ADR se o trabalho periódico entra no MVP e qual o intervalo; não prometer deteção imediata de mudanças SAF.
- Reutilizar exatamente o scanner do WI-05 em foreground e worker.
- Implementar reconstrução explícita do índice sem eliminar playlists de forma acidental.
- Respeitar cancelamento, bateria, reinício de processo e perda de permissão.
- Atualizar ou invalidar filas ativas de forma segura quando a biblioteca muda.

## Critérios de aceitação

- Uma nova faixa aparece após sincronização e uma removida desaparece após scan completo.
- Apenas uma sincronização corre para a mesma raiz.
- Falha parcial não elimina entradas que não foi possível verificar.
- WorkManager termina com sucesso/retry/failure coerente e diagnóstico visível.
- Reconstrução mantém playlists segundo a política definida e não duplica dados.

## Implementação atual

- A Biblioteca disponibiliza a ação explícita **Reindexar**.
- A `MainActivity` agenda uma reindexação automática ao abrir a aplicação quando existe uma raiz SAF ativa.
- Reindexações concorrentes são evitadas; uma nova raiz cancela a execução anterior antes de iniciar a sua sincronização.
- A permissão e o provider SAF são validados antes da reindexação, e o resultado é comunicado na ação manual.
- `ReindexWorker` reutiliza o scanner, exige bateria não baixa, usa trabalho único `KEEP`, e devolve sucesso, retry ou falha de permissão de forma explícita.

**Conclusão:** WI-11 finalizado.

## Testes

- Testes do Worker com scanner fake: sucesso, retry, permissão perdida e cancelamento.
- Alteração da árvore entre duas sincronizações.
- Aplicação terminada pelo sistema durante o trabalho.

## Dependências

WI-05.
