# WI-04 — Selecionar e conservar a raiz com SAF

## Objetivo

Permitir ao utilizador escolher uma pasta acessível e reutilizar a autorização depois de reiniciar a aplicação.

## Trabalho

- Lançar `ACTION_OPEN_DOCUMENT_TREE` através da API de Activity Results.
- Pedir e conservar permissões de leitura com `takePersistableUriPermission`.
- Validar a URI devolvida e apresentar limitações conhecidas do Android 11+ sem pedir permissões de armazenamento excessivas.
- Guardar a URI ativa e permitir trocar de raiz mediante confirmação do impacto no índice.
- Detetar autorização inexistente, revogada ou provider indisponível e conduzir o utilizador a escolher novamente.
- Criar uma abstração testável sobre `ContentResolver`/`DocumentsContract` para listar documentos.
- Não solicitar `MANAGE_EXTERNAL_STORAGE` nem depender de caminhos reais.

## Critérios de aceitação

- A raiz continua acessível após fechar/reabrir a aplicação e reiniciar o dispositivo.
- Cancelar o seletor mantém o estado anterior intacto.
- Trocar de raiz não mistura registos das duas árvores.
- Uma permissão revogada produz uma mensagem recuperável e não um crash.
- Funciona com armazenamento interno e com um provider SAF compatível; limitações do provider são tratadas.

## Testes

- Testes instrumentados do resultado aceite/cancelado.
- Teste manual de persistência após reinício.
- Teste manual de revogação da permissão nas definições do sistema.

## Dependências

WI-02.

