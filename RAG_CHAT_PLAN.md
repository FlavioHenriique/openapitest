# Plano: chat Android com OpenAI API e RAG

## Objetivo

Transformar o projeto Android `OpenAPITest` em um app de teste com uma única tela de perguntas e respostas, visualmente parecida com um chat. As respostas devem consultar um acervo fixo de documentos (RAG) por meio da OpenAI API.

## Progresso atual

- Fase 1 implementada na `MainActivity` com Compose.
- Fase 2 implementada: a conversa usa a Responses API, mostra carregamento, respostas e erros com retry.
- Cada requisição envia o histórico de mensagens do chat atual junto com a nova pergunta. O histórico fica em memória e é apagado ao iniciar um novo chat; store=false continua desativando o armazenamento das respostas na API.
- Fase 3 concluída: o File Search usa o Vector Store fixo configurado por `OPENAI_VECTOR_STORE_ID`, com os arquivos do acervo indexados.
- A chave é lida de `OPENAI_API_KEY` no ambiente do Gradle ou de `local.properties`; o valor real não deve ser versionado.
- `app:assembleDebug` sincronizou e compilou com sucesso. A chamada real depende de uma chave válida configurada localmente.

## Decisões já tomadas

- O projeto Android será o cliente HTTP; **não haverá backend próprio**.
- O app chamará a OpenAI diretamente via HTTPS.
- A chave será fornecida localmente como variável de ambiente ou configuração local do Gradle e injetada no app durante o build. Não gravar a chave no código-fonte nem neste documento.
- O uso é de teste pessoal, não de distribuição comercial. A chave compilada dentro do APK poderá ser extraída; esse risco foi aceito pelo usuário.
- Não haverá seletor de arquivos nem tela para gerenciar documentos dentro do app.
- O corpus RAG será fixo e preparado previamente na OpenAI Platform.
- Cada envio à API conterá as mensagens anteriores do chat atual e a nova pergunta como contexto. O botão “Novo chat” apaga o histórico local e cancela a requisição ativa.
- O CSV será convertido para um formato textual aceito (por exemplo, Markdown, TXT ou JSON) antes de ser adicionado ao acervo.
- A Activity de entrada deve abrir diretamente o chat.

## Arquitetura planejada

```
Compose UI -> ViewModel -> Repository/cliente HTTP -> OpenAI Responses API
                                                        |
                                                        +-> File Search
                                                            -> Vector Store fixo
```

A tela mantém o histórico do chat em memória e o envia junto a cada chamada para dar contexto ao modelo. O mesmo Vector Store é consultado a cada envio. “Novo chat” limpa o histórico local; nada do histórico é salvo no armazenamento do app.

A chave da API não deve ser armazenada em `BuildConfig` no repositório ou hardcoded. Para uso local, o Gradle pode lê-la de `OPENAI_API_KEY` (ou configuração local não versionada) e gerar o valor necessário ao app no build. O ID não secreto do acervo será configurável localmente como `OPENAI_VECTOR_STORE_ID`.

## Fases de implementação

### Fase 1 — Tela de chat (implementada)

- A `MainActivity` abre diretamente a tela Compose de pergunta e resposta.
- Criados estado vazio, sugestões de pergunta, lista de mensagens, balões para usuário e assistente, campo de texto e botão de envio.
- O envio local mostra um indicador e uma resposta demonstrativa, sem chamar a OpenAI nesta fase.
- O envio vazio é desabilitado, a lista rola para a mensagem mais recente e há ação para iniciar um novo chat.
- Na Fase 1, o histórico foi mantido em memória para exibição; o envio como contexto foi implementado na Fase 2.
- O campo de texto ajusta-se ao teclado com suporte a edge-to-edge. Falhas reais da API aparecem com uma ação de retry.

### Fase 2 — Comunicação com a OpenAI (implementada)

- A tela observa estado do `ChatViewModel`; `OpenAiChatRepository` executa a chamada HTTPS fora da thread principal.
- A chamada usa a Responses API, envia as mensagens do chat atual como histórico e define store=false. O contexto é gerenciado manualmente pelo app, sem previous_response_id.
- O File Search é incluído automaticamente quando `OPENAI_VECTOR_STORE_ID` tem valor. Sem ID, o app testa apenas a resposta geral do modelo até a Fase 3 preparar o acervo.
- A resposta textual é exibida na conversa. A UI mostra carregamento e mensagens de erro com retry.
- Há mensagens orientadas para chave inválida/ausente, limite ou saldo, indisponibilidade do modelo, conexão e timeout.
- O modelo configurável por `OPENAI_MODEL` usa `gpt-6.1-sol` como padrão. A resposta não usa streaming.
- O manifesto declara permissão de Internet. O build `app:assembleDebug` concluiu com sucesso; a chamada remota precisa de uma chave válida para ser exercitada.

### Fase 3 — Acervo RAG fixo

Preparação manual, fora do app:

1. Preparar PDFs e arquivos de texto.
2. Converter CSVs em Markdown, TXT ou JSON, preservando cabeçalhos e valores da tabela.
3. Criar um Vector Store na OpenAI Platform e enviar os documentos preparados.
4. Esperar o processamento/indexação dos arquivos terminar.
5. Guardar o ID `vs_...` como `OPENAI_VECTOR_STORE_ID` local para o build.

O app não seleciona, envia, cria, remove ou atualiza arquivos. Mudanças futuras no corpus serão feitas diretamente no Vector Store.

### Fase 4 — Versão funcional para teste

- Sincronizar e compilar o projeto.
- Executar o app em emulador ou dispositivo conectado.
- Testar uma pergunta respondida pelo corpus, outra sem resposta nos documentos, estado de carregamento e erros de rede/configuração.
- Confirmar que chamadas consecutivas incluem o histórico do chat e que “Novo chat” o apaga.
- Se disponível na resposta, mostrar a origem/citação do arquivo consultado.

## Critérios de conclusão

- A Activity launcher abre diretamente a tela de chat.
- É possível enviar uma pergunta e ver a resposta da OpenAI.
- File Search consulta o Vector Store fixo quando `OPENAI_VECTOR_STORE_ID` está configurado.
- O app não oferece fluxo de seleção ou upload de arquivos.
- Cada requisição inclui o histórico do chat atual e a pergunta nova; “Novo chat” limpa o contexto.
- A configuração ausente e falhas de rede/API são apresentadas de forma compreensível.
- A chave real não aparece em arquivos versionados do projeto.

## Pendências para decidir durante a implementação

- O padrão atual é `gpt-6.1-sol`; pode ser trocado via `OPENAI_MODEL`.
- Streaming está desativado no MVP.
- Formato visual das citações/fontes (recomendação: nome do arquivo abaixo da resposta quando disponível).
- Se o histórico em memória deve ser limpo ao fechar/reabrir o app (recomendação: sim).

## Contexto do projeto na última inspeção

- Projeto: `OpenAPITest`, módulo `:app`.
- UI existente é Jetpack Compose.
- Entry point: `app/src/main/java/io/github/openapitest/MainActivity.kt`.
- Configuração observada: `compileSdk = 37`, AGP `9.1.1`, Gradle Wrapper `9.3.1`.
- Configurar a chave real localmente antes de testar chamadas autenticadas.
