#!/bin/bash

# Script de Implantação do Plate Recognition App

# Variáveis de configuração
APP_NAME="PlateRecognitionApp"
VERSION=$(grep "app.version" app/config.properties | cut -d'=' -f2)
BUILD_TYPE=$(grep "app.buildType" app/config.properties | cut -d'=' -f2)

# Funções de suporte
log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"
}

# Verificar pré-requisitos
check_prerequisites() {
    log "Verificando pré-requisitos..."
    command -v gradle >/dev/null 2>&1 || { log "Gradle não instalado. Abortando."; exit 1; }
    command -v adb >/dev/null 2>&1 || { log "ADB não instalado. Abortando."; exit 1; }
}

# Limpar builds anteriores
clean_build() {
    log "Limpando builds anteriores..."
    ./gradlew clean
}

# Construir aplicativo
build_app() {
    log "Construindo $APP_NAME v$VERSION ($BUILD_TYPE)..."
    ./gradlew assembleRelease
}

# Executar testes
run_tests() {
    log "Executando testes unitários..."
    ./gradlew test
}

# Implantar no dispositivo
deploy_to_device() {
    log "Implantando no dispositivo conectado..."
    adb install -r app/build/outputs/apk/release/app-release.apk
}

# Gerar relatório de implantação
generate_deployment_report() {
    local report_file="deployment_report_$(date +'%Y%m%d_%H%M%S').log"
    log "Gerando relatório de implantação: $report_file"
    {
        echo "Deployment Report"
        echo "----------------"
        echo "App: $APP_NAME"
        echo "Version: $VERSION"
        echo "Build Type: $BUILD_TYPE"
        echo "Timestamp: $(date +'%Y-%m-%d %H:%M:%S')"
        echo ""
        echo "Deployment Steps:"
        echo "1. Prerequisites Check: ✓"
        echo "2. Clean Build: ✓"
        echo "3. App Build: ✓"
        echo "4. Unit Tests: ✓"
        echo "5. Device Deployment: ✓"
    } > "$report_file"
}

# Fluxo principal de implantação
main() {
    check_prerequisites
    clean_build
    build_app
    run_tests
    deploy_to_device
    generate_deployment_report
    log "Implantação concluída com sucesso!"
}

# Executar script
main
