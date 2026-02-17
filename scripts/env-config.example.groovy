/**
 * Example environment-specific config.
 * Use from Jenkinsfile to pass different values per ENVIRONMENT parameter.
 * Load and call: envConfig = load 'scripts/env-config.example.groovy'; envConfig.get(params.ENVIRONMENT)
 */

def get(String environment) {
    def configs = [
        dev: [
            resourceGroup      : 'rg-myapp-dev',
            containerAppName   : 'aca-myapp-dev',
            containerAppEnv    : 'myapp-env-dev',
            acrName            : 'myacrdev',
            imageName          : 'myapp',
            subscriptionId     : '00000000-0000-0000-0000-000000000001',
            minReplicas        : '1',
            maxReplicas        : '1',
            cpu                : '0.5',
            memory             : '1Gi',
            envVars            : [
                'ASPNETCORE_ENVIRONMENT': 'Development',
                'LOG_LEVEL'            : 'Debug'
            ]
        ],
        staging: [
            resourceGroup      : 'rg-myapp-staging',
            containerAppName   : 'aca-myapp-staging',
            containerAppEnv    : 'myapp-env-staging',
            acrName            : 'myacrstaging',
            imageName          : 'myapp',
            subscriptionId     : '00000000-0000-0000-0000-000000000002',
            minReplicas        : '2',
            maxReplicas        : '4',
            cpu                : '1',
            memory             : '2Gi',
            envVars            : [
                'ASPNETCORE_ENVIRONMENT': 'Staging',
                'LOG_LEVEL'             : 'Information'
            ]
        ],
        prod: [
            resourceGroup      : 'rg-myapp-prod',
            containerAppName   : 'aca-myapp-prod',
            containerAppEnv    : 'myapp-env-prod',
            acrName            : 'myacrprod',
            imageName          : 'myapp',
            subscriptionId     : '00000000-0000-0000-0000-000000000003',
            minReplicas        : '2',
            maxReplicas        : '10',
            cpu                : '1',
            memory             : '2Gi',
            envVars            : [
                'ASPNETCORE_ENVIRONMENT': 'Production',
                'LOG_LEVEL'             : 'Warning'
            ]
        ]
    ]
    return configs[environment] ?: configs.dev
}

return this
