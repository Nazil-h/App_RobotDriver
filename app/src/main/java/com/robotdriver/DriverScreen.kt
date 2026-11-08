package com.robotdriver

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun DriverScreen(vm: RobotViewModel) {
    val link by vm.connection.linkState.collectAsState()
    val frame by vm.video.frame.collectAsState()
    val isStreaming by vm.video.isStreaming.collectAsState()

    val isConnected = (link == LinkState.CONNECTED) || isStreaming

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F12))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Bar atas: Pengaturan Pairing, Host IP, Tombol Hubungkan, dan Status
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E24),
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Judul
                Text(
                    text = "Video Stream",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White,
                )

                // Input IP
                OutlinedTextField(
                    value = vm.host,
                    onValueChange = { vm.host = it },
                    label = { Text("IP Kamera / Perangkat", fontSize = 11.sp) },
                    singleLine = true,
                    enabled = (link == LinkState.DISCONNECTED) && !isStreaming,
                    modifier = Modifier.width(220.dp),
                    shape = RoundedCornerShape(8.dp),
                )

                // Tombol Hubungkan / Putuskan
                Button(
                    onClick = {
                        if ((link == LinkState.DISCONNECTED) && !isStreaming) {
                            vm.connect()
                        } else {
                            vm.disconnect()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConnected) Color(0xFFD32F2F) else MaterialTheme.colorScheme.primary,
                    ),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = if (isConnected || (link == LinkState.CONNECTING)) "Putuskan" else "Hubungkan",
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Indikator Status Koneksi
                val (statusColor, statusText) = when {
                    isConnected -> Color(0xFF4CAF50) to "Terhubung"
                    link == LinkState.CONNECTING -> Color(0xFFFFB300) to "Menghubungkan..."
                    else -> Color(0xFF9E9E9E) to "Terputus"
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape),
                    )
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // ID Marker ArUco di bilah atas
                val arucoId = vm.lastArucoId
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .background(
                            if (arucoId != null) Color(0xFF00695C) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                ) {
                    Text(
                        text = if (arucoId != null) "ArUco ID: $arucoId" else "ArUco: -",
                        color = if (arucoId != null) Color(0xFF80CBC4) else Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                if (vm.centerReached) {
                    Text(
                        text = "✓ Titik Tengah",
                        color = Color(0xFF81C784),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .background(Color(0xFF2E7D32).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        // Tampilan Utama Layar Video
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black, RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFF2A2A32), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = frame
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "Aliran video pairing",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )

                // Overlay HUD di atas video: LIVE indicator, Deteksi ArUco ID, Titik Tengah
                Row(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Indikator "LIVE"
                    Row(
                        modifier = Modifier
                            .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.Red, CircleShape),
                        )
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    // Badge Deteksi ArUco ID di atas video
                    val currentArucoId = vm.lastArucoId
                    Row(
                        modifier = Modifier
                            .background(
                                color = if (currentArucoId != null) Color(0xFF00695C).copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.65f),
                                shape = RoundedCornerShape(6.dp),
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (currentArucoId != null) "ArUco ID: $currentArucoId" else "ArUco: -",
                            color = if (currentArucoId != null) Color(0xFF80CBC4) else Color.White.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    if (vm.centerReached) {
                        Row(
                            modifier = Modifier
                                .background(Color(0xFF2E7D32).copy(alpha = 0.9f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "✓ Titik Tengah",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (vm.lastStatus.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.65f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "Hub: ${vm.lastStatus}",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(16.dp),
                ) {
                    Text(
                        text = when {
                            link == LinkState.CONNECTING -> "Menghubungkan ke ${vm.host}..."
                            isConnected -> "Menunggu aliran video dari kamera..."
                            else -> "Belum ada video yang dipairing"
                        },
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = if (!isConnected && (link != LinkState.CONNECTING))
                            "Masukkan IP perangkat di atas lalu tekan Hubungkan"
                        else
                            "Pastikan stream video kamera di perangkat pengirim aktif",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 12.sp,
                    )

                    // Jika ID sudah terdeteksi sebelum frame video tiba
                    if (vm.lastArucoId != null) {
                        Text(
                            text = "Terdeteksi ArUco ID: ${vm.lastArucoId}",
                            color = Color(0xFF80CBC4),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFF00695C).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}
