package com.wangucanghai.isposed.ui.credits

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.wangucanghai.isposed.R
import com.wangucanghai.isposed.ui.BaseActivity
import com.wangucanghai.isposed.ui.components.SectionCard
import com.wangucanghai.isposed.ui.components.SectionTitle
import com.wangucanghai.isposed.ui.components.ScaffoldPage
import com.wangucanghai.isposed.ui.components.SpacerH
import com.wangucanghai.isposed.ui.theme.IsposedTheme
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Credits page: upstream projects, design references and contributors.
 *
 * Copyright (C) 2026 万古沧海
 */
class CreditsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IsposedTheme {
                ScaffoldPage(title = stringResource(R.string.credits_title), onBack = { finish() }) {
                    CreditsContent()
                }
            }
        }
    }
}

private data class CreditEntry(
    val name: String,
    val license: String,
    val role: String,
)

@Composable
private fun CreditsContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.credits_desc),
            style = MiuixTheme.textStyles.body2,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )

        SectionTitle(stringResource(R.string.credits_upstream_title))
        Text(
            text = stringResource(R.string.credits_upstream_desc),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            modifier = Modifier.padding(start = 6.dp, end = 6.dp),
        )
        SectionCard {
            CreditRow(
                CreditEntry(
                    name = "LSPosed",
                    license = "GPL-3.0",
                    role = stringResource(R.string.credits_role_lsposed),
                ),
            )
            CreditRow(
                CreditEntry(
                    name = "AxManager / Axeron",
                    license = "Apache-2.0",
                    role = stringResource(R.string.credits_role_axmanager),
                ),
            )
            CreditRow(
                CreditEntry(
                    name = "Miuix",
                    license = "MIT",
                    role = stringResource(R.string.credits_role_miuix),
                ),
            )
            CreditRow(
                CreditEntry(
                    name = "Jetpack Compose",
                    license = "Apache-2.0",
                    role = stringResource(R.string.credits_role_compose),
                ),
            )
        }

        SectionTitle(stringResource(R.string.credits_design_title))
        SectionCard {
            CreditRow(
                CreditEntry(
                    name = stringResource(R.string.credits_entry_miuix),
                    license = "MIT",
                    role = stringResource(R.string.credits_design_desc),
                ),
            )
            CreditRow(
                CreditEntry(
                    name = stringResource(R.string.credits_entry_compose),
                    license = "Apache-2.0",
                    role = stringResource(R.string.credits_design_desc),
                ),
            )
            CreditRow(
                CreditEntry(
                    name = stringResource(R.string.credits_entry_axmanager),
                    license = "Apache-2.0",
                    role = stringResource(R.string.credits_role_axmanager),
                ),
            )
        }

        SectionTitle(stringResource(R.string.credits_contributors_title))
        SectionCard {
            Text(
                text = stringResource(R.string.credits_contributors_desc),
                style = MiuixTheme.textStyles.body2,
            )
        }

        SectionTitle(stringResource(R.string.credits_testers_title))
        SectionCard {
            Text(
                text = stringResource(R.string.credits_testers_desc),
                style = MiuixTheme.textStyles.body2,
            )
        }

        SectionTitle(stringResource(R.string.credits_author_title))
        SectionCard {
            Text(
                text = stringResource(R.string.credits_entry_project),
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Bold,
            )
            SpacerH(4)
            Text(
                text = stringResource(R.string.credits_entry_project_role),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
            SpacerH(8)
            Text(
                text = stringResource(R.string.credits_repo),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.primary,
            )
        }

        SectionCard {
            Text(
                text = stringResource(R.string.credits_thanks_line),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
            )
        }
        SpacerH(24)
    }
}

@Composable
private fun CreditRow(entry: CreditEntry) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Text(
            text = entry.name,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Medium,
        )
        Text(
            text = stringResource(R.string.credits_license_fmt, entry.license),
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.primary,
        )
        Text(
            text = entry.role,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
        )
    }
}
