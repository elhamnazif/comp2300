package com.group8.comp2300.feature.auth.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import com.group8.comp2300.core.ui.components.AppTopBar
import comp2300.i18n.generated.resources.*
import org.jetbrains.compose.resources.stringResource

fun sanitizeVerificationCode(value: String, maxLength: Int = 6): String =
    value.filter(Char::isDigit).take(maxLength)

@Composable
fun AuthFormScaffold(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bannerContent: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()

    Scaffold(
        modifier = modifier,
        topBar = {
            AppTopBar(
                title = {},
                onBackClick = onBack,
                backContentDescription = stringResource(Res.string.auth_back_desc),
            )
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .imePadding(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )

            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = bannerContent,
            )
        }
    }
}

@Composable
fun AuthHeroSection(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    emphasisText: String? = null,
    supportingText: String? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxWidth(),
    ) {
        Card(
            shape = RoundedCornerShape(50),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
            modifier = Modifier.size(80.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
            )
        }

        Spacer(Modifier.size(24.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.size(8.dp))

        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
        )

        emphasisText?.let {
            Spacer(Modifier.size(8.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
        }

        supportingText?.let {
            Spacer(Modifier.size(16.dp))
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun AuthBanner(
    message: String?,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
    contentColor: Color = MaterialTheme.colorScheme.onErrorContainer,
    borderColor: Color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
) {
    AnimatedVisibility(
        visible = message != null,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        message?.let {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                border = BorderStroke(1.dp, borderColor),
                modifier = modifier.fillMaxWidth().padding(bottom = 16.dp),
            ) {
                Text(
                    text = it,
                    color = contentColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }
}

@Composable
fun AuthLoadingButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(50),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = contentColor,
            )
        } else {
            Text(
                text = text,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun VerificationCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.size(8.dp))

        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(sanitizeVerificationCode(it)) },
            placeholder = { Text(placeholder) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            ),
        )
    }
}
