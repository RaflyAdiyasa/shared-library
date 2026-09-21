import com.course.PipelineConfig

def call(PipelineConfig cfg, String buildNumber, String prNum = '') {
    def fullImage = "${cfg.imageName()}:${buildNumber}"
    def deployFile = cfg.gitopsDeployFile ?: 'deployment.yaml'
    def deployPath = "${cfg.gitopsPath}/${deployFile}"
    def prSuffix = (prNum && prNum != '0') ? " (PR #${prNum})" : ""

    def repoHost = cfg.gitopsRepoUrl.replaceFirst('https://', '')

    lock('gitops') {
        withCredentials([usernamePassword(
            credentialsId: 'github-jenkins-token',
            usernameVariable: 'GIT_USER',
            passwordVariable: 'GIT_TOKEN'
        )]) {
            dir('gitops-update') {
                
                deleteDir()
                    retry(3) {
                    
                    // Clone repo GitOps
                    sh """
                        git clone --depth=1 --branch ${cfg.gitopsBranch} \
                          https://\${GIT_USER}:\${GIT_TOKEN}@${repoHost} .
                    """

                    // Update image di patch/deployment.yaml menggunakan sed
                    // Matches: "image: <anything>" di dalam containers[] block
                    sh """
                        yq -i '(.spec.template.spec.containers[] | select(.name == "api") | .image) = "'"${fullImage}"'"' ${deployPath}
                    """

                    // Verifikasi update
                    sh "grep 'image:' ${deployPath}"

                    // Commit + push dengan rebase untuk avoid race condition
                    sh """
                        git config user.email "jenkins@course.local"
                        git config user.name "jenkins-ci"
                        git add ${deployPath}
                        git diff --cached --quiet || git commit -m "ci: ${cfg.appName} image -> build #${buildNumber}${prSuffix} [skip ci]"
                        git pull --rebase https://\${GIT_USER}:\${GIT_TOKEN}@${repoHost} ${cfg.gitopsBranch}
                        git push https://\${GIT_USER}:\${GIT_TOKEN}@${repoHost} HEAD:${cfg.gitopsBranch}
                    """
                }
            }
        }
    }
    echo "GitOps updated: ${deployPath} -> ${fullImage}"
    echo "ArgoCD akan detect commit ini dan trigger deployment."
}
