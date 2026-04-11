package com.example.cs501_final_project.ui

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cs501_final_project.BuildConfig
import com.example.cs501_final_project.ui.components.AppButton
import com.example.cs501_final_project.ui.components.AppCard
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.CircularBounds
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.RectangularBounds
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import com.google.android.libraries.places.api.net.SearchNearbyRequest
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

data class CarePlace(
    val name: String,
    val address: String,
    val latLng: LatLng,
    val distanceMeters: Float
)

@SuppressLint("MissingPermission")
@Composable
fun MapScreen(
    urgency: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }

    if (!Places.isInitialized()) {
        Places.initializeWithNewPlacesApiEnabled(context, BuildConfig.MAPS_API_KEY)
    }

    val placesClient: PlacesClient = remember { Places.createClient(context) }

    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    var errorText by remember { mutableStateOf("") }
    val placesList = remember { mutableStateListOf<CarePlace>() }

    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(42.36, -71.05), 12f)
    }

    val recommendationTitle = when (urgency) {
        "Emergency" -> "Emergency Care Recommended"
        "Urgent Care" -> "Urgent Care Recommended"
        else -> "Primary Care Recommended"
    }

    val recommendationText = when (urgency) {
        "Emergency" ->
            "Your result suggests that you may need emergency medical attention. Please go to the nearest hospital or emergency room as soon as possible."
        "Urgent Care" ->
            "Your symptoms may need treatment today. An urgent care clinic may be a good option nearby."
        else ->
            "Your symptoms seem less severe. A primary care clinic may be the best next step."
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) {
                    val latLng = LatLng(loc.latitude, loc.longitude)
                    userLocation = latLng
                    cameraState.position = CameraPosition.fromLatLngZoom(latLng, 13f)

                    searchPlaces(
                        urgency = urgency,
                        user = latLng,
                        client = placesClient,
                        onResult = { results ->
                            placesList.clear()
                            placesList.addAll(results.take(3))
                        },
                        onError = { msg ->
                            errorText = msg
                        }
                    )
                } else {
                    errorText = "Could not get current location."
                }
            }.addOnFailureListener {
                errorText = "Location request failed."
            }
        } else {
            errorText = "Location permission denied."
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Screen title
        Text(
            text = "Nearby Care",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Recommendation card
        AppCard {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = recommendationTitle,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = recommendationText,
                    modifier = Modifier.padding(top = 8.dp)
                )

                Text(
                    text = "Recommended level: $urgency",
                    modifier = Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Status or error card
        AppCard {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Search Status",
                    style = MaterialTheme.typography.titleMedium
                )

                val statusText = when {
                    errorText.isNotBlank() -> errorText
                    placesList.isEmpty() -> "Looking for nearby care options..."
                    else -> "Nearby places found."
                }

                Text(
                    text = statusText,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        // Map card
        AppCard {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    cameraPositionState = cameraState
                ) {
                    userLocation?.let {
                        Marker(
                            state = MarkerState(position = it),
                            title = "Your Location"
                        )
                    }

                    placesList.forEach { place ->
                        Marker(
                            state = MarkerState(position = place.latLng),
                            title = place.name
                        )
                    }
                }
            }
        }

        // Nearby places card
        AppCard {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Nearby Options",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(placesList) { place ->
                        AppCard {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = place.name,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Text(
                                    text = place.address,
                                    modifier = Modifier.padding(top = 4.dp)
                                )

                                Text(
                                    text = "${"%.2f".format(place.distanceMeters / 1000f)} km away",
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Action buttons card
        AppCard {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AppButton(
                    text = "Back",
                    onClick = onBackClick
                )
            }
        }
    }
}

private fun searchPlaces(
    urgency: String,
    user: LatLng,
    client: PlacesClient,
    onResult: (List<CarePlace>) -> Unit,
    onError: (String) -> Unit
) {
    val fields = listOf(
        Place.Field.DISPLAY_NAME,
        Place.Field.FORMATTED_ADDRESS,
        Place.Field.LOCATION
    )

    if (urgency == "Emergency") {
        val request = SearchNearbyRequest.builder(
            CircularBounds.newInstance(user, 8000.0),
            fields
        )
            .setIncludedTypes(listOf("hospital"))
            .build()

        client.searchNearby(request)
            .addOnSuccessListener { res ->
                val list = res.places.mapNotNull { place ->
                    val loc = place.location ?: return@mapNotNull null
                    CarePlace(
                        name = place.displayName ?: "",
                        address = place.formattedAddress ?: "",
                        latLng = LatLng(loc.latitude, loc.longitude),
                        distanceMeters = distance(user, LatLng(loc.latitude, loc.longitude))
                    )
                }.sortedBy { it.distanceMeters }

                onResult(list)
            }
            .addOnFailureListener {
                onError("Nearby hospital search failed.")
            }
    } else {
        val query = if (urgency == "Urgent Care") {
            "urgent care"
        } else {
            "primary care"
        }

        val request = SearchByTextRequest.builder(query, fields)
            .setLocationRestriction(
                RectangularBounds.newInstance(
                    LatLng(user.latitude - 0.1, user.longitude - 0.1),
                    LatLng(user.latitude + 0.1, user.longitude + 0.1)
                )
            )
            .build()

        client.searchByText(request)
            .addOnSuccessListener { res ->
                val list = res.places.mapNotNull { place ->
                    val loc = place.location ?: return@mapNotNull null
                    CarePlace(
                        name = place.displayName ?: "",
                        address = place.formattedAddress ?: "",
                        latLng = LatLng(loc.latitude, loc.longitude),
                        distanceMeters = distance(user, LatLng(loc.latitude, loc.longitude))
                    )
                }.sortedBy { it.distanceMeters }

                onResult(list)
            }
            .addOnFailureListener {
                onError("Text search failed.")
            }
    }
}

private fun distance(a: LatLng, b: LatLng): Float {
    val result = FloatArray(1)
    Location.distanceBetween(
        a.latitude,
        a.longitude,
        b.latitude,
        b.longitude,
        result
    )
    return result[0]
}