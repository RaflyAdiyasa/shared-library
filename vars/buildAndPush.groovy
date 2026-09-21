import com.course.PipelineConfig

def call(PipelineConfig cfg, String buildNumber) {
    def gitSha = sh(script: 'git rev-parse --short HEAD', returnStdout: true).trim()
    def image = "${cfg.imageName()}:${buildNumber}"

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
              --no-push \
              --tar-path=/tmp/image.tar
        """
    } else {
        // Docker mode — build di host Docker via shared socket
        echo "Building dengan Docker (BuildKit enabled): ${image}"


        def gitMsg = sh(script: 'git log -1 --pretty=%s', returnStdout: true).trim().replace('"', '\\"')

        sh """
            DOCKER_BUILDKIT=1 docker build \
              --build-arg BUILD_NUMBER=${buildNumber} \
              --build-arg COMMIT_SHA=${gitSha} \
              --build-arg "COMMIT_MESSAGE=${gitMsg}" \
              -t ${image} .
        """

        if (cfg.enableSecurityScan) {
            trivyScan(type: 'image', target: image, failOnVuln: false)
        }
    }

    withDockerRegistry(credentialsId: cfg.dockerHubCredID ) {
        sh "docker push ${image}"
    }
        echo "Image loaded to Dockerhub: ${image}"



    // Load image ke KinD cluster (shared Docker socket = kind bisa akses)
    // def kindCluster = cfg.kindClusterName ?: 'devops-local-cluster'
    // sh "kind load docker-image ${image} --name ${kindCluster}"

    // echo "Image loaded to KinD: ${image}"
    return image
}
