import com.course.PipelineConfig

def call(Map args = [:]) {
    def appRepoUrl  = args.get('appRepoUrl') ?: env.APP_REPO_URL ?: env.GIT_URL
    def configPath  = args.get('configPath', '.cicd/pipeline.yaml')
    def credentialsId = args.get('credentialsId', 'github-jenkins-token') 
    def cfg         = null
    def gitSha      = null
    def branchName  = null

    if (!appRepoUrl) {
        error("containerPipeline: 'appRepoUrl' tidak ditemukan. Berikan parameter 'appRepoUrl' pada containerPipeline(...) di Jenkins Job.")
    }

    node {
        try {
            stage('Checkout App Repo') {
                branchName = env.pr_base_branch ?: args.get('buildBranch') ?: 'development'

                git branch: branchName,
                    credentialsId: "${credentialsId}",
                    url: appRepoUrl

                gitSha = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
                echo "Checked out: ${appRepoUrl} @ ${branchName} (${gitSha})"

                def prNumber = env.pr_num ?: args.get('pr_num') ?: ''
                if (prNumber && prNumber.toString() != '0' && prNumber.toString() != '') {
                    currentBuild.displayName = "#${BUILD_NUMBER} - PR #${prNumber} (${gitSha})"
                } else {
                    currentBuild.displayName = "#${BUILD_NUMBER} (${gitSha})"
                }

 
                Map combinedConfig = new HashMap(args)

                if (fileExists(configPath)) {
                    def rawYaml = readYaml(file: configPath)
                    if (rawYaml) {
                        combinedConfig.putAll(rawYaml)
                    }
                }

                cfg = PipelineConfig.fromMap(combinedConfig)
                echo "App: ${cfg.appName} | branch: ${branchName} | sha: ${gitSha} | image: ${cfg.imageName()}"
            }

            if (cfg.enableSecurityScan) {
                stage('Security Scan (Source)') {
                    trivyScan(type: 'fs', target: '.', failOnVuln: false)
                }
            }

            stage('Test') {
                if (cfg.testCommand?.trim()) {
                    sh cfg.testCommand
                } else {
                    echo 'Tidak ada test.command di pipeline.yaml — dilewati.'
                }
            }

            stage('SonarScan') {
                sonarScan(cfg)
            }

            stage('QualityGate') {
                sonarQualityGate(cfg)
            }


            if (branchName == cfg.buildBranch) {
                stage('Build & Push') {
                    buildAndPush(cfg, env.BUILD_NUMBER)
                }
                stage('Update GitOps') {
                    def prNumber = env.pr_num ?: args.get('pr_num') ?: ''
                    updateGitops(cfg, env.BUILD_NUMBER, prNumber.toString())
                }
            } else {
                echo "Branch '${branchName}' != build branch '${cfg.buildBranch}'. Skip build."
            }

            //notifySlack(cfg.slackChannel, 'SUCCESS', "Build ${cfg.appName} @ ${branchName} (${gitSha}) berhasil.", null)

        } catch (Exception e) {
            // if (cfg != null) {
            //     notifySlack(cfg.slackChannel, 'FAILURE', "Build gagal: ${e.getMessage()}", null)
            // }
            throw e
        }
    }
}
