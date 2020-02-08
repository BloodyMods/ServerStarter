package atm.bloodworkxgaming.serverstarter.util


data class ArgsConfig(
        var yamlPath: String = "./server-setup-config.yaml",
        var loggerPath: String = "./serverstarter.log",
        var installForge: Boolean = true
) {
    companion object {
        fun readFromArgs(args: Array<String>): ArgsConfig {
            val iter = args.iterator()
            val argsConfig = ArgsConfig()

            val current = iter.next()
            while (iter.hasNext()) {
                when {
                    current == "-install" -> argsConfig.installForge = false
                    current.startsWith("-logger:") -> readPath(iter, current.substring(8))
                    current.startsWith("-yaml:") -> readPath(iter, current.substring(6))
                }
            }

            return argsConfig
        }

        private fun readPath(iter: Iterator<String>, rest: String): String {
            return if (!rest.startsWith("\"")) rest
            else {
                val sb = StringBuilder()
                sb.append(rest.substring(1))
                inner@ while (iter.hasNext()) {
                    val partial = iter.next()
                    if (!partial.endsWith('"')) sb.append(partial)
                    else {
                        sb.append(partial.substring(0, partial.length - 1))
                        break@inner
                    }
                }

                sb.toString()
            }
        }
    }
}