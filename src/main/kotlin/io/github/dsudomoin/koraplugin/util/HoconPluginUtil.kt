package io.github.dsudomoin.koraplugin.util

import com.intellij.openapi.project.Project
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope

object HoconPluginUtil {

    /**
     * True when the optional HOCON plugin is installed and enabled.
     * The optional dependency declared in plugin.xml makes its classes visible
     * to this plugin's classloader only when the plugin is actually active.
     */
    val isHoconPluginEnabled: Boolean by lazy {
        try {
            Class.forName("org.jetbrains.plugins.hocon.psi.HKey")
            true
        } catch (_: ClassNotFoundException) {
            false
        } catch (_: NoClassDefFoundError) {
            false
        }
    }

    /** True when the project contains at least one `.conf` file. Requires smart mode. */
    fun projectHasConfFiles(project: Project): Boolean {
        return FilenameIndex.getAllFilesByExt(project, "conf", GlobalSearchScope.projectScope(project)).isNotEmpty()
    }
}
