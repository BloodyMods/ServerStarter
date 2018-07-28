package atm.bloodworkxgaming.serverstarter.packtype.curse

import atm.bloodworkxgaming.serverstarter.config.ConfigFile

class CurseIDPackType(configFile: ConfigFile) : CursePackType(configFile) {
    override fun cleanUrl(url: String): String {
        val split = url.split(":")
        if (split.size != 2) {
            throw RuntimeException("In CurseID Format the ID has to be given in format {packid}:{fileid}")
        }

        return "https://minecraft.curseforge.com/projects/${split[0]}/files/${split[1]}/download"
    }
}