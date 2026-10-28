package io.github.dsudomoin.koraplugin.endpoints

import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * "Kora Structure" tool window: HTTP endpoints (annotation-based and OpenAPI-generated)
 * and incoming Kafka topics, collected in the background. See ROADMAP.md F-09.
 */
class KoraEndpointsToolWindowFactory : ToolWindowFactory, DumbAware {

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val panel = KoraEndpointsPanel(project)
        val content = ContentFactory.getInstance().createContent(panel, "", false)
        content.setDisposer(panel)
        toolWindow.contentManager.addContent(content)
    }
}
