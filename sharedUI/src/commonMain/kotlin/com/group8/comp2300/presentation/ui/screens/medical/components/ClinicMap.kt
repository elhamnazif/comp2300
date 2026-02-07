@file:Suppress("FunctionName")

package com.group8.comp2300.presentation.ui.screens.medical.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.group8.comp2300.domain.model.medical.Clinic
import kotlinx.serialization.json.JsonPrimitive
import org.maplibre.compose.camera.CameraPosition
import org.maplibre.compose.camera.rememberCameraState
import org.maplibre.compose.expressions.dsl.asString
import org.maplibre.compose.expressions.dsl.case
import org.maplibre.compose.expressions.dsl.const
import org.maplibre.compose.expressions.dsl.feature
import org.maplibre.compose.expressions.dsl.switch
import org.maplibre.compose.layers.CircleLayer
import org.maplibre.compose.map.MapOptions
import org.maplibre.compose.map.MaplibreMap
import org.maplibre.compose.map.OrnamentOptions
import org.maplibre.compose.sources.GeoJsonData
import org.maplibre.compose.sources.rememberGeoJsonSource
import org.maplibre.compose.style.BaseStyle
import org.maplibre.compose.util.ClickResult
import org.maplibre.spatialk.geojson.*
import kotlin.time.Duration.Companion.seconds

@Composable
fun ClinicMap(
    clinics: List<Clinic>,
    selectedClinic: Clinic? = null,
    onClinicSelect: (Clinic) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    // 1. Dynamic Styling Colors based on Material Theme
    // We can pass Color objects directly to const() in the DSL
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val surfaceColor = MaterialTheme.colorScheme.surface

    // Choose map style based on system theme
    // Dark Style kinda sucks lol
    // val isDark = isSystemInDarkTheme()
    // val styleUri = if (isDark) "https://tiles.openfreemap.org/styles/dark" else
    // "https://tiles.openfreemap.org/styles/liberty"
    val styleUri = "https://tiles.openfreemap.org/styles/liberty"

    // 2. Camera State
    val cameraState =
        rememberCameraState(
            firstPosition =
            CameraPosition(
                target = Position(101.6841, 3.1319), // KL Default
                zoom = 11.0,
            ),
        )

    // 3. Construct GeoJSON with "selected" property
    // We rebuild this when 'selectedClinic' changes to trigger style updates
    val data =
        remember(clinics, selectedClinic) {
            FeatureCollection(
                clinics.map { clinic ->
                    Feature(
                        id = JsonPrimitive(clinic.id),
                        geometry = Point(Position(clinic.lng, clinic.lat)),
                        properties =
                        mapOf(
                            "title" to JsonPrimitive(clinic.name),
                            // Store as String "true"/"false" for
                            // robust matching in 'switch'
                            // expression
                            "isSelected" to
                                JsonPrimitive(
                                    if (clinic.id ==
                                        selectedClinic
                                            ?.id
                                    ) {
                                        "true"
                                    } else {
                                        "false"
                                    },
                                ),
                        ),
                    )
                },
            )
                .toJson()
        }

    // 4. Animate Camera on Selection
    LaunchedEffect(selectedClinic) {
        selectedClinic?.let {
            cameraState.animateTo(
                finalPosition =
                cameraState.position.copy(
                    target = Position(it.lng, it.lat),
                    zoom = 15.0,
                ),
                duration = 1.seconds,
            )
        }
    }

    // 5. Apply Modifier here to respect padding/sizing from parent
    Box(modifier = modifier.fillMaxSize()) {
        MaplibreMap(
            modifier = Modifier.fillMaxSize(),
            baseStyle = BaseStyle.Uri(styleUri),
            options = MapOptions(ornamentOptions = OrnamentOptions.OnlyLogo),
            cameraState = cameraState,
        ) {
            val clinicSource =
                rememberGeoJsonSource(
                    data = GeoJsonData.JsonString(data),
                )

            CircleLayer(
                id = "clinics-layer",
                source = clinicSource,
                color =
                switch(
                    feature["isSelected"].asString(),
                    case(label = "true", output = const(primaryColor)),
                    fallback = const(secondaryColor),
                ),
                radius =
                switch(
                    feature["isSelected"].asString(),
                    case(label = "true", output = const(12.dp)),
                    fallback = const(8.dp),
                ),
                strokeColor = const(surfaceColor),
                strokeWidth = const(2.dp),
                onClick = { features ->
                    val clickedId = features.firstOrNull()?.id?.toString()
                    // Strip quotes if JsonPrimitive adds them
                    val cleanId = clickedId?.replace("\"", "")

                    val match = clinics.find { it.id == cleanId }
                    if (match != null) {
                        onClinicSelect(match)
                        ClickResult.Consume
                    } else {
                        ClickResult.Pass
                    }
                },
            )

//            CompassButton(
//                cameraState = cameraState,
//                modifier = Modifier.align(Alignment.TopEnd)
//            )
        }
    }
}
