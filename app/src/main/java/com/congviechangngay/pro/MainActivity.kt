package com.congviechangngay.pro

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    private lateinit var repo: AppRepository
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = AppRepository(this)
        setContent { AppTheme { CongViecApp(repo) } }
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = lightColorScheme(), content = content)

private val MENU = listOf(
    "Dashboard" to Icons.Default.Home,
    "Nhân sự" to Icons.Default.People,
    "Công việc" to Icons.Default.CheckCircle,
    "OT / AMH" to Icons.Default.Schedule,
    "Hàng" to Icons.Default.Inventory,
    "3S / 3D" to Icons.Default.CleaningServices,
    "Mail" to Icons.Default.Email,
    "Báo cáo" to Icons.Default.BarChart,
    "Backup" to Icons.Default.Backup,
)

private val PRIMARY = listOf("Dashboard", "Nhân sự", "Công việc", "Hàng")

@Composable
fun CongViecApp(repo: AppRepository) {
    val vm: AppViewModel = viewModel(factory = VMFactory(repo))
    val context = LocalContext.current
    val compact = LocalConfiguration.current.screenWidthDp < 600
    val snackbarHost = remember { SnackbarHostState() }
    var moreOpen by remember { mutableStateOf(false) }

    val createBackup = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.use { repo.exportBackupTo(it, vm.state) }
                ?: error("Không ghi được file")
            vm.message = "Đã xuất backup ZIP (có dữ liệu + ảnh)"
        }.onFailure { vm.message = "Lỗi backup: ${it.message}" }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            val newState = context.contentResolver.openInputStream(uri)?.use { repo.importBackup(it) }
                ?: error("Không đọc được file")
            vm.importState(newState)
            vm.message = "Đã nhập backup ZIP"
        }.recoverCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                ?: error("Không đọc được file")
            vm.importState(repo.importJson(text))
            vm.message = "Đã nhập backup JSON"
        }.onFailure { vm.message = "Backup không hợp lệ: ${it.message}" }
    }
    val cameraTarget = remember { mutableStateOf<String?>(null) }
    var pendingCamera by remember { mutableStateOf<Uri?>(null) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok && pendingCamera != null) {
            cameraTarget.value?.let { target -> vm.addPhoto(target, pendingCamera.toString()) }
            vm.message = "Đã lưu ảnh"
        }
        pendingCamera = null
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) cameraTarget.value?.let { target ->
            createPhoto(context, target) { uri ->
                pendingCamera = uri
                takePicture.launch(uri)
            }
        } else vm.message = "Bạn chưa cấp quyền Camera"
    }
    fun photo(target: String) {
        cameraTarget.value = target
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            createPhoto(context, target) { u ->
                pendingCamera = u
                takePicture.launch(u)
            }
        } else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbarHost.showSnackbar(it)
            vm.message = null
        }
    }

    @Composable
    fun Body() {
        when (vm.selected) {
            "Dashboard" -> Dashboard(vm)
            "Nhân sự" -> HR(vm)
            "Công việc" -> Work(vm, ::photo)
            "OT / AMH" -> OT(vm)
            "Hàng" -> Goods(vm, ::photo)
            "3S / 3D" -> ThreeS(vm, ::photo)
            "Mail" -> Mail()
            "Báo cáo" -> Reports(vm)
            "Backup" -> Backup(
                vm,
                { createBackup.launch("congviecpro_${System.currentTimeMillis()}.zip") },
                { importBackupLauncher.launch(arrayOf("application/zip", "application/json", "text/plain")) },
            )
        }
    }

    if (compact) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHost) },
            bottomBar = {
                NavigationBar {
                    PRIMARY.forEach { name ->
                        val icon = MENU.first { it.first == name }.second
                        NavigationBarItem(
                            selected = vm.selected == name,
                            onClick = { vm.selected = name },
                            icon = { Icon(icon, contentDescription = name) },
                            label = { Text(name, maxLines = 1) },
                        )
                    }
                    NavigationBarItem(
                        selected = vm.selected !in PRIMARY,
                        onClick = { moreOpen = true },
                        icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "Thêm") },
                        label = { Text("Thêm") },
                    )
                }
            },
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(14.dp)) { Body() }
        }
        if (moreOpen) {
            AlertDialog(
                onDismissRequest = { moreOpen = false },
                title = { Text("Chức năng khác") },
                text = {
                    Column {
                        MENU.filter { it.first !in PRIMARY }.forEach { (name, icon) ->
                            TextButton(onClick = {
                                vm.selected = name
                                moreOpen = false
                            }) {
                                Icon(icon, null)
                                Spacer(Modifier.width(8.dp))
                                Text(name)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { moreOpen = false }) { Text("Đóng") }
                },
            )
        }
    } else {
        Scaffold(snackbarHost = { SnackbarHost(snackbarHost) }) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                NavigationRail(modifier = Modifier.width(88.dp)) {
                    Spacer(Modifier.height(8.dp))
                    MENU.forEach { (name, icon) ->
                        NavigationRailItem(
                            selected = vm.selected == name,
                            onClick = { vm.selected = name },
                            icon = { Icon(icon, null) },
                            label = { Text(name, maxLines = 2) },
                        )
                    }
                }
                VerticalDivider()
                Column(Modifier.fillMaxSize().padding(14.dp)) { Body() }
            }
        }
    }
}

