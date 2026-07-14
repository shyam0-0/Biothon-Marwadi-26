package com.medfusion.ai.ui.landing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MedicalServices
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.medfusion.ai.domain.model.UserRole
import com.medfusion.ai.ui.components.BrandLogo
import com.medfusion.ai.ui.components.MedFusionCard
import com.medfusion.ai.ui.components.PrimaryButton
import com.medfusion.ai.ui.components.SecondaryButton
import com.medfusion.ai.ui.theme.Sizes
import com.medfusion.ai.ui.theme.Spacing

/**
 * Entry screen: the user identifies as a patient or a doctor, which routes to the
 * matching login. Keeping the two journeys visibly distinct from the first tap
 * sets the right expectations for each audience.
 */
@Composable
fun RoleSelectionScreen(
    onSelectRole: (UserRole) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Sizes.screenPadding, vertical = Spacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        BrandLogo(size = 72.dp)
        Spacer(Modifier.height(Spacing.lg))
        Text(
            "MedFusion AI",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.xs))
        Text(
            "Your intelligent healthcare companion",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.xxl))

        MedFusionCard(contentPadding = Spacing.lg) {
            Text("Continue as", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(Spacing.md))
            PrimaryButton(
                text = "I'm a Patient",
                leadingIcon = Icons.Outlined.Person,
                onClick = { onSelectRole(UserRole.PATIENT) },
            )
            Spacer(Modifier.height(Spacing.sm))
            SecondaryButton(
                text = "I'm a Doctor",
                leadingIcon = Icons.Outlined.MedicalServices,
                onClick = { onSelectRole(UserRole.DOCTOR) },
            )
        }
    }
}
