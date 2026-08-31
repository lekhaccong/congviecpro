package com.congviechangngay.pro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

data class WorkBlock(val id:Int, val name:String, val done:Int, val total:Int)
data class Person(val id:Int, val name:String, val group:String, val shift:String)
data class Item(val id:Int, val title:String, val status:String, val module:String)

class AppViewModel : ViewModel() {
    var selected by mutableStateOf("Dashboard")
    var workBlocks by mutableStateOf(listOf(
        WorkBlock(1,"Kiểm tra DATA",12,15),
        WorkBlock(2,"Hàng xuất",19,23),
        WorkBlock(3,"Chốt Lot",6,8),
        WorkBlock(4,"3S / 3D",7,10)
    ))
    var people by mutableStateOf(listOf(
        Person(1,"Nguyễn Văn A","Nhóm 1","Ca 1"),
        Person(2,"Trần Văn B","Nhóm 1","Ca 1"),
        Person(3,"Lê Văn C","Nhóm 2","Ca 3"),
        Person(4,"Phạm Văn D","Nhóm 2","Ca 3")
    ))
    var items by mutableStateOf(listOf(
        Item(1,"DATA SP-001","Đang xử lý","DATA"),
        Item(2,"Invoice INV-260831","OK","Hàng xuất"),
        Item(3,"LOT-260831-08","Chưa chốt","Lot"),
        Item(4,"Checklist 3S khu thành phẩm","Đang xử lý","3S / 3D")
    ))
    var otHours by mutableStateOf(4.5)

    fun addWork() {
        val n = workBlocks.size + 1
        workBlocks = workBlocks + WorkBlock(n,"Khối công việc mới",0,0)
    }
    fun addPerson() {
        val n = people.size + 1
        people = people + Person(n,"Nhân sự $n","Nhóm 1","Ca 1")
    }
}

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = MaterialTheme.colorScheme.primary,
            surface = MaterialTheme.colorScheme.surface
        ),
        content = content
    )
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppTheme { CongViecApp() } }
    }
}

@Composable
fun CongViecApp(vm: AppViewModel = viewModel()) {
    val menu = listOf(
        "Dashboard" to Icons.Default.Home,
        "Nhân sự" to Icons.Default.People,
        "Công việc" to Icons.Default.CheckCircle,
        "OT / AMH" to Icons.Default.Schedule,
        "Hàng" to Icons.Default.Inventory,
        "3S / 3D" to Icons.Default.CleaningServices,
        "Mail" to Icons.Default.Email,
        "Báo cáo" to Icons.Default.BarChart,
        "Backup" to Icons.Default.Backup
    )
    Row(Modifier.fillMaxSize()) {
        NavigationRail(modifier = Modifier.width(82.dp)) {
            Spacer(Modifier.height(12.dp))
            menu.forEach { (name, icon) ->
                NavigationRailItem(
                    selected = vm.selected == name,
                    onClick = { vm.selected = name },
                    icon = { Icon(icon, null) },
                    label = { Text(name, maxLines = 1) }
                )
            }
        }
        VerticalDivider()
        Box(Modifier.fillMaxSize().padding(16.dp)) {
            when (vm.selected) {
                "Dashboard" -> Dashboard(vm)
                "Nhân sự" -> HR(vm)
                "Công việc" -> Work(vm)
                "OT / AMH" -> OT(vm)
                "Hàng" -> Goods(vm)
                "3S / 3D" -> ThreeS(vm)
                "Mail" -> Mail(vm)
                "Báo cáo" -> Reports(vm)
                "Backup" -> Backup(vm)
            }
        }
    }
}

@Composable
fun Header(title:String, subtitle:String? = null, action:(()->Unit)?=null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
        action?.let { Button(onClick=it) { Text("＋ Thêm") } }
    }
    Spacer(Modifier.height(14.dp))
}

@Composable
fun StatCard(title:String, value:String, detail:String) {
    Card(Modifier.widthIn(min=150.dp).padding(end=8.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(title, style=MaterialTheme.typography.labelLarge)
            Text(value, style=MaterialTheme.typography.headlineMedium, fontWeight=FontWeight.Bold)
            Text(detail, style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun Dashboard(vm:AppViewModel) {
    Header("Dashboard", SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date()))
    LazyColumn(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                StatCard("Nhân sự","${vm.people.size}","Đang khai báo")
                StatCard("Công việc","${vm.workBlocks.size}","Khối theo dõi")
                StatCard("OT","${vm.otHours}h","Hôm nay")
                StatCard("Cảnh báo","4","Cần xử lý")
            }
        }
        item { Text("Tình trạng ca làm", fontWeight=FontWeight.Bold, style=MaterialTheme.typography.titleMedium) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Text("Ca 1  06:00–14:00   •   2 nhóm")
                    Text("Ca 2  08:00–17:00   •   Hành chính")
                    Text("Ca 3  14:00–22:00   •   1 nhóm")
                    Text("Ca 4  22:00–06:00   •   1 nhóm")
                }
            }
        }
        item { Text("Việc cần xử lý", fontWeight=FontWeight.Bold, style=MaterialTheme.typography.titleMedium) }
        items(vm.items) { ItemRow(it) }
    }
}

