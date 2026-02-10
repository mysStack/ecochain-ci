// Logger for CI Pipeline
package org.ecochain.ci.common

class Logger implements Serializable {

    def script

    Logger(script) {
        this.script = script
    }

    def info(String msg) {
        script.echo "[INFO] ${msg}"
    }

    def warn(String msg) {
        script.echo "[WARN] ${msg}"
    }

    def error(String msg) {
        script.echo "[ERROR] ${msg}"
    }
}
