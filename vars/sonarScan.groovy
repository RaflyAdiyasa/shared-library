import com.course.PipelineConfig


def call(PipelineConfig cfg) {

    def projectKey = cfg.sonarProjectKey
    def projectName = cfg.sonarProjectName ?: projectKey
    def sources = cfg.sonarSources ?: '.'
    def sonarqubeKeyID = cfg.sonarqubeKeyID ?: 'sonarqube'
    def exclusions = cfg.sonarExclusions ?: ''
    def scannerHome = tool 'sonarqube'


    withSonarQubeEnv(sonarqubeKeyID) {
        def command = """
            ${scannerHome}/bin/sonar-scanner \
              -Dsonar.projectKey=${projectKey} \
              -Dsonar.projectName=${projectName} \
              -Dsonar.sources=${sources}
        """

        if (exclusions) {
            command += """
              -Dsonar.exclusions=${exclusions}
            """
        }

        sh command
    }
}