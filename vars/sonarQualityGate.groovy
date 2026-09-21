import com.course.PipelineConfig

def call(PipelineConfig cfg) {

    def timeoutMinutes = cfg.sonarQualityGateTimeoutMinutes ?: 5
    def abortPipeline = cfg.sonarQualityGateAbortPipeline != false

    timeout(time: timeoutMinutes, unit: 'MINUTES') {
        waitForQualityGate(
            abortPipeline: abortPipeline
        )
    }
}