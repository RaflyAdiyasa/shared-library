package com.course

/**
 * PipelineConfig — config-as-data.
 * Mendukung konfigurasi terpusat langsung dari parameter Jenkins Job UI,
 * tanpa mewajibkan file .cicd/pipeline.yaml di repositori developer.
 */
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

    /**
     * Bangun config dari Map parameter Jenkins Job atau file YAML.
     */
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
        } else {
            cfg.buildTool   = raw.get('buildTool', 'docker')
            cfg.buildBranch = raw.get('buildBranch', 'development')
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

    /** Full image name (tanpa tag). */
    String imageName() {
        if (customImageName) {
            return customImageName
        }
        if (registryRegion && registryProject) {
            return "${registryRegion}-docker.pkg.dev/${registryProject}/${registryRepository}/${appName}"
        }
        // Default: Docker Hub format (username/appName) — set via customImageName atau appName
        return appName
    }
}
