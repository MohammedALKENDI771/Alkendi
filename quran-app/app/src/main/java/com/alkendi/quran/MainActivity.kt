package com.alkendi.quran

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.alkendi.quran.data.QuranRepository
import com.alkendi.quran.ui.theme.*

fun toArabicDigits(n: Int): String {
    val arabic = charArrayOf('٠','١','٢','٣','٤','٥','٦','٧','٨','٩')
    return n.toString().map { if (it.isDigit()) arabic[it - '0'] else it }.joinToString("")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        QuranRepository.load(this)
        setContent {
            var dark by remember { mutableStateOf(prefs(this).getBoolean("dark", false)) }
            var fontSize by remember { mutableStateOf(prefs(this).getFloat("font", 22f)) }
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                QuranTheme(dark = dark) {
                    AppNav(dark = dark, onDark = {
                        dark = it
                        prefs(this).edit().putBoolean("dark", it).apply()
                    }, fontSize = fontSize, onFont = {
                        fontSize = it
                        prefs(this).edit().putFloat("font", it).apply()
                    })
                }
            }
        }
    }
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)
}

data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppNav(dark: Boolean, onDark: (Boolean) -> Unit, fontSize: Float, onFont: (Float) -> Unit) {
    val nav = rememberNavController()
    val tabs = listOf(
        Tab("home", "السور", Icons.Filled.Menu),
        Tab("azkar", "الأذكار", Icons.Filled.Favorite),
        Tab("fav", "المفضلة", Icons.Filled.Star),
        Tab("settings", "الإعدادات", Icons.Filled.Settings)
    )
    Scaffold(
        bottomBar = {
            val entry by nav.currentBackStackEntryAsState()
            val route = entry?.destination?.route ?: "home"
            // إخفاء الشريط في شاشات التفاصيل
            if (route in tabs.map { it.route }) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    tabs.forEach { t ->
                        NavigationBarItem(
                            selected = route == t.route,
                            onClick = { nav.navigate(t.route) { popUpTo("home"); launchSingleTop = true } },
                            icon = { Icon(t.icon, contentDescription = t.label) },
                            label = { Text(t.label) }
                        )
                    }
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad)) {
            NavHost(navController = nav, startDestination = "home") {
                composable("home") { HomeScreen(nav, fontSize) }
                composable("azkar") { AzkarScreen(nav) }
                composable("fav") { FavScreen(nav) }
                composable("settings") { SettingsScreen(nav, dark, onDark, fontSize, onFont) }
                composable("reader/{n}") { e ->
                    val n = e.arguments?.getString("n")?.toIntOrNull() ?: 1
                    ReaderScreen(nav, n, fontSize)
                }
                composable("tadabbur/{n}") { e ->
                    val n = e.arguments?.getString("n")?.toIntOrNull() ?: 1
                    TadabburScreen(nav, n, fontSize)
                }
            }
        }
    }
}

@Composable
fun HeaderCard(title: String, subtitle: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(EmeraldDark, Emerald, EmeraldLight)))
            .padding(20.dp)
    ) {
        Column {
            Text(title, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = GoldLight, fontSize = 15.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.15f)).padding(10.dp)) {
                Text("﷽  بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", color = Color.White, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
fun HomeScreen(nav: NavController, fontSize: Float) {
    val ctx = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("الكل") }
    val surahs = remember {
        val q = query.trim()
        QuranRepository.surahs
    }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(14.dp)) {
        HeaderCard("قرآن تدبُّر", "القرآن كاملاً بالرسم العثماني • تدبر بأسلوب إيماني تربوي مستوحى من منهج الشيخ محمد المقرمي")
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text("ابحث باسم السورة أو رقمها…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("الكل", "مكية", "مدنية").forEach { f ->
                FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f) })
            }
        }
        Spacer(Modifier.height(8.dp))
        val list = QuranRepository.surahs.filter {
            (filter == "الكل" || it.type == filter) &&
            (query.isBlank() || it.arabicName.contains(query) || it.englishName.contains(query, true) || it.number.toString() == query.trim() || toArabicDigits(it.number) == query.trim())
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(list) { s ->
                SurahRow(s.number, s.arabicName, s.type, s.ayahCount, s.translation,
                    onOpen = { nav.navigate("reader/${s.number}") },
                    onTadabbur = { nav.navigate("tadabbur/${s.number}") })
            }
        }
    }
}

