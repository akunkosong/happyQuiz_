package com.talentiva.happyquiz

import android.app.Activity
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.talentiva.happyquiz.models.Soal
import com.talentiva.happyquiz.ui.theme.HappyQuizTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.ads.MobileAds
import com.talentiva.happyquiz.helpers.AdMobManager
import com.talentiva.happyquiz.helpers.MediaPlayerManager
import com.talentiva.happyquiz.helpers.PreferenceManager
import com.talentiva.sisko.viewmodel.DatabaseViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import androidx.compose.runtime.rememberCoroutineScope
import androidx.room.ext.capitalize
import kotlinx.coroutines.launch
import java.util.UUID


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) { initializationStatus ->}
        enableEdgeToEdge()
        MediaPlayerManager.startMusic(this)
        AdMobManager.loadAd(this)
        setContent {
            HappyQuizTheme {
                MainScreen()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaPlayerManager.stopMusic()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val viewModel: DatabaseViewModel = viewModel()


    val kategoriList = viewModel.kategoriList.collectAsState()
    val selectedKategori = remember { mutableStateOf<String?>(null) }

    val savedUuid = PreferenceManager.getUuid(context)
    val savedUsername = PreferenceManager.getUsername(context)

    var isFirstOpen = remember { mutableStateOf(savedUuid == null || savedUsername == null) }

    val uuid = remember { mutableStateOf(savedUuid ?: "") }
    val username = remember { mutableStateOf(savedUsername ?: "") }

    LaunchedEffect(Unit) {
        viewModel.fetchKategoriList("dataQuiz")
    }

    if (isFirstOpen.value) {
        UsernameDialog(
            onConfirm = { generatedUuid, generatedUsername ->
                uuid.value = generatedUuid
                username.value = generatedUsername

                // Simpan ke session
                PreferenceManager.saveUser(context, generatedUuid, generatedUsername)

                // Kirim ke Google Sheet
                viewModel.sendUsernameToSheet(generatedUuid, generatedUsername)
                viewModel.sinkronUserAnswer(context, generatedUuid, generatedUsername)

                // Sembunyikan dialog
                isFirstOpen.value = false
            }
        )
    } else if (selectedKategori.value == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Pilih Kategori Quiz", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            if (kategoriList.value.isEmpty()) {
                CircularProgressIndicator()
            } else {
                kategoriList.value.forEach { kategori ->
                    Button(
                        onClick = { selectedKategori.value = kategori },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(kategori)
                    }
                }
            }
        }
    } else {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            Greeting(
                name = selectedKategori.value!!,
                modifier = Modifier.padding(innerPadding),
                kategoriDipilih = selectedKategori.value!!
            )
        }
    }
//    else {
//        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//            Greeting(
//                name = "Happy Quiz", // Tidak menimpa nama aplikasi
//                modifier = Modifier.padding(innerPadding)
//            )
//        }
//    }
}


@Composable
fun UsernameDialog(
    onConfirm: (uuid: String, namauser: String) -> Unit
) {
    val openDialog = remember { mutableStateOf(true) }
    val defaultUsername = remember { generateRandomUsername() }
    val namauser = remember { mutableStateOf(defaultUsername) }
    val uuid = remember { generateUUID() }

    if (openDialog.value) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Selamat Datang!") },
            text = {
                Column {
                    Text("Silakan konfirmasi nama pengguna kamu:", style = typography.titleMedium)
                    OutlinedTextField(
                        value = namauser.value,
                        onValueChange = { namauser.value = it },
                        label = { Text("Username", style = typography.titleMedium) },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.displaySmall
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    openDialog.value = false
                    onConfirm(uuid, namauser.value)
                }) {
                    Text("Konfirmasi")
                }
            }
        )
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier, kategoriDipilih: String) {
    val context = LocalContext.current
    val viewModel: DatabaseViewModel = viewModel()
    val soalList = viewModel.soalList.collectAsState()
    var currentIndex = remember { mutableStateOf(0) }
    val score = viewModel.score.collectAsState()
    var quizFinished = remember { mutableStateOf(false) }

    var showTimeUpDialog = remember { mutableStateOf(false) }
    var showMotivationDialog = remember { mutableStateOf(false) }
    var timerKey = remember { mutableStateOf(0) }
    var currentAnswer = remember { mutableStateOf("") }

    val explanationDialogVisible = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.fetchSoalDenganKategori("dataQuiz", kategoriDipilih)
    }

