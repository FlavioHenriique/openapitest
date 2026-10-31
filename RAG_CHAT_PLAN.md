# Plano: chat Android com OpenAI API e RAG

## Objetivo

Transformar o projeto Android `OpenAPITest` em um app de teste com uma única tela de perguntas e respostas, visualmente parecida com um chat. As respostas devem consultar um acervo fixo de documentos (RAG) por meio da OpenAI API.

## Decisões já tomadas

- O projeto Android será o cliente HTTP; **não haverá backend próprio**.
- O app chamará a OpenAI diretamente via HTTPS.
- A chave será fornecida localmente como variável de ambiente ou configuração local do Gradle e injetada no app durante o build. Não gravar a chave no código-fonte nem neste documento.
- O uso é de teste pessoal, não de distribuição comercial. A chave compilada dentro do APK poderá ser extraída; esse risco foi aceito pelo usuário.
- Não haverá seletor de arquivos nem tela para gerenciar documentos dentro do app.
- O corpus RAG será fixo e preparado previamente na OpenAI Platform.
- Cada envio à API conterá somente a pergunta atual. Não enviar perguntas/respostas anteriores como contexto.
- O CSV será convertido para um formato textual aceito (por exemplo, Markdown, TXT ou JSON) antes de ser adicionado ao acervo.
- A Activity de entrada deve abrir diretamente o chat.

## Arquitetura planejada

```
Compose UI -> ViewModel -> Repository/cliente HTTP -> OpenAI Responses API
                                                        |
                                                        +-> File Search
                                                            -> Vector Store fixo
```

A tela pode manter e mostrar o histórico em memória para a experiência de chat, mas cada chamada independente envia apenas a pergunta atual e referencia o mesmo vector store. Não haverá memória conversacional no modelo.

A chave da API não deve ser armazenada em `BuildConfig` no repositório ou hardcoded. Para uso local, o Gradle pode lê-la de `OPENAI_API_KEY` (ou configuração local não versionada) e gerar o valor necessário ao app no build. O ID não secreto do acervo será configurável localmente como `OPENAI_VECTOR_STORE_ID`.

## Fases de implementação

### Fase 1 — Tela de chat

- Substituir o conteúdo inicial atual da `MainActivity` (hoje, Greeting/“Hello Android”) por uma tela Compose de pergunta e resposta.
- Criar estado vazio, lista de mensagens, balões para usuário e assistente, campo de texto e botão de envio.
- Mostrar estado de carregamento, impedir envio vazio e apresentar erro com opção de tentar novamente.
- Fazer o conteúdo rolar para a mensagem mais recente.
- Manter o histórico somente durante a sessão; não persistir nem enviar o histórico ao modelo.

### Fase 2 — Comunicação com a OpenAI

- Implementar uma camada de rede isolada do Compose, chamada pelo ViewModel.
- Enviar requisições HTTPS para a Responses API usando a chave configurada localmente.
- Incluir no request a pergunta atual e a ferramenta File Search vinculada ao `OPENAI_VECTOR_STORE_ID`.
- Extrair e mostrar o texto da resposta.
- Tratar falhas HTTP, ausência de configuração da chave/ID e problemas de rede.
- MVP pode usar resposta não transmitida em streaming; streaming pode ser adicionado se necessário.

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
- Confirmar que chamadas consecutivas não enviam o histórico anterior.
- Se disponível na resposta, mostrar a origem/citação do arquivo consultado.

## Critérios de conclusão

- A Activity launcher abre diretamente a tela de chat.
- É possível enviar uma pergunta e ver a resposta da OpenAI.
- File Search consulta sempre o Vector Store fixo.
- O app não oferece fluxo de seleção ou upload de arquivos.
- Cada requisição inclui somente a pergunta atual, sem histórico.
- A configuração ausente e falhas de rede/API são apresentadas de forma compreensível.
- A chave real não aparece em arquivos versionados do projeto.

## Pendências para decidir durante a implementação

- Modelo OpenAI a usar; manter configurável para troca simples.
- Se a resposta deve ser mostrada com streaming (recomendação inicial: não, para simplificar o MVP).
- Formato visual das citações/fontes (recomendação: nome do arquivo abaixo da resposta quando disponível).
- Se o histórico em memória deve ser limpo ao fechar/reabrir o app (recomendação: sim).

## Contexto do projeto na última inspeção

- Projeto: `OpenAPITest`, módulo `:app`.
- UI existente é Jetpack Compose.
- Entry point: `app/src/main/java/io/github/openapitest/MainActivity.kt`.
- Configuração observada: `compileSdk = 37`, AGP `9.1.1`, Gradle Wrapper `9.3.1`.
- Configurar a chave real localmente antes de testar chamadas autenticadas.