@Composable
fun HR(vm:AppViewModel) {
    Header("NHÂN SỰ","Danh sách • Chia nhóm • Ca làm • Chấm công • OT"){ vm.addPerson() }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item { Text("Ca hiện tại: Ca 1 — 06:00 đến 14:00", fontWeight=FontWeight.Bold) }
        items(vm.people) { p ->
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name,fontWeight=FontWeight.Bold)
                        Text("${p.group} • ${p.shift}")
                    }
                    AssistChip(onClick={}, label={Text("Có mặt")})
                }
            }
        }
    }
}

@Composable
fun Work(vm:AppViewModel) {
    Header("CÔNG VIỆC","Khối công việc • Checklist • Tiến độ • Ảnh • Lịch sử"){ vm.addWork() }
    LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        items(vm.workBlocks) { b ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(b.name, Modifier.weight(1f), fontWeight=FontWeight.Bold)
                        Text("${b.done}/${b.total}")
                    }
                    Spacer(Modifier.height(8.dp))
                    val progress = if(b.total==0) 0f else b.done.toFloat()/b.total
                    LinearProgressIndicator(progress={progress}, Modifier.fillMaxWidth())
                    Text("Checklist • Tiến độ • Ảnh minh chứng • Lịch sử")
                }
            }
        }
    }
}

@Composable
fun OT(vm:AppViewModel) {
    Header("OT / AMH","Tính giờ • Khai báo • Thống kê")
    Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("OT hôm nay", style=MaterialTheme.typography.titleMedium)
                Text("${vm.otHours} giờ", style=MaterialTheme.typography.displaySmall, fontWeight=FontWeight.Bold)
                Text("Tự động tổng hợp theo ca và thời gian chấm công.")
            }
        }
        Button(onClick={vm.otHours += 0.5}) { Text("＋ Ghi nhận 30 phút OT") }
        OutlinedButton(onClick={}) { Text("Khai báo OT / AMH") }
    }
}

@Composable
fun Goods(vm:AppViewModel) {
    Header("HÀNG","DATA • Hàng xuất • Lot • Chốt Lot • QR/Barcode")
    LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick={}) { Icon(Icons.Default.QrCodeScanner,null); Spacer(Modifier.width(6.dp)); Text("Quét QR") }
                OutlinedButton(onClick={}) { Text("Thêm DATA") }
            }
        }
        items(vm.items.filter { it.module in listOf("DATA","Hàng xuất","Lot") }) { ItemRow(it) }
    }
}

@Composable
fun ThreeS(vm:AppViewModel) {
    Header("3S / 3D","Checklist • Ảnh trước/sau • Bất thường")
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Checklist khu thành phẩm", fontWeight=FontWeight.Bold)
        listOf("Sàng lọc","Sắp xếp","Sạch sẽ","Duy trì","Kỷ luật","3D / Bất thường").forEach {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Checkbox(checked=false,onCheckedChange={})
                    Text(it, Modifier.weight(1f))
                    IconButton(onClick={}) { Icon(Icons.Default.CameraAlt,null) }
                }
            }
        }
    }
}

@Composable
fun Mail(vm:AppViewModel) {
    Header("MAIL","Theo dõi thông tin và chốt Lot")
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Text("Các thao tác nhanh", fontWeight=FontWeight.Bold)
        Button(onClick={}) { Icon(Icons.Default.Email,null); Spacer(Modifier.width(8.dp)); Text("Mở ứng dụng Email") }
        OutlinedButton(onClick={}) { Text("Soạn mail chốt Lot") }
        OutlinedButton(onClick={}) { Text("Soạn mail báo bất thường") }
    }
}

@Composable
fun Reports(vm:AppViewModel) {
    Header("BÁO CÁO","KPI • Công việc • Nhân sự • OT • Hàng")
    LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        item { StatCard("Hoàn thành","82%","Công việc hôm nay") }
        item { StatCard("Hàng xuất","19/23","Đã xử lý") }
        item { StatCard("DATA","12/15","Đã OK") }
        item { StatCard("Lot","6/8","Đã chốt") }
        item { StatCard("OT","${vm.otHours}h","Tổng hôm nay") }
    }
}

@Composable
fun Backup(vm:AppViewModel) {
    Header("BACKUP","Toàn bộ • Module • Import • Export • Chia sẻ")
    Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Backup dữ liệu",fontWeight=FontWeight.Bold)
                Text("Thiết kế sẵn cho backup toàn bộ dữ liệu và từng module.")
            }
        }
        Button(onClick={}) { Icon(Icons.Default.Backup,null); Spacer(Modifier.width(8.dp)); Text("Backup toàn bộ") }
        OutlinedButton(onClick={}) { Text("Backup module") }
        OutlinedButton(onClick={}) { Text("Import backup") }
        OutlinedButton(onClick={}) { Text("Export / Chia sẻ backup") }
    }
}

@Composable
fun ItemRow(item:Item) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically) {
            Icon(
                when(item.module) {
                    "DATA" -> Icons.Default.Description
                    "Lot" -> Icons.Default.Inventory
                    else -> Icons.Default.Task
                }, null
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title,fontWeight=FontWeight.Bold)
                Text(item.module)
            }
            AssistChip(onClick={},label={Text(item.status)})
        }
    }
}