//    LaunchedEffect(Unit) {
//        viewModel.fetchSoal("dataQuiz")
//    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ){
                PreferenceManager.getUsername(context)?.let {
                    Text(
                        text = it,
                        modifier = Modifier.padding(4.dp),
                        textAlign = TextAlign.Center,
                        style = typography.displaySmall
                    )
                }

                Text(
                    text = "${score.value}",
                    modifier = Modifier.padding(4.dp),
                    textAlign = TextAlign.Center,
                    style = typography.displaySmall
                )

            }
            WaktuBerjalan()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = name,
                    modifier = Modifier.padding(4.dp),
                    textAlign = TextAlign.Center,
                    style = typography.displaySmall
                )
                Text(
                    text = "by digitalent",
                    textAlign = TextAlign.Center,
                    style = typography.titleSmall
                )
            }
        }

        item {
            CircularCountdownTimer(
                key = timerKey.value,
                durationInSeconds = 60,
                onFinished = {
                    if (currentAnswer.value.isEmpty()) {
                        showTimeUpDialog.value = true
                    }
                }
            )
        }

        item {
            if (soalList.value.isEmpty()) {
                CircularProgressIndicator()
            } else if(!quizFinished.value){
                LaunchedEffect(Unit) {
                    viewModel.fetchLeaderboard()
                }
                val remainingSoal = soalList.value.filterNot { it.sudahDijawab }
                val currentSoal = soalList.value.getOrNull(currentIndex.value)
                if (currentSoal != null) {
                    val isAnswered = currentSoal.sudahDijawab
                    val isCorrect = currentAnswer.value == currentSoal.jawaban
                    val backgroundColor = when {
                        !isAnswered -> Color.White
                        isCorrect -> Color(0xFFD0F0C0) // Hijau muda
                        else -> Color(0xFFFFC0CB)     // Merah muda
                    }
                    SoalCard(
                        soal = currentSoal,
                        backgroundColor = backgroundColor,
                        onAnswerSelected = { answer ->
                            currentAnswer.value = answer
                            if (!isAnswered) {
                                if (answer == currentSoal.jawaban) {
                                    viewModel.tambahSkor()
                                    viewModel.saveAnswer(answer, currentSoal.copy(sudahDijawab = true))
                                    val uuid = UUID.randomUUID()
                                    viewModel.sendUserScoreToLeaderboard(uuid = uuid.toString(), username = name, score.value, remainingSoal.size)
                                } else {
                                    viewModel.kurangiSkor()
                                    //showMotivationDialog.value = true
                                    viewModel.saveAnswer(answer, currentSoal.copy(sudahDijawab = true))
                                    val uuid = UUID.randomUUID()
                                    viewModel.sendUserScoreToLeaderboard(uuid = uuid.toString(), username = name, score.value, remainingSoal.size)
                                }
                            }
                        },
                        activity = Activity(),
                        onNextClicked = {
                            currentAnswer.value = ""
                            showTimeUpDialog.value = false
                            showMotivationDialog.value = false
                            explanationDialogVisible.value = false
                            timerKey.value++

                            if (currentIndex.value < remainingSoal.lastIndex) {
                                currentIndex.value++
                            } else {
                                viewModel.hitungSkor()
                                quizFinished.value = true
                            }
                        }
                    )
                }
            } else {
                Text(
                    text = "Skor Anda: ${score.value} dari ${soalList.value.size}",
                    style = typography.headlineMedium,
                    modifier = Modifier.padding(24.dp)
                )
                Button(
                    onClick = {
                        viewModel.resetQuiz(kategoriDipilih)
                        currentIndex.value = 0
                        quizFinished.value = false
                        viewModel.fetchSoalDenganKategori("dataQuiz", kategoriDipilih)
                    }
                ) {
                    Text(text = "Restart Quiz")
                }
            }
        }
    }
    if (showTimeUpDialog.value) {
        AlertDialog(
            onDismissRequest = { showTimeUpDialog.value = false },
            confirmButton = {
                TextButton(onClick = { showTimeUpDialog.value = false }) {
                    Text("Oke")
                }
            },
            title = { Text("Waktu Habis!") },
            text = { Text("Kenapa belum dijawab?") }
        )
    }

    if (showMotivationDialog.value) {
        AlertDialog(
            onDismissRequest = { showMotivationDialog.value = false },
            confirmButton = {
                TextButton(onClick = { showMotivationDialog.value = false }) {
                    Text("Lanjut")
                }
            },
            title = { Text("Tetap Semangat!") },
            text = { Text("Jawabanmu belum benar. Terus belajar ya!") }
        )
    }
}

