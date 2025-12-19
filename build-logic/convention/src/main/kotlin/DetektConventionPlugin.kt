import org.comp2300.buildlogic.configureDetekt
import dev.detekt.gradle.extensions.DetektExtension
import org.comp2300.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

@Suppress("unused")
class DetektConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugins.detekt.get().pluginId)
            val extension = extensions.getByType<DetektExtension>()
            configureDetekt(extension)
        }
    }
}