package com.course

class PipelineConfig implements Serializable {

    String appName
    String language
    String testCommand

    String customImageName
    String registryRegion
    String registryProject
    String registryRepository

    String gitopsRepoUrl
    String gitopsBranch
    String gitopsPath
    String gitopsDeployFile   // default: deployment.yaml (was rolloutFile)

    String buildTool      // docker | kaniko
    String buildBranch

    String slackChannel
    String agentLabel
    Boolean enableSecurityScan
    String kindClusterName    // KinD cluster name for image loading

    String dockerHubCredID

    String sonarProjectKey
    String sonarProjectName
    String sonarSources
    String sonarqubeKeyID
    String sonarExclusions

    String sonarQualityGateTimeoutMinutes
    Boolean sonarQualityGateAbortPipeline

    static PipelineConfig fromMap(Map raw) {
        if (raw == null) {
            raw = [:]
        }
        def cfg = new PipelineConfig()

        cfg.appName            = raw.get('appName') ?: raw.get('app_name') ?: 'app'
        cfg.language           = raw.get('language', 'generic')
        cfg.enableSecurityScan = raw.containsKey('enableSecurityScan') ? raw.get('enableSecurityScan') : (raw.containsKey('enable_security_scan') ? raw.get('enable_security_scan') : true) as Boolean

        def test = raw.get('test')
        if (test instanceof Map) {
            cfg.testCommand = test.get('command', '')
        } else {
            cfg.testCommand = raw.get('testCommand') ?: ''
        }

        cfg.customImageName = raw.get('imageName') ?: raw.get('image_name') ?: raw.get('customImageName')

        def registry = raw.get('registry')
        if (registry instanceof Map) {
            cfg.registryRegion     = registry.get('region', '')
            cfg.registryProject    = registry.get('project_id', '')
            cfg.registryRepository = registry.get('repository', 'docker-images-repo')
        } else {
            cfg.registryRegion     = raw.get('registryRegion', '')
            cfg.registryProject    = raw.get('registryProject', '')
            cfg.registryRepository = raw.get('registryRepository', 'docker-images-repo')
        }

        def gitops = raw.get('gitops')
        if (gitops instanceof Map) {
            cfg.gitopsRepoUrl     = gitops.get('repo_url') ?: gitops.get('repoUrl', '')
            cfg.gitopsBranch      = gitops.get('branch', 'main')
            cfg.gitopsPath        = gitops.get('path', '')
            cfg.gitopsDeployFile  = gitops.get('deploy_file', 'deployment.yaml')
        } else {
            cfg.gitopsRepoUrl     = raw.get('gitopsRepoUrl') ?: raw.get('gitopsRepo', '')
            cfg.gitopsBranch      = raw.get('gitopsBranch', 'main')
            cfg.gitopsPath        = raw.get('gitopsPath', '')
            cfg.gitopsDeployFile  = raw.get('gitopsDeployFile', 'deployment.yaml')
        }

        def build = raw.get('build')
        if (build instanceof Map) {
            cfg.buildTool   = build.get('tool', 'docker')
            cfg.buildBranch = build.get('branch', 'development')
            cfg.dockerHubCredID = build.get('dockerHubCredID', 'dockerhub-credentials')
        } else {
            cfg.buildTool   = raw.get('buildTool', 'docker')
            cfg.buildBranch = raw.get('buildBranch', 'development')
            cfg.dockerHubCredID = raw.get('dockerHubCredID', 'dockerhub-credentials')
        }

        def sonar = raw.get('sonar')
        if (sonar instanceof Map){
            cfg.sonarProjectKey     = sonar.get('sonarProjectKey','Learn')
            cfg.sonarProjectName    = sonar.get('sonarProjectName','Learn')
            cfg.sonarSources        = sonar.get('sonarSources','.')
            cfg.sonarqubeKeyID      = sonar.get('sonarqubeKeyID','sonarqube')
            cfg.sonarExclusions     = sonar.get('sonarExclusions','')
            cfg.sonarQualityGateTimeoutMinutes = sonar.get('sonarQualityGateTimeoutMinutes','5')
            cfg.sonarQualityGateAbortPipeline = sonar.get('sonarQualityGateAbortPipeline', true) as Boolean
        } else {
            cfg.sonarProjectKey     = raw.get('sonarProjectKey','Learn')
            cfg.sonarProjectName    = raw.get('sonarProjectName','Learn')
            cfg.sonarSources        = raw.get('sonarSources','.')
            cfg.sonarqubeKeyID      = raw.get('sonarqubeKeyID','sonarqube')
            cfg.sonarExclusions     = raw.get('sonarExclusions','')
            cfg.sonarQualityGateTimeoutMinutes = raw.get('sonarQualityGateTimeoutMinutes','5')      
            cfg.sonarQualityGateAbortPipeline = raw.get('sonarQualityGateAbortPipeline', true) as Boolean
        }

        def slack = raw.get('slack')
        if (slack instanceof Map) {
            cfg.slackChannel = slack.get('channel', '')
        } else {
            cfg.slackChannel = raw.get('slackChannel', '')
        }

        cfg.agentLabel = raw.get('agentLabel') ?: raw.get('agent_label', 'built-in')
        cfg.kindClusterName = raw.get('kindClusterName') ?: raw.get('kind_cluster_name', 'devops-local-cluster')
        return cfg
    }

    String imageName() {
        if (customImageName) {
            return customImageName
        }
        if (registryRegion && registryProject) {
            return "${registryRegion}-docker.pkg.dev/${registryProject}/${registryRepository}/${appName}"
        }
        
        return appName
    }
}
