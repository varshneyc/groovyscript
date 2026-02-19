/**
 * Azure Container App deployment — Makefile build, two-repo checkout (main + settings for .env).
 * Pipeline script from SCM (GitHub). Use ENVIRONMENT parameter to select dev/staging/prod config.
 */

pipeline {
    agent any

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'prod'],
            description: 'Target deployment environment'
        )
        string(
            name: 'IMAGE_TAG',
            defaultValue: '${BUILD_NUMBER}',
            description: 'Image tag (use ${BUILD_NUMBER} or a specific version)'
        )
        string(
            name: 'MAIN_BRANCH_OVERRIDE',
            defaultValue: '',
            description: 'Override main repo branch (empty = use env default)'
        )
        string(
            name: 'ENV_BRANCH_OVERRIDE',
            defaultValue: '',
            description: 'Override settings repo branch for .env (empty = use env default)'
        )
    }

    environment {
        MAIN_REPO = 'https://github.com/Uniqreate/tap_python_core.git'
        SETTINGS_REPO = 'https://github.com/Uniqreate/neo_settings.git'
    }

    stages {

        stage('Set environment config') {
            steps {
                script {
                    def envConfigs = [
                        dev: [
                            RESOURCE_GROUP  : 'taplend-dev-RG',
                            ACA_ENV_NAME    : 'TaplendDevFnAppEnv',
                            ACA_LOCATION    : 'Central India',
                            ACR_NAME        : 'devtaplendregistry',
                            CONTAINER_APP   : 'fntaplendpythoncoreapp',
                            IMAGE_NAME      : 'tap_python_core',
                            ENV_FILE        : '.env.devcore.azure',
                            MAIN_BRANCH     : 'tap_dev',
                            ENV_BRANCH      : 'tap_dev'
                        ],
                        staging: [
                            RESOURCE_GROUP  : 'taplend-staging-RG',
                            ACA_ENV_NAME    : 'TaplendStagingFnAppEnv',
                            ACA_LOCATION    : 'Central India',
                            ACR_NAME        : 'stagingtaplendregistry',
                            CONTAINER_APP   : 'fntaplendpythoncoreapp-staging',
                            IMAGE_NAME      : 'tap_python_core',
                            ENV_FILE        : '.env.stagingcore.azure',
                            MAIN_BRANCH     : 'tap_staging',
                            ENV_BRANCH      : 'tap_staging'
                        ],
                        prod: [
                            RESOURCE_GROUP  : 'taplend-prod-RG',
                            ACA_ENV_NAME    : 'TaplendProdFnAppEnv',
                            ACA_LOCATION    : 'Central India',
                            ACR_NAME        : 'prodtaplendregistry',
                            CONTAINER_APP   : 'fntaplendpythoncoreapp',
                            IMAGE_NAME      : 'tap_python_core',
                            ENV_FILE        : '.env.prodcore.azure',
                            MAIN_BRANCH     : 'tap_prod',
                            ENV_BRANCH      : 'tap_prod'
                        ]
                    ]
                    def config = envConfigs[params.ENVIRONMENT] ?: envConfigs.dev
                    config.each { key, value ->
                        env."${key}" = value
                    }
                    def tag = params.IMAGE_TAG?.trim() ?: "${BUILD_NUMBER}"
                    if (tag == '${BUILD_NUMBER}') tag = "${BUILD_NUMBER}"
                    env.VERSION = tag
                    env.FULL_IMAGE = "${env.ACR_NAME}.azurecr.io/${env.IMAGE_NAME}:${env.VERSION}"
                    if (params.MAIN_BRANCH_OVERRIDE?.trim()) env.MAIN_BRANCH = params.MAIN_BRANCH_OVERRIDE.trim()
                    if (params.ENV_BRANCH_OVERRIDE?.trim()) env.ENV_BRANCH = params.ENV_BRANCH_OVERRIDE.trim()
                    echo "Environment: ${params.ENVIRONMENT} | RG: ${env.RESOURCE_GROUP} | ACA: ${env.CONTAINER_APP} | Image: ${env.FULL_IMAGE}"
                }
            }
        }

        stage('Checkout Code + .env') {
            steps {
                withCredentials([string(credentialsId: 'github-pat', variable: 'TOKEN')]) {
                    sh '''
                        set -e
                        rm -rf * 
                        git clone --branch "${MAIN_BRANCH}" "https://${TOKEN}@${MAIN_REPO#https://}" tap_python_core
                        git clone --branch "${ENV_BRANCH}" "https://${TOKEN}@${SETTINGS_REPO#https://}" neo_settings
                        cd neo_settings 
                        ls -la
                        cd ..
                        cp "neo_settings/TAPLEND/${ENV_FILE}" .env
                        echo ".env loaded from neo_settings (${ENV_FILE})"
                    '''
                }
            }
        }

        stage('Print Git Commit Info') {
            steps {
                sh '''
                    echo "================ Git Info ================"
                    echo "Commit ID : $(git rev-parse --short HEAD)"
                    echo "Message   : $(git log -1 --pretty=%s)"
                    echo "Author    : $(git log -1 --pretty=%an)"
                    echo "Date      : $(git log -1 --pretty=%cd)"
                    echo "=========================================="
                '''
            }
        }

        stage('Build and Push to ACR using Makefile') {
            steps {
                withCredentials([azureServicePrincipal('azure-sp')]) {
                    sh '''
                        set -e
                        az login --service-principal -u "$AZURE_CLIENT_ID" -p "$AZURE_CLIENT_SECRET" --tenant "$AZURE_TENANT_ID"
                        az account set --subscription "$AZURE_SUBSCRIPTION_ID"
                        az acr login --name "$ACR_NAME"

                        export ACA_ENV_NAME="$ACA_ENV_NAME"
                        export ACA_LOCATION="$ACA_LOCATION"
                        export AZURE_RG="$RESOURCE_GROUP"
                        export IMAGE_NAME="$IMAGE_NAME"
                        export VERSION="$VERSION"

                        echo "DEBUG: ACA_ENV_NAME=$ACA_ENV_NAME"
                        echo "DEBUG: ACA_LOCATION=$ACA_LOCATION"
                        echo "DEBUG: AZURE_RG=$AZURE_RG"

                        make deploy-azure-aca VERSION="$VERSION"
                    '''
                }
            }
        }

        stage('Deploy to Azure Container App') {
            steps {
                withCredentials([azureServicePrincipal('azure-sp')]) {
                    sh '''
                        set -e
                        echo "Deploying container to Azure Container App..."
                        az login --service-principal -u "$AZURE_CLIENT_ID" -p "$AZURE_CLIENT_SECRET" --tenant "$AZURE_TENANT_ID" > /dev/null
                        az account set --subscription "$AZURE_SUBSCRIPTION_ID"

                        az containerapp update \
                            --name "${CONTAINER_APP}" \
                            --resource-group "${RESOURCE_GROUP}" \
                            --image "${FULL_IMAGE}-aca" \
                            --set-env-vars $(cat .env | grep -v '^#' | xargs)

                        echo "Container App successfully updated to image ${FULL_IMAGE}-aca"
                    '''
                }
            }
        }
    }

    post {
        success {
            echo "✅ SUCCESS — Deployed to Azure Container Apps (${params.ENVIRONMENT})"
        }
        failure {
            echo "❌ PIPELINE FAILED"
        }
        always {
            cleanWs()
        }
    }
}
