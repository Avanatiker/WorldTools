package org.waste.of.time.storage

class PathTreeNode(private val name: String) {
    private val children = mutableListOf<PathTreeNode>()

    fun getOrCreateChild(name: String): PathTreeNode {
        children.find { it.name == name }?.let { return it }
        val newChild = PathTreeNode(name)
        children.add(newChild)
        return newChild
    }

    fun buildTreeString(stringBuilder: StringBuilder, prefix: String, isLast: Boolean) {
        stringBuilder.apply {
            if (prefix.isNotEmpty()) {
                append(prefix)
                append(if (isLast) "└─ " else "├─ ")
                append(name)
                append("\n")
            } else {
                append(name)
                append("\n")
            }

            children.forEachIndexed { index, treeNode ->
                val isLastChild = index == children.size - 1
                val childPrefix = if (isLast) "$prefix    " else "$prefix│   "
                treeNode.buildTreeString(this, childPrefix, isLastChild)
            }
        }
    }

    companion object {
        fun buildTree(paths: List<String>): String {
            val root = PathTreeNode("minecraft")
            paths.forEach { path ->
                var currentNode = root
                path.split("/").forEach {
                    currentNode = currentNode.getOrCreateChild(it)
                }
            }
            val stringBuilder = StringBuilder()
            root.buildTreeString(stringBuilder, "", true)
            return stringBuilder.toString()
        }
    }
}