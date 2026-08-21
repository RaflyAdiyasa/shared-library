import com.course.PipelineConfig

/**
 * updateGitops — perbarui image tag di repo GitOps, commit & push.
 *
 * CI update image langsung di patch/deployment.yaml menggunakan sed.
 *
 * Pola dari produksi: lock('gitops') + rebase retry agar aman dari race condition.
 * ArgoCD mendeteksi perubahan commit → trigger sync → deploy.
 *
 * Format image di patch/deployment.yaml:
 *   image: DOCKERHUB_USER/APP:TAG  # <- CI replaces this line
 */
def call(PipelineConfig cfg, String buildNumber, String prNum = '') {
    def fullImage = "${cfg.imageName()}:${buildNumber}"
    def deployFile = cfg.gitopsDeployFile ?: 'deployment.yaml'
    def deployPath = "${cfg.gitopsPath}/patch/${deployFile}"
    def prSuffix = (prNum && prNum != '0') ? " (PR #${prNum})" : ""

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
                          https://\${GIT_USER}:\${GIT_TOKEN}@${cfg.gitopsRepoUrl} .
                    """

                    // Update image di patch/deployment.yaml menggunakan sed
                    // Matches: "image: <anything>" di dalam containers[] block
                    sh """
                        sed -i 's|image:.*|image: ${fullImage}|g' ${deployPath}
                    """

                    // Verifikasi update
                    sh "grep 'image:' ${deployPath}"

                    // Commit + push dengan rebase untuk avoid race condition
                    sh """
                        git config user.email "jenkins@course.local"
                        git config user.name "jenkins-ci"
                        git add ${deployPath}
                        git diff --cached --quiet || git commit -m "ci: ${cfg.appName} image -> build #${buildNumber}${prSuffix} [skip ci]"
                        git pull --rebase https://\${GIT_USER}:\${GIT_TOKEN}@${cfg.gitopsRepoUrl} ${cfg.gitopsBranch}
                        git push https://\${GIT_USER}:\${GIT_TOKEN}@${cfg.gitopsRepoUrl} HEAD:${cfg.gitopsBranch}
                    """
                }
            }
        }
    }
    echo "GitOps updated: ${deployPath} -> ${fullImage}"
    echo "ArgoCD akan detect commit ini dan trigger deployment."
}
