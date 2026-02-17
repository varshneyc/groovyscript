/**
 * Reusable Azure Container App deployment script.
 * Can be loaded from Jenkinsfile: load 'scripts/azureContainerAppDeploy.groovy'
 * Then call: azureContainerAppDeploy(config)
 *
 * Or use from Shared Library as a step.
 */

def call(Map config) {
    if (!config.resourceGroup || !config.containerAppName || !config.image) {
        error 'azureContainerAppDeploy: resourceGroup, containerAppName, and image are required.'
    }

    def resourceGroup = config.resourceGroup
    def containerAppName = config.containerAppName
    def image = config.image
    def minReplicas = config.minReplicas ?: '1'
    def maxReplicas = config.maxReplicas ?: minReplicas
    def cpu = config.cpu ?: '0.5'
    def memory = config.memory ?: '1Gi'
    def envVars = config.envVars ?: []  // List of "KEY=value" or Map
    def subscriptionId = config.subscriptionId

    def envArg = ''
    if (envVars instanceof List && !envVars.isEmpty()) {
        def pairs = envVars.findAll { it?.trim() }
        envArg = pairs ? "--set-env-vars \"${pairs.join(' ')}\"" : ''
    } else if (envVars instanceof Map && !envVars.isEmpty()) {
        def pairs = envVars.collect { k, v -> "${k}=${v}" }
        envArg = "--set-env-vars \"${pairs.join(' ')}\""
    }

    def cmd = """
        az containerapp update \
            --name ${containerAppName} \
            --resource-group ${resourceGroup} \
            --image ${image} \
            --min-replicas ${minReplicas} \
            --max-replicas ${maxReplicas} \
            --cpu ${cpu} \
            --memory ${memory} \
            ${envArg}
    """.stripIndent().trim()

    if (subscriptionId?.trim()) {
        sh "az account set --subscription ${subscriptionId}"
    }

    sh cmd
}
