# OpenAPITest

Aplicativo Android de teste para fazer perguntas à OpenAI API em uma interface simples de chat. O objetivo é experimentar respostas sobre um tema específico usando um acervo fixo de documentos como fonte de consulta.

## Como funciona

- A tela de chat é feita com Jetpack Compose.
- O app envia cada pergunta diretamente à Responses API da OpenAI.
- Cada chamada envia o histórico do chat atual junto com a nova pergunta para manter o contexto.
- O histórico fica apenas em memória e é apagado ao tocar em “Novo chat”; store=false impede o armazenamento da resposta pela API.
- Como o histórico inteiro é reenviado a cada chamada, conversas mais longas usam mais tokens.
- Quando há um Vector Store configurado, a chamada habilita File Search para buscar trechos relevantes nos documentos enviados à plataforma OpenAI.

## Configuração local

No `local.properties`, na raiz do projeto, configure:

```properties
OPENAI_API_KEY=sua-chave-da-api
OPENAI_VECTOR_STORE_ID=vs_seu_vector_store_id
```

Opcionalmente, escolha outro modelo:

```properties
OPENAI_MODEL=id-do-modelo
```

Depois, sincronize o Gradle e execute o app pelo Android Studio. O projeto também aceita essas configurações como variáveis de ambiente do processo de build.

A API key e o ID do Vector Store são injetados no `BuildConfig` durante a compilação. Este projeto é voltado a testes locais; não distribua o APK com a chave configurada.

## Acervo RAG

Os documentos são preparados e adicionados previamente a um Vector Store na OpenAI Platform. O app não oferece seleção, upload ou gerenciamento de arquivos.

A pasta local `RAG/` pode ser usada para preparar os documentos do acervo e está excluída do Git. Para este fluxo, documentos Markdown ou texto podem ser enviados ao File Search; converta arquivos CSV para um formato textual compatível antes do upload. Após a indexação terminar, configure o ID `vs_...` do Vector Store em `local.properties` e reconstrua o app.

## Executar

1. Instale o Android SDK Platform 37 e abra o projeto no Android Studio.
2. Configure `OPENAI_API_KEY` e, para usar a RAG, `OPENAI_VECTOR_STORE_ID` em `local.properties`.
3. Sincronize o Gradle.
4. Execute a configuração `app` em um emulador ou dispositivo Android.

O uso da OpenAI API requer acesso e faturamento configurados na plataforma da API, independentemente do plano do ChatGPT.
