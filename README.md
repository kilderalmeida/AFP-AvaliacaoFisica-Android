# AFP - Avaliação Física e Performance 🚀

AFP é um ecossistema mobile desenvolvido em **Jetpack Compose** e **Firebase** para otimizar o monitoramento de atletas por coaches e treinadores de alto rendimento.

## 📱 Funcionalidades Principais

### Para o Atleta
- **Check-in Pré-Treino:** Monitoramento de fadiga, sono, dor, estresse, humor e VFC.
- **Check-out Pós-Treino:** Registro de percepção subjetiva de esforço (PSE Foster) e duração.
- **Plano de Treino IA:** Acesso direto ao planejamento de 4 semanas gerado pelo Coach.

### Para o Coach / Treinador
- **Métricas Avançadas:** Dashboard com Carga Total, PSE Médio, Evolução da Carga e Distribuição de Modalidades.
- **Gerador de Plano IA:** Integração (Simulada/Gemini) para criar planos de 4 semanas personalizados em segundos.
- **Exportação PDF:** Relatórios profissionais prontos para compartilhamento em um clique.
- **Modo Demo:** Demonstração instantânea com dados fictícios para apresentações.

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** Kotlin
- **UI:** Jetpack Compose (Material 3)
- **Arquitetura:** MVVM (Model-View-ViewModel)
- **Backend:** Firebase (Auth, Firestore, Analytics)
- **PDF:** Android PdfDocument API
- **Navegação:** Compose Navigation
- **Injeção de Dependência:** Hilt (Estrutura base)

## 📦 Como Instalar

1. Clone o repositório.
2. Certifique-se de ter o arquivo `google-services.json` configurado na pasta `app/`.
3. Build e rode no Android Studio.

## 🚀 Roadmap v1.0.0
- [x] Autenticação Firebase
- [x] Fluxo Check-in/Check-out
- [x] Dashboard de Métricas
- [x] Exportação de Relatórios PDF
- [x] Geração de Planos Assistida por IA
- [x] Modo Demonstração
- [x] Preparação para Play Store

---
Desenvolvido por **AFP Team** | [Políticas de Privacidade](https://afp-performance.web.app/privacy)
