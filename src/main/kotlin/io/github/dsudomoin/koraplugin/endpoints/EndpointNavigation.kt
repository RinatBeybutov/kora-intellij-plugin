package io.github.dsudomoin.koraplugin.endpoints

import com.intellij.json.psi.JsonProperty
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ui.JBUI
import org.jetbrains.yaml.psi.YAMLKeyValue
import java.awt.Component
import javax.swing.DefaultListCellRenderer
import javax.swing.JList

/**
 * Click-to-navigate for endpoint/topic targets. Targets are deflated to strings, so the
 * element is re-resolved in a fresh read action at click time; stale coordinates degrade
 * gracefully (nearest element or a warning balloon).
 */
object EndpointNavigation {

    fun navigate(project: Project, targets: List<EndpointTarget>) {
        if (targets.isEmpty()) return
        val elements = ReadAction.compute<List<PsiElement>, RuntimeException> {
            targets.mapNotNull { resolveTarget(project, it) }.distinct()
        }
        when (elements.size) {
            0 -> Notifications.Bus.notify(
                Notification("Kora", "Kora", "Endpoint location no longer exists", NotificationType.WARNING),
                project,
            )
            1 -> navigateToElement(elements.single())
            else -> showChooser(elements)
        }
    }

    private fun resolveTarget(project: Project, target: EndpointTarget): PsiElement? {
        if (target.classFqn != null) {
            val psiClass = JavaPsiFacade.getInstance(project)
                .findClass(target.classFqn, GlobalSearchScope.allScope(project)) ?: return null
            if (target.methodName == null) return psiClass
            return psiClass.findMethodsByName(target.methodName, false).firstOrNull()
        }
        if (target.specFileUrl != null) {
            val virtualFile = VirtualFileManager.getInstance().findFileByUrl(target.specFileUrl) ?: return null
            val psiFile = com.intellij.psi.PsiManager.getInstance(project).findFile(virtualFile) ?: return null
            val anchor = psiFile.findElementAt(target.offset) ?: psiFile
            return PsiTreeUtil.getParentOfType(anchor, YAMLKeyValue::class.java)
                ?: PsiTreeUtil.getParentOfType(anchor, JsonProperty::class.java)
                ?: anchor
        }
        return null
    }

    private fun navigateToElement(element: PsiElement) {
        val file = element.containingFile?.virtualFile ?: return
        OpenFileDescriptor(element.project, file, element.textOffset).navigate(true)
    }

    private fun showChooser(elements: List<PsiElement>) {
        JBPopupFactory.getInstance().createPopupChooserBuilder(elements)
            .setTitle("Choose Endpoint Location")
            .setRenderer(object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    selected: Boolean,
                    hasFocus: Boolean,
                ): Component {
                    val component = super.getListCellRendererComponent(list, value, index, selected, hasFocus)
                    (value as? PsiElement)?.let { text = presentable(it) }
                    // After super: DefaultListCellRenderer resets the border from UIManager
                    // on every call, so a border set in init would be silently discarded.
                    border = JBUI.Borders.empty(2, 10)
                    return component
                }
            })
            .setItemChosenCallback { element -> navigateToElement(element) }
            .createPopup()
            .showInFocusCenter()
    }

    /** "method — File.kt": the element name distinguishes rows within the same file. */
    private fun presentable(element: PsiElement): String {
        val name = (element as? PsiNamedElement)?.name
            ?: element.text.take(60).takeIf { it.isNotBlank() }
        val fileName = element.containingFile?.name
        return when {
            name == null -> fileName ?: "(unknown)"
            fileName == null -> name
            else -> "$name — $fileName"
        }
    }
}