@Composable
fun SoalCard(
    modifier: Modifier = Modifier,
    soal: Soal?,
    backgroundColor: Color = Color.White,
    activity: Activity,
    onAnswerSelected: (String) -> Unit,
    onNextClicked: () -> Unit
) {

    val viewModel: DatabaseViewModel = viewModel()

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val selectedAnswer = remember(soal?.uuid) { mutableStateOf("") }
    val explanationDialogVisible = remember { mutableStateOf(false) }


    if (soal != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .background(backgroundColor)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Kategori Soal:",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF35257C)
                    )
                    Text(
                        text = soal.kategori,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF35257C)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(text = soal.soal, style = MaterialTheme.typography.headlineSmall)

                Spacer(modifier = Modifier.height(12.dp))

                // helper untuk background opsi
                fun optionBackground(option: String): Color {
                    return when {
                        selectedAnswer.value == option && option == soal.jawaban -> Color(0xFFD0F0C0) // Hijau muda
                        selectedAnswer.value == option && option != soal.jawaban -> Color(0xFFFFC0CB) // Merah muda
                        else -> Color.Transparent
                    }
                }

                @Composable
                fun optionRow(option: String) {
                    val isSelected = selectedAnswer.value == option
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(optionBackground(option))
                            .padding(4.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable(enabled = selectedAnswer.value.isEmpty()) {
                                selectedAnswer.value = option
                                //selectedAnswer(option)
                            }
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                if (selectedAnswer.value.isEmpty()) {
                                    selectedAnswer.value = option
                                    //selectedAnswer(option)
                                }
                            },
                            enabled = selectedAnswer.value.isEmpty()
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = option)
                    }
                }

                // Opsi A-D
                optionRow(soal.opsiA)
                optionRow(soal.opsiB)
                optionRow(soal.opsiC)
                optionRow(soal.opsiD)

                Spacer(modifier = Modifier.height(8.dp))

                ExplanationSection(soal = soal, activity = activity)

                Spacer(modifier = Modifier.height(8.dp))

                // Tombol Next
                TextButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNextClicked() },
                    colors = ButtonColors(
                        containerColor = Color(0xDD35257C),
                        contentColor = Color.White,
                        disabledContainerColor = Color.Gray,
                        disabledContentColor = Color.Black
                    )
                ) {
                    Text("Next", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

@Composable
fun CircularCountdownTimer(
    key: Int,
    durationInSeconds: Int,
    onFinished: () -> Unit
) {
    var timeLeft = remember(key) { mutableStateOf(durationInSeconds) }
    val progress = (60f - timeLeft.value) / 60f // progress 0.0 - 1.0

    LaunchedEffect(key1 = timeLeft.value) {
        if (timeLeft.value > 0) {
            delay(1000L)
            timeLeft.value--
        } else {
            onFinished()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(150.dp)
        ) {
            CircularProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxSize()
                    .size(80.dp),
                strokeWidth = 8.dp,
                color = Color.Blue
            )
            Text(
                text = "${timeLeft.value.toInt()}",
                style = typography.titleLarge,
            )
        }
    }
}


@Composable
fun WaktuBerjalan(modifier: Modifier = Modifier) {
    var waktuSekarang = remember { mutableStateOf(Date()) }

    // Update waktu setiap detik
    LaunchedEffect(Unit) {
        while (true) {
            waktuSekarang.value = Date() // <- Pastikan ini adalah java.util.Date
            delay(1000)
        }
    }

    // Formatter
    val formatterTanggal = remember { SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()) }
    val formatterWaktu = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    // Format nilai
    val tanggal = formatterTanggal.format(waktuSekarang.value)
    val waktu = formatterWaktu.format(waktuSekarang.value)

    Text(
        text = "$tanggal $waktu",
        modifier = modifier.padding(8.dp),
        textAlign = TextAlign.Center,
        style = typography.titleMedium
    )
}

fun generateUUID(): String = java.util.UUID.randomUUID().toString()

fun generateRandomUsername(): String {
    val source = "happyquiz".toCharArray().toList().shuffled().take(5).joinToString("")
    return "hq${source.replace(" ", "").capitalize()}"
}

@Composable
fun ExplanationSection(
    soal: Soal,
    activity: Activity,
) {
    val context = LocalContext.current
    var explanationDialogVisible = remember(soal.uuid) { mutableStateOf(false) }
    var explanationConfirmed = remember(soal.uuid) { mutableStateOf(false) }

    var showAdPrompt = remember { mutableStateOf(false) }

    Column {
        TextButton(
            onClick = {
                if (explanationConfirmed.value) {
                    explanationDialogVisible.value = !explanationDialogVisible.value
                } else {
                    showAdPrompt.value = true
                }
            }
        ) {
            Text("Explanation", style = MaterialTheme.typography.titleMedium)
        }

        if (explanationDialogVisible.value) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFEFEF))
                    .padding(12.dp)
            ) {
                Text("Penjelasan:", style = MaterialTheme.typography.titleMedium)
                Text(soal.penjelasan, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (showAdPrompt.value) {
            AlertDialog(
                onDismissRequest = { showAdPrompt.value = false },
                title = { Text("Tonton Iklan?") },
                text = { Text("Untuk melihat penjelasan, Anda harus menonton iklan terlebih dahulu.") },
                confirmButton = {
                    Button(onClick = {
                        showAdPrompt.value = false
                        AdMobManager.showAd(activity) {
                            //AdMobManager.loadAd(context)
                            explanationConfirmed.value = true
                            explanationDialogVisible.value = true
                        }
                    }) {
                        Text("Tonton Iklan")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = {
                        showAdPrompt.value = false
                    }) {
                        Text("Batal")
                    }
                }
            )
        }
    }
}



//@RequiresApi(Build.VERSION_CODES.O)
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    HappyQuizTheme {
//        Greeting("Android")
//    }
//}