private fun createPhoto(context: Context, target: String, onReady: (Uri) -> Unit) {
    val dir = File(context.filesDir, "photos").apply { mkdirs() }
    val file = File(dir, "${target.replace("[^A-Za-z0-9_-]".toRegex(), "_")}_${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    onReady(uri)
}

@Composable
fun Header(title: String, subtitle: String? = null, action: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it) }
        }
        action?.let { Button(onClick = it) { Text("Thêm") } }
    }
    Spacer(Modifier.height(12.dp))
}

@Composable
fun StatCard(title: String, value: String, detail: String) {
    Card(Modifier.widthIn(min = 135.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(title)
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(detail, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun Dashboard(vm: AppViewModel) {
    Header("Dashboard", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                StatCard("Nhân sự", vm.state.people.size.toString(), "Khai báo")
                StatCard("Công việc", vm.state.workBlocks.size.toString(), "Khối")
                StatCard("OT", String.format(Locale.US, "%.1fh", vm.state.otEntries.sumOf { it.hours }), "Tổng")
                StatCard("Cảnh báo", vm.state.items.count { it.status != "OK" }.toString(), "Cần xử lý")
            }
        }
        item { Text("Tình trạng ca làm", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Ca 1  06:00–14:00 • ${vm.state.people.count { it.shift == "Ca 1" }} người")
                    Text("Ca 2  08:00–17:00 • Hành chính")
                    Text("Ca 3  14:00–22:00 • ${vm.state.people.count { it.shift == "Ca 3" }} người")
                    Text("Ca 4  22:00–06:00 • Theo khai báo")
                }
            }
        }
        item { Text("Việc cần xử lý", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
        items(vm.state.items.filter { it.status != "OK" }) { ItemRow(it) }
        item { Text("Lịch sử gần nhất", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
        items(vm.state.history.takeLast(5).reversed()) { HistoryRow(it) }
    }
}

@Composable
fun ItemRow(i: WorkItem) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp)) {
            Text(i.title, fontWeight = FontWeight.Bold)
            Text("${i.module} • ${i.status}")
        }
    }
}

@Composable
fun HR(vm: AppViewModel) {
    var dialog by remember { mutableStateOf<Person?>(null) }
    var show by remember { mutableStateOf(false) }
    Header("NHÂN SỰ", "Danh sách • Chia nhóm • Ca • Chấm công") {
        dialog = null
        show = true
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(vm.state.people) { p ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontWeight = FontWeight.Bold)
                        Text("${p.group} • ${p.shift}")
                        Text(if (p.present) "Đã chấm công" else "Chưa chấm công", style = MaterialTheme.typography.bodySmall)
                    }
                    AssistChip(onClick = { vm.togglePresent(p) }, label = { Text(if (p.present) "Có mặt" else "Check-in") })
                    IconButton(onClick = { dialog = p; show = true }) { Icon(Icons.Default.Edit, null) }
                    IconButton(onClick = { vm.deletePerson(p) }) { Icon(Icons.Default.Delete, null) }
                }
            }
        }
    }
    if (show) PersonDialog(dialog, { show = false }) { p ->
        if (dialog == null) vm.addPerson(p.name, p.group, p.shift) else vm.updatePerson(p)
        show = false
    }
}

