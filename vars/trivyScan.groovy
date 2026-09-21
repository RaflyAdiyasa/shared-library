
def call(Map args = [:]) {
    def type       = args.get('type', 'fs')
    def target     = args.get('target', '.')
    def failOnVuln = args.get('failOnVuln', true)
    def exitCode   = failOnVuln ? "1" : "0"

    echo "DEBUG type       = ${type}"
    echo "DEBUG target     = ${target}"
    echo "DEBUG failOnVuln  = ${failOnVuln}"
    echo "DEBUG exitCode   = ${exitCode}"

  
    def cacheVolume = "-v trivy-cache:/root/.cache/"

    if (type == 'fs') {
        echo "=== [DevSecOps] Memulai Trivy FileSystem, Secrets, & IaC Scan ==="
        sh """
            docker run --rm \
              -v \$(pwd):/workspace \
              ${cacheVolume} \
              aquasec/trivy:latest fs \
              --scanners vuln,secret,config \
              --severity HIGH,CRITICAL \
              --exit-code ${exitCode} \
              /workspace
        """
    } else if (type == 'image') {
        echo "=== [DevSecOps] Memulai Trivy Container Image Scan pada: ${target} ==="
        // Scan docker image local menggunakan docker socket
        sh """
            docker run --rm \
              -v /var/run/docker.sock:/var/run/docker.sock \
              ${cacheVolume} \
              aquasec/trivy:latest image \
              --severity HIGH,CRITICAL \
              --exit-code ${exitCode} \
              ${target}
        """
    }
}