@Composable
fun SurahRow(n: Int, name: String, type: String, count: Int, trans: String, onOpen: () -> Unit, onTadabbur: () -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.fillMaxWidth().clickable { onOpen() }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).clip(CircleShape).background(Brush.radialGradient(listOf(GoldLight, Gold))), contentAlignment = Alignment.Center) {
                Text(toArabicDigits(n), fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 18.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = MaterialTheme.colorScheme.onSurface)
                Text("$type • ${toArabicDigits(count)} آيات • $trans", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                FilledTonalButton(onClick = onTadabbur) { Text("تدبر") }
            }
        }
    }
}

@Composable
fun ReaderScreen(nav: NavController, n: Int, fontSize: Float) {
    val ctx = LocalContext.current
    val surah = remember(n) { QuranRepository.getSurah(n) }
    val tad = remember(n) { QuranRepository.getTadabbur(n) }
    var favs by remember { mutableStateOf(favSet(ctx)) }
    val isFav = favs.contains(n)
    if (surah == null) { Text("السورة غير موجودة"); return }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        @OptIn(ExperimentalMaterial3Api::class)
        TopAppBar(
            title = { Text(surah.arabicName, fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowForward, contentDescription = "رجوع") } },
            actions = {
                IconButton(onClick = {
                    toggleFav(ctx, n); favs = favSet(ctx)
                }) { Icon(if (isFav) Icons.Filled.Star else Icons.Filled.Star, contentDescription = null, tint = if (isFav) Gold else MaterialTheme.colorScheme.onSurfaceVariant) }
                TextButton(onClick = { nav.navigate("tadabbur/$n") }) { Text("تدبر السورة") }
            }
        )
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(tad?.title ?: "", fontWeight = FontWeight.Bold, color = EmeraldDark, fontSize = 17.sp)
                        Text(tad?.summary ?: "", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            items(surah.ayahs.indices.toList()) { idx ->
                val ayahText = surah.ayahs[idx]
                val ayahNo = idx + 1
                val t = QuranRepository.getAyahTadabbur(n, ayahNo)
                var showT by remember { mutableStateOf(false) }
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "$ayahText  ﴿${toArabicDigits(ayahNo)}﴾",
                            fontSize = fontSize.sp, lineHeight = (fontSize + 12).sp,
                            textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()
                        )
                        if (t != null) {
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { showT = !showT }) { Text(if (showT) "إخفاء الوقفة التدبرية" else "وقفة تدبرية") }
                            if (showT) {
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(CreamDark).padding(10.dp)) {
                                    Text("💡 $t", fontSize = 15.sp, color = Ink)
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(60.dp)) }
        }
    }
}

