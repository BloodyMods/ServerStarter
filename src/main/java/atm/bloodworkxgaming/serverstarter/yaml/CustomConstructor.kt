package atm.bloodworkxgaming.serverstarter.yaml

import atm.bloodworkxgaming.serverstarter.ServerStarter
import org.yaml.snakeyaml.constructor.Constructor
import org.yaml.snakeyaml.nodes.Node
import java.util.*

class CustomConstructor(clazz: Class<*>) : Constructor(clazz) {
    override fun constructObject(node: Node?): Any? {
        val o = super.constructObject(node)
        val clazz = node?.type

        if (o == null && clazz != null) {
            ServerStarter.LOGGER.info("Changing $node of type ${node.type} to the a default value")

            return when {
                clazz.isAssignableFrom(String::class.java) -> "" as Any? // no, this cast is indeed needed.
                clazz.isAssignableFrom(List::class.java) -> Collections.EMPTY_LIST
                clazz.isAssignableFrom(Map::class.java) -> Collections.EMPTY_MAP
                else -> o
            }
        }

        return o
    }
}