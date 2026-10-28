package io.github.dsudomoin.koraplugin

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.github.dsudomoin.koraplugin.config.configKeyDisplayPath
import io.github.dsudomoin.koraplugin.config.projectHasConfConfig
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.junit.Assume.assumeTrue

class ConfigKeyPresentationTest : BasePlatformTestCase() {

    fun `test configKeyDisplayPath builds dotted path from nested yaml keys`() {
        val yaml = myFixture.addFileToProject(
            "application.yaml",
            """
            orphanRulesCleanup:
              scheduler:
                pageSize: 10
            """.trimIndent(),
        ) as YAMLFile

        val pageSize = findYamlKeyValue(yaml, "pageSize")
        assertNotNull("YAMLKeyValue pageSize not found", pageSize)
        assertEquals("orphanRulesCleanup.scheduler.pageSize", configKeyDisplayPath(pageSize!!))
    }

    fun `test configKeyDisplayPath keeps kebab-case key verbatim`() {
        val yaml = myFixture.addFileToProject(
            "application.yaml",
            """
            hub:
              spawn-location: default
            """.trimIndent(),
        ) as YAMLFile

        val key = findYamlKeyValue(yaml, "spawn-location")
        assertNotNull("YAMLKeyValue spawn-location not found", key)
        assertEquals("hub.spawn-location", configKeyDisplayPath(key!!))
    }

    fun `test configKeyDisplayPath builds dotted path for hocon key`() {
        val hKeyClass = try {
            Class.forName("org.jetbrains.plugins.hocon.psi.HKey")
        } catch (_: Throwable) {
            null
        }
        assumeTrue("HOCON plugin is not available in this test environment", hKeyClass != null)

        myFixture.configureByText(
            "application.conf",
            """
            orphanRulesCleanup {
              scheduler {
                pageSize = 10
              }
            }
            """.trimIndent(),
        )

        val leaf = myFixture.findElementByText("pageSize", PsiElement::class.java)
        @Suppress("UNCHECKED_CAST")
        val hKey = PsiTreeUtil.getParentOfType(leaf, hKeyClass as Class<PsiElement>, false)
        assertNotNull("HKey element not found for pageSize", hKey)
        assertEquals("orphanRulesCleanup.scheduler.pageSize", configKeyDisplayPath(hKey!!))
    }

    fun `test projectHasConfFiles is false without conf files`() {
        myFixture.configureByText("Dummy.java", "class Dummy {}")
        assertFalse(projectHasConfConfig(project))
    }

    fun `test projectHasConfFiles is true with conf file`() {
        myFixture.addFileToProject("application.conf", "key = 1")
        myFixture.configureByText("Dummy.java", "class Dummy {}")
        assertTrue(projectHasConfConfig(project))
    }

    fun `test hocon plugin is enabled in test environment`() {
        // The HOCON plugin is a test dependency (see build.gradle.kts), so the
        // "install HOCON" notification branch is not reachable in tests and is
        // covered by manual verification only.
        assertNotNull(Class.forName("org.jetbrains.plugins.hocon.psi.HKey"))
    }

    private fun findYamlKeyValue(element: PsiElement, keyText: String): YAMLKeyValue? {
        if (element is YAMLKeyValue && element.keyText == keyText) return element
        for (child in element.children) {
            findYamlKeyValue(child, keyText)?.let { return it }
        }
        return null
    }
}