@Composable
fun PersonDialog(original: Person?, onDismiss: () -> Unit, onSave: (Person) -> Unit) {
    var name by remember(original) { mutableStateOf(original?.name ?: "") }
    var group by remember(original) { mutableStateOf(original?.group ?: "Nhóm 1") }
    var shift by remember(original) { mutableStateOf(original?.shift ?: "Ca 1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (original == null) "Thêm nhân sự" else "Sửa nhân sự") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Họ tên") }, singleLine = true)
                OutlinedTextField(group, { group = it }, label = { Text("Nhóm") }, singleLine = true)
                OutlinedTextField(shift, { shift = it }, label = { Text("Ca (Ca 1/2/3/4)") }, singleLine = true)
            }
        },
        confirmButton = {
            Button(
                enabled = name.isNotBlank(),
                onClick = { onSave((original ?: Person(name = name, group = group, shift = shift)).copy(name = name, group = group, shift = shift)) },
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
fun Work(vm: AppViewModel, camera: (String) -> Unit) {
    var editing by remember { mutableStateOf<WorkBlock?>(null) }
    var show by remember { mutableStateOf(false) }
    Header("CÔNG VIỆC", "Khối • Checklist • Tiến độ • Ảnh • Lịch sử") {
        editing = null
        show = true
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(vm.state.workBlocks) { b ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(b.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        Text("${b.done}/${b.total}")
                    }
                    val progress = if (b.total <= 0) 0f else (b.done.toFloat() / b.total).coerceIn(0f, 1f)
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text(if (b.note.isBlank()) "Chưa có ghi chú" else b.note)
                    Row {
                        Button(onClick = { vm.updateWork(b.copy(done = (b.done + 1).coerceAtMost(maxOf(b.total, b.done + 1)))) }) { Text("+1") }
                        Spacer(Modifier.width(6.dp))
                        OutlinedButton(onClick = { camera("work:${b.id}") }) {
                            Icon(Icons.Default.CameraAlt, null)
                            Text(" Ảnh ${b.photoUris.size}")
                        }
                        IconButton(onClick = { editing = b; show = true }) { Icon(Icons.Default.Edit, null) }
                        IconButton(onClick = { vm.deleteWork(b) }) { Icon(Icons.Default.Delete, null) }
                    }
                }
            }
        }
    }
    if (show) WorkDialog(editing, { show = false }) { b ->
        if (editing == null) vm.addWork(b.name, b.total, b.note) else vm.updateWork(b)
        show = false
    }
}

