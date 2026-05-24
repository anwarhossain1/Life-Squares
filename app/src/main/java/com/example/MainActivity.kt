package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AppDatabase
import com.example.data.CountryData
import com.example.data.CountryInfo
import com.example.data.Profile
import com.example.data.ProfileRepository
import com.example.ui.MainViewModel
import com.example.ui.MainViewModelFactory
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Get database and repo
        val database = AppDatabase.getDatabase(this)
        val repository = ProfileRepository(database.profileDao())

        setContent {
            MyApplicationTheme {
                val viewModel: MainViewModel = viewModel(
                    factory = MainViewModelFactory(repository)
                )

                val profileState by viewModel.profile.collectAsStateWithLifecycle()
                val selectedDayIndex by viewModel.selectedDayIndex.collectAsStateWithLifecycle()
                val gridMode by viewModel.gridMode.collectAsStateWithLifecycle()

                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (profileState == null) {
                            OnboardingScreen(
                                onSaveProfile = { name, country, birthDate, expectancy ->
                                    viewModel.saveProfile(name, country, birthDate, expectancy)
                                }
                            )
                        } else {
                            DashboardScreen(
                                profile = profileState!!,
                                selectedDayIndex = selectedDayIndex,
                                gridMode = gridMode,
                                onDaySelected = { viewModel.selectDay(it) },
                                onChangeMode = { viewModel.setGridMode(it) },
                                onDeleteProfile = { viewModel.deleteProfile() },
                                onEditProfile = { name, country, birthDate, expectancy ->
                                    viewModel.saveProfile(name, country, birthDate, expectancy)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    onSaveProfile: (String, String, String, Double) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var selectedCountry by remember { mutableStateOf(CountryData.countries.first { it.name == "Global Average" }) }
    var searchCountryQuery by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Manual Customization toggle
    var useCustomExpectancy by remember { mutableStateOf(false) }
    var customExpectancyValue by remember { mutableStateOf(80.0f) }

    // Birth Date selections
    val currentYear = LocalDate.now().year
    var birthYear by remember { mutableStateOf(2000) }
    var birthMonth by remember { mutableStateOf(1) }
    var birthDay by remember { mutableStateOf(1) }

    // Form validation
    var nameError by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    val filteredCountries = remember(searchCountryQuery) {
        if (searchCountryQuery.isEmpty()) {
            CountryData.countries
        } else {
            CountryData.countries.filter {
                it.name.contains(searchCountryQuery, ignoreCase = true)
            }
        }
    }

    // Dynamic days calculations helper to avoid February 30, April 31 etc.
    val daysInMonth = remember(birthYear, birthMonth) {
        try {
            java.time.YearMonth.of(birthYear, birthMonth).lengthOfMonth()
        } catch (e: Exception) {
            31
        }
    }

    // Coerce birthDay if its value runs beyond the allowable day length
    LaunchedEffect(daysInMonth) {
        if (birthDay > daysInMonth) {
            birthDay = daysInMonth
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmersiveBackground)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Visual Greeting Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            ImmersivePrimary,
                            ImmersivePrimaryContainer
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.DateRange,
                contentDescription = "Calendar",
                tint = ImmersiveOnPrimary,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Welcome to Life Squares",
            color = ImmersiveTextWhite,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Text(
            text = "Let's capture your demographics to chart your journey through time.",
            style = MaterialTheme.typography.bodyLarge,
            color = ImmersiveTextMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Step 1: User's Name
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
            border = BorderStroke(1.dp, ImmersiveOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "What is your name?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ImmersivePrimary
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) nameError = false
                    },
                    placeholder = { Text("Your Name", color = ImmersiveTextMuted) },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = ImmersiveTextMuted) },
                    singleLine = true,
                    isError = nameError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ImmersivePrimary,
                        unfocusedBorderColor = ImmersiveOutline,
                        focusedTextColor = ImmersiveTextWhite,
                        unfocusedTextColor = ImmersiveTextBody,
                        focusedContainerColor = ImmersiveSurfaceVariant,
                        unfocusedContainerColor = ImmersiveSurfaceVariant
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("name_input")
                )
                if (nameError) {
                    Text(
                        text = "Name cannot be empty",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 2: Location Selector
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
            border = BorderStroke(1.dp, ImmersiveOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Where are you living?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ImmersivePrimary
                )
                Text(
                    text = "We use location to estimate realistic average regional life expectancies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ImmersiveTextMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { dropdownExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .testTag("country_picker_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ImmersiveOutline),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = ImmersiveTextBody
                        )
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = selectedCountry.flagEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedCountry.name,
                                    color = ImmersiveTextWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(Icons.Default.Place, contentDescription = "Dropdown", tint = ImmersiveTextMuted)
                        }
                    }

                    if (dropdownExpanded) {
                        AlertDialog(
                            onDismissRequest = { dropdownExpanded = false },
                            confirmButton = {
                                TextButton(onClick = { dropdownExpanded = false }) {
                                    Text("Done", color = ImmersivePrimary)
                                }
                            },
                            title = { Text("Select Country of Residence", color = ImmersiveTextWhite) },
                            text = {
                                Column(modifier = Modifier.fillMaxHeight(0.6f)) {
                                    OutlinedTextField(
                                        value = searchCountryQuery,
                                        onValueChange = { searchCountryQuery = it },
                                        placeholder = { Text("Search Country...", color = ImmersiveTextMuted) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = ImmersiveTextMuted) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ImmersivePrimary,
                                            unfocusedBorderColor = ImmersiveOutline,
                                            focusedTextColor = ImmersiveTextWhite,
                                            unfocusedTextColor = ImmersiveTextBody,
                                            focusedContainerColor = ImmersiveSurface,
                                            unfocusedContainerColor = ImmersiveSurface
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    )

                                    Box(modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                            filteredCountries.forEach { country ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedCountry = country
                                                            if (!useCustomExpectancy) {
                                                                customExpectancyValue = country.lifeExpectancy.toFloat()
                                                            }
                                                            dropdownExpanded = false
                                                        }
                                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = country.flagEmoji, fontSize = 22.sp)
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = country.name,
                                                        color = ImmersiveTextBody,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = "~${country.lifeExpectancy.toInt()} yrs",
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = ImmersiveTextMuted
                                                    )
                                                }
                                                HorizontalDivider(color = ImmersiveOutline.copy(alpha = 0.3f))
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 3: Birthday wheels
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
            border = BorderStroke(1.dp, ImmersiveOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "When were you born?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ImmersivePrimary
                )
                Text(
                    text = "We use your exact date of birth to track lived days correctly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = ImmersiveTextMuted,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Month Picker
                    var monthMenuExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1.3f)) {
                        OutlinedButton(
                            onClick = { monthMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, ImmersiveOutline),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersiveTextWhite)
                        ) {
                            Text(
                                text = java.time.Month.of(birthMonth).getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        DropdownMenu(
                            expanded = monthMenuExpanded,
                            onDismissRequest = { monthMenuExpanded = false }
                        ) {
                            (1..12).forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())) },
                                    onClick = {
                                        birthMonth = m
                                        monthMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Day Picker
                    var dayMenuExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(0.9f)) {
                        OutlinedButton(
                            onClick = { dayMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, ImmersiveOutline),
                            contentPadding = PaddingValues(horizontal = 8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersiveTextWhite)
                        ) {
                            Text(text = birthDay.toString())
                        }
                        DropdownMenu(
                            expanded = dayMenuExpanded,
                            onDismissRequest = { dayMenuExpanded = false }
                        ) {
                            (1..daysInMonth).forEach { d ->
                                DropdownMenuItem(
                                    text = { Text(d.toString()) },
                                    onClick = {
                                        birthDay = d
                                        dayMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Year Picker
                    var yearMenuExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { yearMenuExpanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            border = BorderStroke(1.dp, ImmersiveOutline),
                            contentPadding = PaddingValues(horizontal = 4.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersiveTextWhite)
                        ) {
                            Text(text = birthYear.toString())
                        }
                        DropdownMenu(
                            expanded = yearMenuExpanded,
                            onDismissRequest = { yearMenuExpanded = false }
                        ) {
                            val availableYears = (1920..currentYear).reversed().toList()
                            availableYears.forEach { yr ->
                                DropdownMenuItem(
                                    text = { Text(yr.toString()) },
                                    onClick = {
                                        birthYear = yr
                                        yearMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Step 4: Expectancy customization
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
            border = BorderStroke(1.dp, ImmersiveOutline),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Life Expectancy Mode",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ImmersivePrimary
                        )
                        Text(
                            text = if (useCustomExpectancy) "Manual Settings Enabled" else "Using country average: ~${selectedCountry.lifeExpectancy.toInt()} yrs",
                            style = MaterialTheme.typography.bodySmall,
                            color = ImmersiveTextMuted
                        )
                    }
                    Switch(
                        checked = useCustomExpectancy,
                        onCheckedChange = {
                            useCustomExpectancy = it
                            if (!it) {
                                customExpectancyValue = selectedCountry.lifeExpectancy.toFloat()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = ImmersivePrimary,
                            checkedTrackColor = ImmersivePrimaryContainer
                        )
                    )
                }

                if (useCustomExpectancy) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Expectancy: ${customExpectancyValue.toInt()} Years",
                            style = MaterialTheme.typography.bodyLarge,
                            color = ImmersiveTextWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = customExpectancyValue,
                        onValueChange = { customExpectancyValue = it },
                        valueRange = 40f..115f,
                        steps = 75,
                        colors = SliderDefaults.colors(
                            thumbColor = ImmersivePrimary,
                            activeTrackColor = ImmersivePrimary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                if (name.isBlank()) {
                    nameError = true
                    focusManager.clearFocus()
                } else {
                    val finalExpectancy = if (useCustomExpectancy) customExpectancyValue.toDouble() else selectedCountry.lifeExpectancy
                    val formattedMonth = String.format("%02d", birthMonth)
                    val formattedDay = String.format("%02d", birthDay)
                    val finalBirthDateStr = "$birthYear-$formattedMonth-$formattedDay"
                    onSaveProfile(name, selectedCountry.name, finalBirthDateStr, finalExpectancy)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("submit_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = ImmersivePrimary,
                contentColor = ImmersiveOnPrimary
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            Text(
                text = "Chart My Canvas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    profile: Profile,
    selectedDayIndex: Long?,
    gridMode: String,
    onDaySelected: (Long?) -> Unit,
    onChangeMode: (String) -> Unit,
    onDeleteProfile: () -> Unit,
    onEditProfile: (String, String, String, Double) -> Unit
) {
    val birthDate = remember(profile.birthDate) {
        try {
            LocalDate.parse(profile.birthDate, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            LocalDate.now().minusYears(25)
        }
    }
    val today = LocalDate.now()

    val totalDaysExpected = remember(profile.lifeExpectancy) {
        (profile.lifeExpectancy * 365.2425).toLong()
    }

    val daysLived = remember(birthDate, today) {
        maxOf(0L, ChronoUnit.DAYS.between(birthDate, today))
    }

    val daysRemaining = remember(totalDaysExpected, daysLived) {
        maxOf(0L, totalDaysExpected - daysLived)
    }

    val percentageProgress = remember(totalDaysExpected, daysLived) {
        if (totalDaysExpected > 0) {
            (daysLived.toDouble() / totalDaysExpected.toDouble() * 100.0).coerceIn(0.0, 100.0)
        } else {
            100.0
        }
    }

    val finalAgeYears = remember(birthDate, today) {
        ChronoUnit.YEARS.between(birthDate, today).toInt()
    }

    val ageMonths = remember(birthDate, today) {
        try {
            java.time.Period.between(birthDate, today).months
        } catch (e: Exception) {
            0
        }
    }

    val selectedCountryInfo = remember(profile.country) {
        CountryData.getByCountryName(profile.country)
    }

    // Modal state for editing profile
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ImmersiveBackground)
    ) {
        // Sticky/Scrollable Body
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Header (Immersive Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CURRENT CHAPTER",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = ImmersivePrimary,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = ImmersiveTextWhite
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Location",
                            tint = ImmersiveTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "${profile.country} • Regional expectancy: ~${profile.lifeExpectancy.toInt()} yrs",
                            style = MaterialTheme.typography.bodySmall,
                            color = ImmersiveTextMuted
                        )
                    }
                }

                // Profile Avatar Button in immersive palette
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(ImmersivePrimaryContainer)
                        .border(1.dp, ImmersivePrimary.copy(alpha = 0.2f), CircleShape)
                        .clickable { showEditDialog = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Edit Profile",
                        tint = ImmersiveTextWhite,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Longevity Announcement Card
            if (daysLived >= totalDaysExpected) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = ImmersivePrimaryContainer),
                    border = BorderStroke(1.dp, ImmersiveOutline),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Golden Years Star",
                            tint = ImmersivePrimary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Beautiful Golden Longevity! 🌟",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = ImmersiveTextWhite
                            )
                            Text(
                                text = "You are living in bonus years (~${String.format("%,d", daysLived - totalDaysExpected)} days beyond statistical average). Enjoy every single miracle day!",
                                style = MaterialTheme.typography.bodySmall,
                                color = ImmersiveTextBody
                            )
                        }
                    }
                }
            }

            // Two-column statistical KPI metrics cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                    border = BorderStroke(1.dp, ImmersiveOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "LIVED",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ImmersivePrimary,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%,d", daysLived),
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                            fontWeight = FontWeight.Black,
                            color = ImmersiveTextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.1f%% recorded", percentageProgress),
                            style = MaterialTheme.typography.bodySmall,
                            color = ImmersiveTextMuted,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                    border = BorderStroke(1.dp, ImmersiveOutline)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "HORIZON",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveTextMuted,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%,d", daysRemaining),
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp),
                            fontWeight = FontWeight.Black,
                            color = ImmersiveTextWhite
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "estimated days",
                            style = MaterialTheme.typography.bodySmall,
                            color = ImmersiveTextMuted,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            // Life Grid Title + Age tracking section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "The Life Grid",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = ImmersiveTextBody
                    )
                    Box(
                        modifier = Modifier
                            .background(ImmersivePrimaryContainer, RoundedCornerShape(100.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = String.format("%.1f%% Complete", percentageProgress),
                            style = MaterialTheme.typography.labelSmall,
                            color = ImmersivePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Age: ${finalAgeYears}y ${ageMonths}m",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = ImmersiveTextMuted
                )
            }

            // Inner visual grid box matching deep #141218 nested background
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = ImmersiveSurfaceVariant),
                border = BorderStroke(1.dp, ImmersiveOutline.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title and toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "LIFESPAN SCALE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = ImmersivePrimary,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = when (gridMode) {
                                    "days_100" -> "Row = 100 days"
                                    "days_365" -> "Row = 365 days (1 year)"
                                    else -> "Row = 52 weeks (1 year)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = ImmersiveTextMuted
                            )
                        }

                        // Mini segmented trigger
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("days_100" to "100", "days_365" to "365", "weeks" to "Wks").forEach { (mode, label) ->
                                val isSel = gridMode == mode
                                FilledTonalIconButton(
                                    onClick = { onChangeMode(mode) },
                                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                                        containerColor = if (isSel) ImmersivePrimary else ImmersiveSurface,
                                        contentColor = if (isSel) ImmersiveOnPrimary else ImmersiveTextMuted
                                    ),
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Text(
                                        label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Scrollable Canvas container styled like high-performance inner block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 340.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(ImmersiveSurface)
                            .border(1.dp, ImmersiveOutline.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .verticalScroll(rememberScrollState())
                            .padding(12.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        RenderHugeGridCanvas(
                            gridMode = gridMode,
                            totalDaysExpected = totalDaysExpected,
                            daysLived = daysLived,
                            expectedLifeYears = profile.lifeExpectancy,
                            selectedDayIndex = selectedDayIndex,
                            onDaySelected = onDaySelected
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Legends Section matching Tailwind Spec
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(ImmersivePrimary, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Passed", color = ImmersiveTextMuted, style = MaterialTheme.typography.bodySmall)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .border(1.dp, ImmersiveTextMuted.copy(alpha = 0.4f), RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Future", color = ImmersiveTextMuted, style = MaterialTheme.typography.bodySmall)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(TodayHighlightGold, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Today", color = ImmersiveTextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Day / Week Inspector (If selected)
            AnimatedVisibility(
                visible = selectedDayIndex != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val idx = selectedDayIndex ?: 0L
                val dateOfSelectedDay = remember(idx, birthDate) {
                    if (gridMode == "weeks") {
                        birthDate.plusWeeks(idx)
                    } else {
                        birthDate.plusDays(idx)
                    }
                }
                val formattedSelectedDate = remember(dateOfSelectedDay) {
                    dateOfSelectedDay.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
                }
                val ageAtSelectedDay = remember(birthDate, dateOfSelectedDay) {
                    if (gridMode == "weeks") {
                        val ageApprox = idx.toDouble() / 52.1775
                        String.format("%.1f", ageApprox)
                    } else {
                        val ageApprox = idx.toDouble() / 365.2425
                        String.format("%.1f", ageApprox)
                    }
                }
                val isLived = if (gridMode == "weeks") {
                    idx * 7 < daysLived
                } else {
                    idx < daysLived
                }
                val isCurrentToday = if (gridMode == "weeks") {
                    (idx * 7 <= daysLived) && ((idx + 1) * 7 > daysLived)
                } else {
                    idx == daysLived
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = ImmersiveSurface),
                    border = BorderStroke(1.dp, ImmersivePrimary.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (gridMode == "weeks") "WEEK METRIC INSPECTOR" else "DAY METRIC INSPECTOR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = ImmersivePrimary,
                                letterSpacing = 1.2.sp
                            )
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Close",
                                tint = ImmersivePrimary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable { onDaySelected(null) }
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (gridMode == "weeks") "Week $idx of your life" else "Day $idx of your life",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ImmersiveTextWhite
                        )
                        Text(
                            text = "Estimated Date: $formattedSelectedDate",
                            style = MaterialTheme.typography.bodyMedium,
                            color = ImmersiveTextBody
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Age approx: $ageAtSelectedDay years",
                            style = MaterialTheme.typography.bodySmall,
                            color = ImmersiveTextMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    text = when {
                                        isCurrentToday -> if (gridMode == "weeks") "Current Active Week" else "Today's Active Square ⚡"
                                        isLived -> "Lived Historical Period 🎉"
                                        else -> "Potential Future Period 🕊️"
                                    },
                                    color = ImmersiveTextWhite
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = when {
                                        isCurrentToday -> TodayHighlightGold.copy(alpha = 0.3f)
                                        isLived -> ImmersivePrimaryContainer.copy(alpha = 0.6f)
                                        else -> Color.Transparent
                                }
                            ),
                            border = AssistChipDefaults.assistChipBorder(borderColor = ImmersiveOutline, enabled = true)
                        )
                    }
                }
            }

            // Reset Profile Trigger
            OutlinedButton(
                onClick = onDeleteProfile,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .testTag("delete_profile_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Profile Data", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            }
        }

        // Sticky premium visual Navigation Bar directly mapping to HTML tab indicators
        ImmersiveBottomBar(
            currentMode = gridMode,
            onChangeMode = onChangeMode,
            onSettingsClick = { showEditDialog = true }
        )
    }

    if (showEditDialog) {
        EditProfileDialog(
            profile = profile,
            onDismiss = { showEditDialog = false },
            onSave = { updatedName, updatedCountry, updatedBirthDate, updatedExpectancy ->
                onEditProfile(updatedName, updatedCountry, updatedBirthDate, updatedExpectancy)
                showEditDialog = false
            }
        )
    }
}

@Composable
fun ImmersiveBottomBar(
    currentMode: String,
    onChangeMode: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(ImmersiveSurface)
            .border(BorderStroke(1.dp, ImmersiveOutline.copy(alpha = 0.5f)))
            .navigationBarsPadding()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab 1: Overview
        BottomTabItem(
            label = "Overview",
            icon = Icons.Default.Star,
            isActive = currentMode == "weeks",
            onClick = { onChangeMode("weeks") }
        )

        // Tab 2: Stats (100)
        BottomTabItem(
            label = "Stats 100",
            icon = Icons.Default.Info,
            isActive = currentMode == "days_100",
            onClick = { onChangeMode("days_100") }
        )

        // Tab 3: Journal (365)
        BottomTabItem(
            label = "Journal 365",
            icon = Icons.Default.DateRange,
            isActive = currentMode == "days_365",
            onClick = { onChangeMode("days_365") }
        )

        // Tab 4: Settings
        BottomTabItem(
            label = "Settings",
            icon = Icons.Default.Person,
            isActive = false,
            onClick = onSettingsClick
        )
    }
}

@Composable
fun RowScope.BottomTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val backgroundPill = if (isActive) ImmersivePrimaryContainer else Color.Transparent
        val iconColor = if (isActive) ImmersiveTextWhite else ImmersiveTextMuted
        val textColor = if (isActive) ImmersiveTextWhite else ImmersiveTextMuted

        Box(
            modifier = Modifier
                .width(56.dp)
                .height(32.dp)
                .background(backgroundPill, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

@Composable
fun RenderHugeGridCanvas(
    gridMode: String,
    totalDaysExpected: Long,
    daysLived: Long,
    expectedLifeYears: Double,
    selectedDayIndex: Long?,
    onDaySelected: (Long?) -> Unit
) {
    val localDensity = LocalDensity.current

    val primaryColor = MaterialTheme.colorScheme.primary
    val outlineColor = ImmersiveTextMuted.copy(alpha = 0.4f)
    val selectionColor = MaterialTheme.colorScheme.error // Glowing magenta/pink
    val todayColor = TodayHighlightGold

    // Pulse transition for the current day
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val containerWidthDp = maxWidth
        val containerWidthPx = with(localDensity) { containerWidthDp.toPx() }

        val columns = when (gridMode) {
            "days_100" -> 100
            "days_365" -> 365
            "weeks" -> 52
            else -> 100
        }

        val totalSquaresCount = remember(gridMode, expectedLifeYears, totalDaysExpected) {
            when (gridMode) {
                "weeks" -> (expectedLifeYears * 52).toLong()
                "days_365" -> (expectedLifeYears * 365).toLong()
                else -> totalDaysExpected
            }
        }

        val spacerSizePx = 1.5f // Clear spacing between grids

        // Calculate size of square dynamically
        val squareSizePx = maxOf(0.4f, (containerWidthPx - (columns - 1) * spacerSizePx) / columns)

        val rowsCount = ((totalSquaresCount + columns - 1) / columns).toInt()
        val calculatedCanvasHeightDp = with(localDensity) {
            (rowsCount * (squareSizePx + spacerSizePx)).toDp()
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(calculatedCanvasHeightDp)
                .pointerInput(gridMode, totalSquaresCount, columns, squareSizePx, spacerSizePx) {
                    detectTapGestures { offset ->
                        val col = (offset.x / (squareSizePx + spacerSizePx)).toInt()
                        val row = (offset.y / (squareSizePx + spacerSizePx)).toInt()
                        if (col in 0 until columns) {
                            val idx = (row * columns + col).toLong()
                            if (idx in 0L until totalSquaresCount) {
                                onDaySelected(idx)
                            }
                        }
                    }
                }
        ) {
            val livedCounterLimit = if (gridMode == "weeks") {
                daysLived / 7
            } else {
                daysLived
            }

            // High aesthetic rounded corners proportional to size
            val rawRadius = maxOf(0.4f, squareSizePx * 0.18f)
            val cornerRadius = androidx.compose.ui.geometry.CornerRadius(rawRadius, rawRadius)

            for (i in 0L until totalSquaresCount) {
                val col = (i % columns).toInt()
                val row = (i / columns).toInt()

                val xPos = col * (squareSizePx + spacerSizePx)
                val yPos = row * (squareSizePx + spacerSizePx)

                val isLived = i < livedCounterLimit
                val isToday = i == livedCounterLimit
                val isSelected = i == selectedDayIndex

                when {
                    isToday -> {
                        // Pulsing Today square
                        drawRoundRect(
                            color = todayColor.copy(alpha = pulseAlpha),
                            topLeft = Offset(xPos, yPos),
                            size = Size(squareSizePx, squareSizePx),
                            cornerRadius = cornerRadius
                        )
                        if (isSelected) {
                            drawRoundRect(
                                color = selectionColor,
                                topLeft = Offset(xPos, yPos),
                                size = Size(squareSizePx, squareSizePx),
                                style = Stroke(width = maxOf(1f, squareSizePx * 0.25f)),
                                cornerRadius = cornerRadius
                            )
                        }
                    }
                    isSelected -> {
                        drawRoundRect(
                            color = selectionColor,
                            topLeft = Offset(xPos, yPos),
                            size = Size(squareSizePx, squareSizePx),
                            cornerRadius = cornerRadius
                        )
                    }
                    isLived -> {
                        drawRoundRect(
                            color = primaryColor,
                            topLeft = Offset(xPos, yPos),
                            size = Size(squareSizePx, squareSizePx),
                            cornerRadius = cornerRadius
                        )
                    }
                    else -> {
                        // Future outline days
                        drawRoundRect(
                            color = outlineColor,
                            topLeft = Offset(xPos + 0.3f, yPos + 0.3f),
                            size = Size(squareSizePx - 0.6f, squareSizePx - 0.6f),
                            style = Stroke(width = maxOf(0.4f, squareSizePx * 0.12f)),
                            cornerRadius = cornerRadius
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    profile: Profile,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double) -> Unit
) {
    var editName by remember { mutableStateOf(profile.name) }
    var selectedCountry by remember { mutableStateOf(CountryData.getByCountryName(profile.country)) }
    var searchCountryQuery by remember { mutableStateOf("") }
    var countryDropdownExpanded by remember { mutableStateOf(false) }

    var useCustomExpectancy by remember { mutableStateOf(profile.lifeExpectancy != selectedCountry.lifeExpectancy) }
    var customExpectancyValue by remember { mutableStateOf(profile.lifeExpectancy.toFloat()) }

    var finalBirthDate by remember { mutableStateOf(profile.birthDate) }

    val currentYear = LocalDate.now().year
    val parsedBirth = remember(profile.birthDate) {
        try {
            LocalDate.parse(profile.birthDate, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            LocalDate.now().minusYears(25)
        }
    }

    var birthYear by remember { mutableStateOf(parsedBirth.year) }
    var birthMonth by remember { mutableStateOf(parsedBirth.monthValue) }
    var birthDay by remember { mutableStateOf(parsedBirth.dayOfMonth) }

    val daysInMonth = remember(birthYear, birthMonth) {
        try {
            java.time.YearMonth.of(birthYear, birthMonth).lengthOfMonth()
        } catch (e: Exception) {
            31
        }
    }

    LaunchedEffect(daysInMonth) {
        if (birthDay > daysInMonth) {
            birthDay = daysInMonth
        }
    }

    val filteredCountries = remember(searchCountryQuery) {
        if (searchCountryQuery.isEmpty()) {
            CountryData.countries
        } else {
            CountryData.countries.filter {
                it.name.contains(searchCountryQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(ImmersiveSurface, RoundedCornerShape(28.dp))
            .border(1.dp, ImmersiveOutline, RoundedCornerShape(28.dp)),
        title = {
            Text(
                "Edit Demographics",
                fontWeight = FontWeight.Bold,
                color = ImmersiveTextWhite,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    if (editName.isNotBlank()) {
                        val formattedMonth = String.format("%02d", birthMonth)
                        val formattedDay = String.format("%02d", birthDay)
                        val updatedBirthDate = "$birthYear-$formattedMonth-$formattedDay"
                        val updatedExpVal = if (useCustomExpectancy) customExpectancyValue.toDouble() else selectedCountry.lifeExpectancy
                        onSave(editName, selectedCountry.name, updatedBirthDate, updatedExpVal)
                    }
                },
                enabled = editName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ImmersivePrimary,
                    contentColor = ImmersiveOnPrimary
                ),
                shape = RoundedCornerShape(100.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = ImmersivePrimary)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Name", color = ImmersiveTextMuted) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Name", tint = ImmersiveTextMuted) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ImmersivePrimary,
                        unfocusedBorderColor = ImmersiveOutline,
                        focusedTextColor = ImmersiveTextWhite,
                        unfocusedTextColor = ImmersiveTextBody,
                        focusedContainerColor = ImmersiveSurfaceVariant,
                        unfocusedContainerColor = ImmersiveSurfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Country Selector
                Column {
                    Text(
                        text = "Residence Country",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ImmersivePrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { countryDropdownExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ImmersiveOutline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersiveTextWhite)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(selectedCountry.flagEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(selectedCountry.name, overflow = TextOverflow.Ellipsis)
                            }
                            Icon(Icons.Default.Place, contentDescription = "Place", tint = ImmersiveTextMuted)
                        }
                    }

                    if (countryDropdownExpanded) {
                        AlertDialog(
                            onDismissRequest = { countryDropdownExpanded = false },
                            confirmButton = {
                                TextButton(onClick = { countryDropdownExpanded = false }) {
                                    Text("Done", color = ImmersivePrimary)
                                }
                            },
                            title = { Text("Select Residence Country", color = ImmersiveTextWhite) },
                            text = {
                                Column(modifier = Modifier.fillMaxHeight(0.5f)) {
                                    OutlinedTextField(
                                        value = searchCountryQuery,
                                        onValueChange = { searchCountryQuery = it },
                                        placeholder = { Text("Search Country...", color = ImmersiveTextMuted) },
                                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = ImmersiveTextMuted) },
                                        singleLine = true,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = ImmersivePrimary,
                                            unfocusedBorderColor = ImmersiveOutline,
                                            focusedTextColor = ImmersiveTextWhite,
                                            unfocusedTextColor = ImmersiveTextBody,
                                            focusedContainerColor = ImmersiveSurface,
                                            unfocusedContainerColor = ImmersiveSurface
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp)
                                    )

                                    Box(modifier = Modifier.weight(1f)) {
                                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                            filteredCountries.forEach { country ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedCountry = country
                                                            if (!useCustomExpectancy) {
                                                                customExpectancyValue = country.lifeExpectancy.toFloat()
                                                            }
                                                            countryDropdownExpanded = false
                                                        }
                                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(text = country.flagEmoji, fontSize = 20.sp)
                                                    Spacer(modifier = Modifier.width(10.dp))
                                                    Text(
                                                        text = country.name,
                                                        color = ImmersiveTextBody,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = "~${country.lifeExpectancy.toInt()} yrs",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = ImmersiveTextMuted
                                                    )
                                                }
                                                HorizontalDivider(color = ImmersiveOutline.copy(alpha = 0.3f))
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                // Birthday spinners
                Column {
                    Text(
                        text = "Birth Date",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = ImmersivePrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Month Dropdown
                        var monthOpen by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1.3f)) {
                            OutlinedButton(
                                onClick = { monthOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, ImmersiveOutline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersiveTextWhite)
                            ) {
                                Text(
                                    text = java.time.Month.of(birthMonth).getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            DropdownMenu(expanded = monthOpen, onDismissRequest = { monthOpen = false }) {
                                (1..12).forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(java.time.Month.of(m).getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())) },
                                        onClick = {
                                            birthMonth = m
                                            monthOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        // Day Dropdown
                        var dayOpen by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(0.9f)) {
                            OutlinedButton(
                                onClick = { dayOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, ImmersiveOutline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersiveTextWhite)
                            ) {
                                Text(text = birthDay.toString())
                            }
                            DropdownMenu(expanded = dayOpen, onDismissRequest = { dayOpen = false }) {
                                (1..daysInMonth).forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(d.toString()) },
                                        onClick = {
                                            birthDay = d
                                            dayOpen = false
                                        }
                                    )
                                }
                            }
                        }

                        // Year Dropdown
                        var yearOpen by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1.1f)) {
                            OutlinedButton(
                                onClick = { yearOpen = true },
                                modifier = Modifier.fillMaxWidth(),
                                border = BorderStroke(1.dp, ImmersiveOutline),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ImmersiveTextWhite)
                            ) {
                                Text(text = birthYear.toString())
                            }
                            DropdownMenu(expanded = yearOpen, onDismissRequest = { yearOpen = false }) {
                                (1920..currentYear).reversed().toList().forEach { yr ->
                                    DropdownMenuItem(
                                        text = { Text(yr.toString()) },
                                        onClick = {
                                            birthYear = yr
                                            yearOpen = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Custom Expectancy Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Manual Life Expectancy",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ImmersiveTextWhite
                        )
                        Switch(
                            checked = useCustomExpectancy,
                            onCheckedChange = {
                                useCustomExpectancy = it
                                if (!it) {
                                    customExpectancyValue = selectedCountry.lifeExpectancy.toFloat()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ImmersivePrimary,
                                checkedTrackColor = ImmersivePrimaryContainer
                            )
                        )
                    }

                    if (useCustomExpectancy) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Expectancy: ${customExpectancyValue.toInt()} Years",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = ImmersivePrimary
                        )
                        Slider(
                            value = customExpectancyValue,
                            onValueChange = { customExpectancyValue = it },
                            valueRange = 40f..115f,
                            steps = 75,
                            colors = SliderDefaults.colors(
                                thumbColor = ImmersivePrimary,
                                activeTrackColor = ImmersivePrimary
                            )
                        )
                    }
                }
            }
        }
    )
}
