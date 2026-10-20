package com.example.narrator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.R

/** Demo screen. Replace the hard-coded readings with your real sensor state. */
@Composable
fun FarmDashboardScreen(modifier: Modifier = Modifier) {
    val soilMoisture = 42
    val temperature = 31

    NarratorScreen(screenTitle = NarrationText.Res(R.string.farm_title), modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .padding(bottom = 88.dp), // room for the floating narrator control
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Visible title; its narration comes from NarratorScreen(screenTitle = ...).
            Text(
                text = stringResource(R.string.farm_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.semantics { heading() },
            )

            Text(
                text = stringResource(R.string.farm_intro),
                modifier = Modifier.narratable(NarrationText.Res(R.string.farm_intro)),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Decorative: contentDescription = null, not registered, so it is skipped.
                Icon(Icons.Filled.WaterDrop, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.farm_heading_field),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                        .semantics { heading() }
                        .narratable(NarrationText.Res(R.string.farm_heading_field), NarrationKind.HEADING),
                )
            }

            // Meaningful image: description is spoken and shown to TalkBack. The status is also
            // written as text, so it is not conveyed by the icon's color alone.
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusDescription = stringResource(R.string.farm_status_image_cd)
                Icon(
                    imageVector = Icons.Filled.Grass,
                    contentDescription = statusDescription,
                    modifier = Modifier
                        .size(40.dp)
                        .narratable(NarrationText.Res(R.string.farm_status_image_cd), NarrationKind.IMAGE),
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.farm_status_healthy), style = MaterialTheme.typography.titleMedium)
            }

            // Reading + element narration affordance.
            val soilText = NarrationText.Res(R.string.farm_soil_moisture, listOf(soilMoisture))
            ReadingCard(
                icon = { Icon(Icons.Filled.WaterDrop, contentDescription = null) },
                text = stringResource(R.string.farm_soil_moisture, soilMoisture),
                narration = soilText,
            )
            val tempText = NarrationText.Res(R.string.farm_temperature, listOf(temperature))
            ReadingCard(
                icon = { Icon(Icons.Filled.Thermostat, contentDescription = null) },
                text = stringResource(R.string.farm_temperature, temperature),
                narration = tempText,
            )

            Button(
                onClick = { /* sync sensors */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .narratable(NarrationText.Res(R.string.farm_action_sync), NarrationKind.ACTION),
            ) {
                Icon(Icons.Filled.Sync, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.farm_action_sync))
            }
            OutlinedButton(
                onClick = { /* export CSV */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .narratable(NarrationText.Res(R.string.farm_action_export), NarrationKind.ACTION),
            ) {
                Icon(Icons.Filled.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.farm_action_export))
            }
        }
    }
}

@Composable
private fun ReadingCard(
    icon: @Composable () -> Unit,
    text: String,
    narration: NarrationText,
) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(start = 16.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                modifier = Modifier
                    .weight(1f)
                    .narratable(narration),
            )
            // Reads only this card aloud.
            NarrateElementButton(text = narration)
        }
    }
}