@Composable
fun WorkDialog(original: WorkBlock?, onDismiss: () -> Unit, onSave: (WorkBlock) -> Unit) {
    var name by remember(original) { mutableStateOf(original?.name ?: "") }
    var total by remember(original) { mutableStateOf((original?.total ?: 0).toString()) }
    var note by remember(original) { mutableStateOf(original?.note ?: "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (original == null) "Thêm khối công việc" else "Sửa khối công việc") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Tên khối") }, singleLine = true)
                OutlinedTextField(total, { total = it.filter(Char::isDigit) }, label = { Text("Tổng số việc") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Ghi chú") })
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = {
                onSave((original ?: WorkBlock(name = name)).copy(name = name, total = total.toIntOrNull() ?: 0, note = note))
            }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
fun OT(vm: AppViewModel) {
    var show by remember { mutableStateOf(false) }
    Header("OT / AMH", "Khai báo • Tính giờ • Thống kê") { show = true }
    val total = vm.state.otEntries.sumOf { it.hours }
    Column {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Tổng OT / AMH", style = MaterialTheme.typography.titleMedium)
                Text(String.format(Locale.US, "%.1f giờ", total), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        Button(onClick = { show = true }) { Text("Khai báo OT / AMH") }
        Spacer(Modifier.height(10.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(vm.state.otEntries.reversed()) { x ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(x.personName, fontWeight = FontWeight.Bold)
                            Text("${x.date} • ${x.type} • ${x.hours}h")
                            if (x.note.isNotBlank()) Text(x.note)
                        }
                        IconButton(onClick = { vm.deleteOt(x) }) { Icon(Icons.Default.Delete, null) }
                    }
                }
            }
        }
    }
    if (show) OTDialog(onDismiss = { show = false }) { values ->
        vm.addOt(values[0], values[1], values[2].toDouble(), values[3], values[4])
        show = false
    }
}

@Composable
fun OTDialog(onDismiss: () -> Unit, onSave: (Array<String>) -> Unit) {
    var person by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())) }
    var hours by remember { mutableStateOf("0.5") }
    var type by remember { mutableStateOf("OT") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Khai báo OT / AMH") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(person, { person = it }, label = { Text("Nhân sự") }, singleLine = true)
                OutlinedTextField(date, { date = it }, label = { Text("Ngày") }, singleLine = true)
                OutlinedTextField(hours, { hours = it }, label = { Text("Số giờ") }, singleLine = true)
                OutlinedTextField(type, { type = it }, label = { Text("Loại: OT / AMH") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Ghi chú") })
            }
        },
        confirmButton = {
            Button(
                enabled = person.isNotBlank() && (hours.toDoubleOrNull() ?: 0.0) > 0,
                onClick = { onSave(arrayOf(person, date, hours.toDoubleOrNull()!!.toString(), type, note)) },
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
fun Goods(vm: AppViewModel, camera: (String) -> Unit) {
    var show by remember { mutableStateOf(false) }
    Header("HÀNG", "DATA • Hàng xuất • Lot • Chốt Lot") { show = true }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Button(onClick = { show = true }) { Text("Thêm DATA/Hàng/Lot") }
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            items(vm.state.items.filter { it.module in listOf("DATA", "Hàng xuất", "Lot") }) { i ->
                ItemCard(
                    i,
                    { vm.updateItem(i.copy(status = nextStatus(i.status, i.module))) },
                    { vm.deleteItem(i) },
                    { camera("item:${i.id}") },
                )
            }
        }
    }
    if (show) ItemDialog(onDismiss = { show = false }) { values ->
        vm.addItem(values[0], values[1], values[2], values[3])
        show = false
    }
}

@Composable
fun ItemCard(i: WorkItem, onStatus: () -> Unit, onDelete: () -> Unit, onPhoto: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(11.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (i.module == "DATA") Icons.Default.Description else Icons.Default.Inventory, null)
            Spacer(Modifier.width(9.dp))
            Column(Modifier.weight(1f)) {
                Text(i.title, fontWeight = FontWeight.Bold)
                Text(i.module)
                if (i.note.isNotBlank()) Text(i.note, style = MaterialTheme.typography.bodySmall)
            }
            AssistChip(onClick = onStatus, label = { Text(i.status) })
            IconButton(onClick = onPhoto) { Icon(Icons.Default.CameraAlt, null) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null) }
        }
    }
}

@Composable
fun ItemDialog(onDismiss: () -> Unit, onSave: (Array<String>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var module by remember { mutableStateOf("DATA") }
    var status by remember { mutableStateOf("Đang xử lý") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm DATA / Hàng / Lot") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                OutlinedTextField(title, { title = it }, label = { Text("Mã / nội dung") }, singleLine = true)
                OutlinedTextField(module, { module = it }, label = { Text("Module: DATA / Hàng xuất / Lot") }, singleLine = true)
                OutlinedTextField(status, { status = it }, label = { Text("Trạng thái") }, singleLine = true)
                OutlinedTextField(note, { note = it }, label = { Text("Ghi chú") })
            }
        },
        confirmButton = {
            Button(enabled = title.isNotBlank(), onClick = { onSave(arrayOf(title, module, status, note)) }) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}

@Composable
fun ThreeS(vm: AppViewModel, camera: (String) -> Unit) {
    Header("3S / 3D", "Checklist • Ảnh • Bất thường")
    LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        items(vm.state.checklist) { x ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(x.checked, { vm.toggleCheck(x) })
                    Text(x.title, Modifier.weight(1f), fontWeight = if (x.checked) FontWeight.Normal else FontWeight.Bold)
                    IconButton(onClick = { camera("check:${x.id}") }) { Icon(Icons.Default.CameraAlt, null) }
                    Text("${x.photoUris.size}")
                }
            }
        }
    }
}

