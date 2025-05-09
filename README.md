## Documentação da Aplicação de Captura de Matrículas

### Visão Geral
Esta aplicação Android permite capturar e ler matrículas de automóveis através da câmara do telemóvel, armazenando-as numa base de dados local para posterior consulta. A aplicação reconhece matrículas portuguesas no formato XX-XX-XX, mostra as matrículas capturadas em tempo real e oferece opções para consulta do histórico.

### Requisitos
- Android 5.0 (API 21) ou superior
- Câmara traseira
- Permissões: Câmara, Localização, Armazenamento

### Funcionalidades Principais
1. **Captura de Matrículas**
   - Reconhecimento em tempo real de matrículas portuguesas (XX-XX-XX)
   - Visualização da matrícula detetada no ecrã
   - Guia visual para posicionamento da matrícula

2. **Armazenamento de Dados**
   - Armazenamento local em SQLite
   - Registo de data/hora da captura
   - Registo de localização GPS (quando disponível)
   - Possibilidade de adicionar notas

3. **Consulta de Histórico**
   - Visualização de todas as matrículas capturadas
   - Filtros por número de matrícula
   - Filtros por intervalo de datas
   - Visualização em lista com detalhes completos

4. **Interface do Utilizador**
   - Design moderno com Material Design
   - Visualização em tempo real das matrículas
   - Interface intuitiva e responsiva
   - Suporte a modo offline

### Arquitetura da Aplicação
A aplicação segue a arquitetura MVVM (Model-View-ViewModel) e utiliza os seguintes componentes:

- **CameraX**: Para acesso à câmara e captura de imagens
- **ML Kit**: Para reconhecimento ótico de caracteres (OCR)
- **Room**: Para persistência de dados local
- **ViewModel e LiveData**: Para gestão de estado e comunicação entre componentes
- **Coroutines**: Para operações assíncronas

### Estrutura do Projeto
- **model**: Classes de dados e entidades
- **database**: Classes relacionadas à base de dados (DAO, AppDatabase)
- **repository**: Camada de repositório para acesso aos dados
- **viewmodel**: ViewModels para gestão de estado
- **ocr**: Classes para processamento de OCR e reconhecimento de matrículas
- **adapter**: Adaptadores para RecyclerViews
- **util**: Classes utilitárias

### Instalação
1. Transfira o APK para o seu dispositivo Android
2. Permita a instalação de aplicações de fontes desconhecidas nas configurações do dispositivo
3. Instale a aplicação e conceda as permissões solicitadas

### Utilização
1. Abra a aplicação e aponte a câmara para uma matrícula
2. A aplicação reconhecerá automaticamente a matrícula e a exibirá no ecrã
3. Clique em "Guardar Matrícula" para armazenar a matrícula detetada
4. Aceda ao histórico clicando no botão flutuante no canto inferior direito
5. Utilize os filtros para pesquisar matrículas específicas ou por intervalo de datas

### Limitações
- O reconhecimento de matrículas depende de boas condições de iluminação
- Matrículas muito danificadas ou sujas podem não ser reconhecidas corretamente
- A precisão da localização GPS depende do hardware do dispositivo

### Desenvolvimento Futuro
- Reconhecimento de matrículas de outros países
- Sincronização com serviço na nuvem
- Estatísticas e relatórios sobre as matrículas capturadas
- Integração com serviços externos para obter informações adicionais sobre os veículos
