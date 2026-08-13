package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderSpecial
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.NavigationTab
import com.example.ui.theme.PrimaryIndigo
import com.example.ui.theme.SurfaceBorderLight
import com.example.ui.theme.TextPrimaryDark
import com.example.ui.theme.TextSecondaryMuted

@Composable
fun NavigationDrawerContent(
    activeTab: NavigationTab,
    onSelectTab: (NavigationTab) -> Unit,
    onCloseDrawer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .width(280.dp)
            .fillMaxHeight(),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .padding(vertical = 16.dp, horizontal = 16.dp)
        ) {
            // Header: Brand & Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCut,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = "VideoCut Studio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onCloseDrawer) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Menu",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "NAVIGATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )

            // Navigation Items
            DrawerMenuItem(
                label = "Overview",
                icon = Icons.Outlined.Dashboard,
                isSelected = activeTab == NavigationTab.OVERVIEW,
                onClick = {
                    onSelectTab(NavigationTab.OVERVIEW)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = "Cut Video",
                icon = Icons.Outlined.ContentCut,
                isSelected = activeTab == NavigationTab.SINGLE_CUT,
                onClick = {
                    onSelectTab(NavigationTab.SINGLE_CUT)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = "Multi-Part Cut",
                icon = Icons.Outlined.FolderSpecial,
                isSelected = activeTab == NavigationTab.MULTI_CUT,
                onClick = {
                    onSelectTab(NavigationTab.MULTI_CUT)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = "Video Compressor",
                icon = Icons.Outlined.Compress,
                isSelected = activeTab == NavigationTab.COMPRESSOR,
                onClick = {
                    onSelectTab(NavigationTab.COMPRESSOR)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = "Join Video",
                icon = androidx.compose.material.icons.Icons.Default.CallMerge,
                isSelected = activeTab == NavigationTab.JOIN_VIDEO,
                onClick = {
                    onSelectTab(NavigationTab.JOIN_VIDEO)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = "Rotate Video",
                icon = androidx.compose.material.icons.Icons.Outlined.RotateRight,
                isSelected = activeTab == NavigationTab.ROTATE_VIDEO,
                onClick = {
                    onSelectTab(NavigationTab.ROTATE_VIDEO)
                    onCloseDrawer()
                }
            )

            DrawerMenuItem(
                label = "Media Selector",
                icon = androidx.compose.material.icons.Icons.Outlined.Folder,
                isSelected = activeTab == NavigationTab.MEDIA_SELECTOR,
                onClick = {
                    onSelectTab(NavigationTab.MEDIA_SELECTOR)
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "LIBRARY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )

            DrawerMenuItem(
                label = "Saved Videos",
                icon = Icons.Outlined.VideoLibrary,
                isSelected = activeTab == NavigationTab.HISTORY,
                onClick = {
                    onSelectTab(NavigationTab.HISTORY)
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "PREFERENCES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
            )

            DrawerMenuItem(
                label = "Settings",
                icon = Icons.Outlined.Settings,
                isSelected = activeTab == NavigationTab.SETTINGS,
                onClick = {
                    onSelectTab(NavigationTab.SETTINGS)
                    onCloseDrawer()
                }
            )

            Spacer(modifier = Modifier.weight(1f))

            // Footer info
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Column {
                    Text(
                        text = "Fast Stream Engine",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Lossless cutting & hardware compression active",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerMenuItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
    val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = label,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 14.sp,
            color = contentColor
        )
    }
}