@Composable
fun Mail() {
    val context = LocalContext.current
    fun mail(subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
            .putExtra(Intent.EXTRA_SUBJECT, subject)
            .putExtra(Intent.EXTRA_TEXT, body)
        runCatching { context.startActivity(intent) }
    }
    Header("MAIL", "Soạn mail chốt Lot • Bất thường")
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Button(onClick = { mail("", "") }) {
            Icon(Icons.Default.Email, null)
            Text("  Mở ứng dụng Email")
        }
        OutlinedButton(onClick = {
            mail("Chốt Lot", "Kính gửi bộ phận liên quan,\n\nLot đã hoàn thành/chốt.\n\nTrân trọng.")
        }) { Text("Soạn mail chốt Lot") }
        OutlinedButton(onClick = {
            mail("Báo bất thường", "Kính gửi bộ phận liên quan,\n\nNội dung bất thường: \nĐề nghị xử lý.\n\nTrân trọng.")
        }) { Text("Soạn mail báo bất thường") }
    }
}

@Composable
fun Reports(vm: AppViewModel) {
    Header("BÁO CÁO", "Tổng hợp dữ liệu tại máy")
    LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        item {
            val done = vm.state.workBlocks.sumOf { it.done }
            val total = vm.state.workBlocks.sumOf { it.total }
            StatCard("Hoàn thành", if (total == 0) "0%" else "${(done * 100 / total).coerceIn(0, 100)}%", "Công việc")
        }
        item { StatCard("Hàng xuất", vm.state.items.count { it.module == "Hàng xuất" && it.status == "OK" }.toString(), "Đã OK") }
        item { StatCard("DATA", vm.state.items.count { it.module == "DATA" && it.status == "OK" }.toString(), "Đã OK") }
        item { StatCard("Lot", vm.state.items.count { it.module == "Lot" && it.status == "Đã chốt" }.toString(), "Đã chốt") }
        item { StatCard("OT", String.format(Locale.US, "%.1fh", vm.state.otEntries.sumOf { it.hours }), "Tổng") }
        item { Text("Nhật ký thay đổi", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) }
        items(vm.state.history.reversed()) { HistoryRow(it) }
    }
}

@Composable
fun HistoryRow(h: HistoryEntry) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(9.dp)) {
            Text(
                "${SimpleDateFormat("dd/MM HH:mm:ss", Locale.getDefault()).format(Date(h.time))} • ${h.action}",
                fontWeight = FontWeight.Bold,
            )
            Text("${h.module}: ${h.detail}")
        }
    }
}

@Composable
fun Backup(vm: AppViewModel, create: () -> Unit, importer: () -> Unit) {
    Header("BACKUP", "Dữ liệu • Import • Export • Lịch sử")
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Backup v$BACKUP_VERSION", fontWeight = FontWeight.Bold)
                Text("Xuất ZIP gồm backup.json + thư mục ảnh. Nhập backup sẽ khôi phục dữ liệu và ảnh vào bộ nhớ ứng dụng.")
            }
        }
        Button(onClick = create) {
            Icon(Icons.Default.Backup, null)
            Text("  Xuất backup")
        }
        OutlinedButton(onClick = importer) { Text("Nhập backup") }
        OutlinedButton(onClick = { vm.reset(); vm.message = "Đã đặt lại dữ liệu mẫu" }) { Text("Đặt lại dữ liệu mẫu") }
        Text("Đã ghi ${vm.state.history.size} sự kiện lịch sử", fontWeight = FontWeight.Bold)
        Text("Ảnh được lưu trong bộ nhớ ứng dụng; backup ZIP mang theo cả ảnh.")
    }
}
