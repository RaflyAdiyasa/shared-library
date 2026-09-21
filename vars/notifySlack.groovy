
def call(String channel, String status, String message, def previousThread) {
    if (!channel?.trim()) {
        echo "[slack:skip] channel kosong, skip notifikasi."
        return null
    }

    def color = [
        'STARTED': '#439FE0',
        'SUCCESS': 'good',
        'FAILURE': 'danger'
    ].get(status, '#cccccc')

    def text = "*${status}* — ${message}\nJob: ${env.JOB_NAME} #${env.BUILD_NUMBER}\n${env.BUILD_URL}"

    try {

        def resp = slackSend(
            channel: channel,
            color: color,
            message: text
        )
        return resp
    } catch (Exception e) {
        echo "[slack:error] Gagal kirim notifikasi: ${e.getMessage()}"
        return null
    }
}