@Composable
fun TadabburScreen(nav: NavController, n: Int, fontSize: Float) {
    val surah = remember(n) { QuranRepository.getSurah(n) }
    val tad = remember(n) { QuranRepository.getTadabbur(n) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        @OptIn(ExperimentalMaterial3Api::class)
        TopAppBar(
            title = { Text("تدبر ${surah?.arabicName ?: ""}", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Filled.ArrowForward, contentDescription = "رجوع") } },
            actions = { TextButton(onClick = { nav.navigate("reader/$n") }) { Text("التلاوة") } }
        )
        if (tad == null) { Text("لا يوجد تدبر"); return@Column }
        Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Brush.linearGradient(listOf(EmeraldDark, Emerald))).padding(18.dp)) {
                Column {
                    Text("المحور العام", color = GoldLight, fontWeight = FontWeight.Bold)
                    Text(tad.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text(tad.summary, color = Color.White.copy(0.9f), fontSize = 15.sp)
                }
            }
            SectionCard("📖 وقفة تدبرية", tad.tadabbur, Gold)
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(14.dp)) {
                    Text("🌟 هدايات عملية", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EmeraldDark)
                    tad.hidayat.forEachIndexed { i, h ->
                        Text("${toArabicDigits(i + 1)}. $h", fontSize = 15.sp, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
            SectionCard("✅ العمل بالآية", tad.amal, Emerald)
            SectionCard("🤔 سؤال للتدبر", tad.question, Gold)
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Text(
                    "تنبيه منهجي: مادة التدبر في هذا التطبيق محتوى تربوي إيماني أصلي، مستوحى من منهج الشيخ محمد المقرمي في التدبر (الوقفات والهدايات والعمل)، وليست نقلاً حرفياً من كتبه أو دروسه.",
                    fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(12.dp)
                )
            }
            Button(onClick = { nav.navigate("reader/$n") }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text("ابدأ تلاوة السورة بتدبر")
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun SectionCard(title: String, body: String, accent: Color) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EmeraldDark)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(accent.copy(0.12f)).padding(10.dp)) {
                Text(body, fontSize = 16.sp, lineHeight = 26.sp)
            }
        }
    }
}

val AZKAR = listOf(
    "أذكار الصباح والمساء" to listOf(
        "آية الكرسي: ﴿اللَّهُ لَا إِلَٰهَ إِلَّا هُوَ الْحَيُّ الْقَيُّومُ…﴾ — من قالها حين يمسي أُجير من الجن حتى يصبح.",
        "سورة الإخلاص والمعوذتين (٣ مرات): تكفيك من كل شيء.",
        "أصبحنا وأصبح الملك لله والحمد لله… / أمسينا وأمسى الملك لله…",
        "اللهم بك أصبحنا وبك أمسينا وبك نحيا وبك نموت وإليك النشور.",
        "رضيت بالله رباً وبالإسلام ديناً وبمحمد ﷺ نبياً (٣ مرات).",
        "اللهم إني أسألك العافية في الدنيا والآخرة.",
        "سبحان الله وبحمده (١٠٠ مرة): حُطت خطاياه وإن كانت مثل زبد البحر."
    ),
    "بعد الصلوات" to listOf(
        "أستغفر الله (٣ مرات)، اللهم أنت السلام ومنك السلام تباركت يا ذا الجلال والإكرام.",
        "سبحان الله (٣٣)، الحمد لله (٣٣)، الله أكبر (٣٣)، وتمام المائة: لا إله إلا الله وحده لا شريك له.",
        "آية الكرسي: من قرأها دبر كل صلاة لم يمنعه من دخول الجنة إلا أن يموت.",
        "اللهم أعني على ذكرك وشكرك وحسن عبادتك."
    ),
    "أدعية قرآنية للتدبر" to listOf(
        "﴿اهْدِنَا الصِّرَاطَ الْمُسْتَقِيمَ﴾ — ردده بيقين في كل ركعة.",
        "﴿رَبَّنَا آتِنَا فِي الدُّنْيَا حَسَنَةً وَفِي الْآخِرَةِ حَسَنَةً وَقِنَا عَذَابَ النَّارِ﴾",
        "﴿رَبَّنَا لَا تُزِغْ قُلُوبَنَا بَعْدَ إِذْ هَدَيْتَنَا﴾",
        "﴿رَبِّ اشْرَحْ لِي صَدْرِي وَيَسِّرْ لِي أَمْرِي﴾"
    )
)

