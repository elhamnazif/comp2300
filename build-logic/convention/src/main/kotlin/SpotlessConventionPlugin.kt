import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.comp2300.buildlogic.configureSpotless
import org.comp2300.buildlogic.libs

@Suppress("unused")
class SpotlessConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = libs.plugins.spotless.get().pluginId)
            val extension = extensions.getByType<SpotlessExtension>()
            configureSpotless(extension)
        }
    }
}