import com.course.PipelineConfig

/**
 * buildAndPush — build & push image ke Docker Hub + load ke KinD.
 *
 * Mode:
 * - docker (default untuk VM agent): docker build + docker push
 * - kaniko: untuk K8s agent (rootless, tanpa Docker daemon)
 *
 * Auth ke Docker Hub:
 * - Credentials 'dockerhub-credentials' (username/password) di Jenkins
 *
 * Tag = Jenkins BUILD_NUMBER (sequential, traceable)
 */
def call(PipelineConfig cfg, String buildNumber) {
    def image = "${cfg.imageName()}:${buildNumber}"
    def latestImage = "${cfg.imageName()}:latest"

    if (cfg.buildTool == 'kaniko') {
        echo "Building dengan Kaniko: ${image}"
        sh """
            docker run --rm \
              -v \$(pwd):/workspace \
              -v \$HOME/.docker:/kaniko/.docker:ro \
              gcr.io/kaniko-project/executor:latest \
              --context=dir:///workspace \
              --dockerfile=/workspace/Dockerfile \
              --destination=${image} \
              --destination=${latestImage} \
              --cache=true
        """
    } else {
        // Docker mode — push ke Docker Hub
        echo "Building dengan Docker: ${image}"

        withCredentials([usernamePassword(
            credentialsId: 'dockerhub-credentials',
            usernameVariable: 'DOCKER_USER',
            passwordVariable: 'DOCKER_PASS'
        )]) {
            sh 'echo "${DOCKER_PASS}" | docker login -u "${DOCKER_USER}" --password-stdin'
        }

        sh "docker build -t ${image} -t ${latestImage} ."

        if (cfg.enableSecurityScan) {
            trivyScan(type: 'image', target: image, failOnVuln: true)
        }

        sh "docker push ${image}"
        sh "docker push ${latestImage}"

        // Load ke KinD cluster (agar pod tidak perlu pull dari registry)
        def kindCluster = cfg.kindClusterName ?: 'devops-local-cluster'
        sh "kind load docker-image ${image} --name ${kindCluster} || echo 'KinD load skipped (cluster not found)'"
    }

    echo "Image pushed: ${image}"
    return image
}