@Composable
fun AzkarScreen(nav: NavController) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(14.dp)) {
        HeaderCard("الأذكار والأدعية", "حصن المسلم اليومي • أذكار مختارة بأدلة صحيحة")
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(AZKAR) { (title, items) ->
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(14.dp)) {
                        Text(title, fontWeight = FontWeight.Bold, fontSize = 19.sp, color = EmeraldDark)
                        Spacer(Modifier.height(6.dp))
                        items.forEach { z ->
                            Box(Modifier.fillMaxWidth().padding(vertical = 5.dp).clip(RoundedCornerShape(10.dp)).background(Cream).padding(10.dp)) {
                                Text(z, fontSize = 15.sp, lineHeight = 24.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun favSet(ctx: Context): Set<Int> {
    val s = ctx.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE).getStringSet("fav", emptySet()) ?: emptySet()
    return s.mapNotNull { it.toIntOrNull() }.toSet()
}
fun toggleFav(ctx: Context, n: Int) {
    val p = ctx.getSharedPreferences("quran_prefs", Context.MODE_PRIVATE)
    val cur = p.getStringSet("fav", emptySet())?.toMutableSet() ?: mutableSetOf()
    val s = n.toString()
    if (cur.contains(s)) cur.remove(s) else cur.add(s)
    p.edit().putStringSet("fav", cur).apply()
}

@Composable
fun FavScreen(nav: NavController) {
    val ctx = LocalContext.current
    var favs by remember { mutableStateOf(favSet(ctx)) }
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(14.dp)) {
        HeaderCard("المفضلة ⭐", "سورك المحفوظة للتلاوة والتدبر السريع")
        Spacer(Modifier.height(12.dp))
        if (favs.isEmpty()) {
            Card(shape = RoundedCornerShape(18.dp)) {
                Text("لا توجد سور في المفضلة بعد. افتح أي سورة واضغط زر النجمة ⭐ لحفظها هنا.", modifier = Modifier.padding(16.dp), fontSize = 16.sp)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(favs.sorted()) { n ->
                    val s = QuranRepository.getSurah(n) ?: return@items
                    SurahRow(s.number, s.arabicName, s.type, s.ayahCount, s.translation,
                        onOpen = { nav.navigate("reader/${s.number}") },
                        onTadabbur = { nav.navigate("tadabbur/${s.number}") })
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(nav: NavController, dark: Boolean, onDark: (Boolean) -> Unit, fontSize: Float, onFont: (Float) -> Unit) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(14.dp).verticalScroll(rememberScrollState())) {
        HeaderCard("الإعدادات ⚙️", "خصص تجربة التلاوة والتدبر")
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("الوضع الليلي 🌙", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Switch(checked = dark, onCheckedChange = onDark)
                }
                Divider()
                Text("حجم خط الآيات: ${toArabicDigits(fontSize.toInt())}", fontWeight = FontWeight.Bold)
                Slider(value = fontSize, onValueChange = onFont, valueRange = 16f..32f, steps = 7)
                Text("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", fontSize = fontSize.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(18.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("عن التطبيق ومنهج التدبر", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = EmeraldDark)
                Text(
                    "تطبيق «قرآن تدبر» يعرض القرآن الكريم كاملاً بالرسم العثماني بألوان مريحة (زمردي وذهبي وكريمي)، مع مادة تدبرية لكل سورة: المحور، وقفة تدبرية، هدايات عملية، عمل بالآية، وسؤال للتفكر.\n\nالمنهج مستوحى من طريقة الشيخ محمد المقرمي في التدبر: ربط الآية بالقلب والواقع، والخروج بهداية عملية. جميع نصوص التدبر في التطبيق محتوى أصلي كُتب خصيصاً له، وليست نقلاً من كتب الشيخ — للاستزادة ارجع لدروسه ومؤلفاته.\n\nصُمم بواسطة Alkendi • الإصدار 1.0.0",
                    fontSize = 15.sp, lineHeight = 25.sp
                )
            }
        }
    }
}
