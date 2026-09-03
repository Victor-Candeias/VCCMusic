# ADR-001 — Decisões iniciais do produto

- **Estado:** Aceite
- **Data:** 3 de setembro de 2026
- **Work item:** WI-01

## Identidade

- Nome público: **VCCMusic**.
- Nome do projeto Android: **VCCMusic**.
- `applicationId`: `pt.vcc.vccmusic`.
- A documentação não depende de um caminho absoluto do computador de desenvolvimento.

## Compatibilidade Android

- `minSdk`: API 26.
- No WI-02, `compileSdk` e `targetSdk` serão fixados para a versão estável compatível com o Android Studio, Android Gradle Plugin e bibliotecas adotadas nesse momento.
- Antes de cada publicação, o `targetSdk` será revisto face aos requisitos da Google Play.
- São aceites os formatos que o Media3 consiga reproduzir através dos codecs disponíveis no dispositivo. O suporte pode variar por formato, versão Android e fabricante.

## Âmbito do MVP

### Incluído

- Uma raiz ativa escolhida através do Storage Access Framework.
- Leitura recursiva da raiz e das suas subpastas.
- Navegação por pastas e listagem de todas as músicas.
- Reprodução de toda a biblioteca e das músicas diretamente contidas na pasta aberta.
- Playlists personalizadas e reprodução normal ou aleatória.
- Reprodução em segundo plano e com o ecrã desligado.
- Controlos do sistema, Bluetooth e Android Auto.
- Reindexação da biblioteca.

### Excluído

- Pesquisa, favoritos, histórico e temporizador.
- Múltiplas raízes simultâneas.
- Sincronização cloud e telemetria.
- Edição de ficheiros ou dos seus metadados.

## Apresentação e ordenação

- Pastas: ordem alfabética, sem distinguir maiúsculas de minúsculas.
- Músicas: ordem alfabética pelo título apresentado. O título apresentado é o título dos metadados ou, se estiver vazio, o nome do ficheiro.
- Playlists: ordem alfabética, sem distinguir maiúsculas de minúsculas.
- Artista ausente: apresentar **Desconhecido**.
- Álbum ausente: apresentar **Desconhecido**.
- “Tocar pasta”: incluir apenas as músicas diretamente contidas na pasta aberta; não incluir músicas das subpastas.
- Empates de ordenação devem usar um segundo critério estável, como URI ou ID, para evitar mudanças visuais aleatórias.

## Identidade e ciclo de vida das faixas

- O URI persistente `content://` é a identidade externa da faixa.
- A aplicação nunca depende da conversão do URI para um caminho físico.
- Depois de uma sincronização completa e bem-sucedida, ficheiros removidos, movidos ou inacessíveis deixam de aparecer na biblioteca.
- Uma mudança de URI causada por mover um ficheiro é tratada como remoção da entrada anterior e descoberta de uma nova faixa.
- Faixas indisponíveis não aparecem nas playlists. A associação obsoleta pode ser eliminada durante a reconciliação do índice.
- Uma enumeração parcial ou falhada não pode apagar faixas apenas porque não foi possível confirmá-las.
- Se a autorização da raiz for revogada, a biblioteca deixa de ser apresentada como disponível e a aplicação pede ao utilizador que volte a escolher uma pasta; não tenta obter um caminho físico alternativo.

## Privacidade

- Índice, preferências e playlists permanecem apenas no dispositivo.
- O MVP não recolhe telemetria.
- A aplicação não pede acesso total ao armazenamento.
- Nomes de ficheiros, URIs e metadados não são enviados para serviços externos.
- Logs de produção não devem revelar URIs nem nomes de ficheiros.

## Consequências para os work items seguintes

- O WI-02 usa `VCCMusic`, `pt.vcc.vccmusic` e API 26.
- O WI-05 só remove registos ausentes depois de concluir uma enumeração válida.
- O WI-08 constrói a fila de uma pasta apenas com descendentes diretos.
- O WI-09 omite e reconcilia associações de faixas indisponíveis.
- O WI-10 define o layout detalhado de “Em reprodução”; esse desenho não altera as decisões deste ADR.
