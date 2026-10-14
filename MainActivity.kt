package com.example.sheguard

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sheguard.ui.theme.SheGuardTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            SheGuardTheme {
                SheGuardScreen()
            }
        }
    }
}

@Composable
fun SheGuardScreen() {

    val context = LocalContext.current

    val preferences = remember {
        context.getSharedPreferences(
            "SheGuardPrefs",
            Context.MODE_PRIVATE
        )
    }

    var contact1 by remember {
        mutableStateOf(preferences.getString("contact1", "") ?: "")
    }

    var contact2 by remember {
        mutableStateOf(preferences.getString("contact2", "") ?: "")
    }

    var contact3 by remember {
        mutableStateOf(preferences.getString("contact3", "") ?: "")
    }

    var showContacts by remember {
        mutableStateOf(false)
    }

    var sendSmsAfterPermission by remember {
        mutableStateOf(false)
    }

    val smsPermissionLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (granted) {
                if (sendSmsAfterPermission) {
                    sendSmsAfterPermission = false

                    sendSosSms(
                        context = context,
                        contacts = listOf(
                            contact1,
                            contact2,
                            contact3
                        )
                    )
                }
            } else {
                sendSmsAfterPermission = false

                Toast.makeText(
                    context,
                    "SMS permission is required for SOS.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    LaunchedEffect(Unit) {
        showContacts = true
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = "SheGuard"
        )

        Spacer(modifier = Modifier.height(25.dp))

        Text(
            text = "Emergency Contacts"
        )

        Spacer(modifier = Modifier.height(15.dp))

        OutlinedTextField(
            value = contact1,
            onValueChange = {
                contact1 = it
            },
            label = {
                Text("Emergency Contact 1")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = contact2,
            onValueChange = {
                contact2 = it
            },
            label = {
                Text("Emergency Contact 2")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = contact3,
            onValueChange = {
                contact3 = it
            },
            label = {
                Text("Emergency Contact 3")
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {

                val c1 = contact1.trim()
                val c2 = contact2.trim()
                val c3 = contact3.trim()

                if (
                    c1.isBlank() &&
                    c2.isBlank() &&
                    c3.isBlank()
                ) {

                    Toast.makeText(
                        context,
                        "Please enter at least one contact.",
                        Toast.LENGTH_SHORT
                    ).show()

                } else {

                    preferences.edit()
                        .putString("contact1", c1)
                        .putString("contact2", c2)
                        .putString("contact3", c3)
                        .apply()

                    Toast.makeText(
                        context,
                        "Emergency contacts saved.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Save Contacts")
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (showContacts) {

            val savedContacts = listOf(
                contact1,
                contact2,
                contact3
            ).filter {
                it.isNotBlank()
            }

            if (savedContacts.isNotEmpty()) {

                Text(
                    text = "Saved Contacts: ${savedContacts.size}"
                )

                Spacer(modifier = Modifier.height(8.dp))

                savedContacts.forEachIndexed { index, number ->

                    Text(
                        text = "${index + 1}. $number"
                    )

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )
                }

                Spacer(
                    modifier = Modifier.height(20.dp)
                )
            }
        }

        Button(
            onClick = {

                val savedContacts = listOf(
                    contact1,
                    contact2,
                    contact3
                ).filter {
                    it.isNotBlank()
                }

                if (savedContacts.isEmpty()) {

                    Toast.makeText(
                        context,
                        "First save an emergency contact.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@Button
                }

                if (
                    androidx.core.content.ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.SEND_SMS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {

                    sendSmsAfterPermission = true

                    smsPermissionLauncher.launch(
                        Manifest.permission.SEND_SMS
                    )

                } else {

                    sendSosSms(
                        context = context,
                        contacts = savedContacts
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
        ) {
            Text("🆘 SOS")
        }
    }
}


/* ---------------------------------------------------------
   SOS SMS
   --------------------------------------------------------- */

fun sendSosSms(
    context: Context,
    contacts: List<String>
) {

    if (contacts.isEmpty()) {

        Toast.makeText(
            context,
            "No emergency contact saved.",
            Toast.LENGTH_SHORT
        ).show()

        return
    }

    if (
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) != PackageManager.PERMISSION_GRANTED
    ) {

        Toast.makeText(
            context,
            "SMS permission is required.",
            Toast.LENGTH_LONG
        ).show()

        return
    }

    val location = getPhoneLocation(context)

    val locationMessage = if (location != null) {

        "My location:\n" +
                "https://maps.google.com/?q=" +
                "${location.latitude},${location.longitude}"

    } else {

        "My location is currently unavailable."
    }

    val message =
        "SOS! I need help.\n\n" +
                "SheGuard emergency alert.\n\n" +
                locationMessage

    try {

        val subscriptionId =
            SubscriptionManager.getDefaultSmsSubscriptionId()

        @Suppress("DEPRECATION")
        val smsManager = if (
            subscriptionId !=
            SubscriptionManager.INVALID_SUBSCRIPTION_ID
        ) {

            SmsManager.getSmsManagerForSubscriptionId(
                subscriptionId
            )

        } else {

            SmsManager.getDefault()
        }

        var sentCount = 0

        contacts.forEach { number ->

            val destination =
                cleanIndianNumber(number)

            if (destination.isNotBlank()) {

                val parts =
                    smsManager.divideMessage(message)

                if (parts.size > 1) {

                    @Suppress("DEPRECATION")
                    smsManager.sendMultipartTextMessage(
                        destination,
                        null,
                        parts,
                        null,
                        null
                    )

                } else {

                    @Suppress("DEPRECATION")
                    smsManager.sendTextMessage(
                        destination,
                        null,
                        message,
                        null,
                        null
                    )
                }

                sentCount++
            }
        }

        if (sentCount > 0) {

            Toast.makeText(
                context,
                "SOS SMS sent to $sentCount contact(s).",
                Toast.LENGTH_LONG
            ).show()

        } else {

            Toast.makeText(
                context,
                "No valid phone number found.",
                Toast.LENGTH_LONG
            ).show()
        }

    } catch (e: SecurityException) {

        Toast.makeText(
            context,
            "SMS permission is required.",
            Toast.LENGTH_LONG
        ).show()

    } catch (e: Exception) {

        Toast.makeText(
            context,
            "SMS failed: ${e.message}",
            Toast.LENGTH_LONG
        ).show()
    }
}


/* ---------------------------------------------------------
   PHONE NUMBER
   --------------------------------------------------------- */

fun cleanIndianNumber(
    number: String
): String {

    val original = number.trim()

    val digits =
        original.filter {
            it.isDigit()
        }

    return when {

        digits.length == 10 -> {
            "+91$digits"
        }

        digits.length == 12 &&
                digits.startsWith("91") -> {
            "+$digits"
        }

        original.startsWith("+") -> {
            original
        }

        else -> {
            original
        }
    }
}


/* ---------------------------------------------------------
   LOCATION
   --------------------------------------------------------- */

fun getPhoneLocation(
    context: Context
): Location? {

    val finePermission =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val coarsePermission =
        androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    if (!finePermission && !coarsePermission) {
        return null
    }

    return try {

        val locationManager =
            context.getSystemService(
                Context.LOCATION_SERVICE
            ) as LocationManager

        var bestLocation: Location? = null

        val providers = listOf(
            LocationManager.GPS_PROVIDER,
            LocationManager.NETWORK_PROVIDER
        )

        for (provider in providers) {

            try {

                if (
                    !locationManager.isProviderEnabled(
                        provider
                    )
                ) {
                    continue
                }

                val location =
                    locationManager.getLastKnownLocation(
                        provider
                    )

                if (location != null) {

                    if (
                        bestLocation == null ||
                        location.accuracy <
                        bestLocation.accuracy
                    ) {

                        bestLocation = location
                    }
                }

            } catch (_: SecurityException) {
                // Ignore this provider
            }
        }

        bestLocation

    } catch (_: Exception) {

        null
    }
}