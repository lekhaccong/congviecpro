package com.congviechangngay.pro

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

const val PREFS = "congviecpro_data_v2"
const val KEY_STATE = "state_json"
const val BACKUP_VERSION = 2

data class WorkBlock(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val done: Int = 0,
    val total: Int = 0,
    val note: String = "",
    val photoUris: List<String> = emptyList(),
)

data class Person(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val group: String,
    val shift: String,
    val present: Boolean = false,
)

data class WorkItem(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val status: String,
    val module: String,
    val note: String = "",
    val photoUris: List<String> = emptyList(),
)

data class OtEntry(
    val id: String = UUID.randomUUID().toString(),
    val personName: String,
    val date: String,
    val hours: Double,
    val type: String,
    val note: String = "",
)

data class ChecklistEntry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val checked: Boolean = false,
    val note: String = "",
    val photoUris: List<String> = emptyList(),
)

data class HistoryEntry(
    val id: String = UUID.randomUUID().toString(),
    val time: Long = System.currentTimeMillis(),
    val action: String,
    val module: String,
    val detail: String,
)

data class AppState(
    val workBlocks: List<WorkBlock> = emptyList(),
    val people: List<Person> = emptyList(),
    val items: List<WorkItem> = emptyList(),
    val otEntries: List<OtEntry> = emptyList(),
    val checklist: List<ChecklistEntry> = emptyList(),
    val history: List<HistoryEntry> = emptyList(),
)

class AppRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): AppState {
        val raw = prefs.getString(KEY_STATE, null) ?: return defaultState()
        return try {
            stateFromJson(JSONObject(raw))
        } catch (_: Exception) {
            defaultState()
        }
    }

    fun save(state: AppState) {
        prefs.edit().putString(KEY_STATE, stateToJson(state).toString()).apply()
    }

    fun clear() = prefs.edit().remove(KEY_STATE).apply()

    fun defaultState(): AppState {
        return AppState(
            workBlocks = listOf(
                WorkBlock(name = "Kiểm tra DATA", done = 12, total = 15),
                WorkBlock(name = "Hàng xuất", done = 19, total = 23),
                WorkBlock(name = "Chốt Lot", done = 6, total = 8),
                WorkBlock(name = "3S / 3D", done = 7, total = 10),
            ),
            people = listOf(
                Person(name = "Nguyễn Văn A", group = "Nhóm 1", shift = "Ca 1", present = true),
                Person(name = "Trần Văn B", group = "Nhóm 1", shift = "Ca 1", present = true),
                Person(name = "Lê Văn C", group = "Nhóm 2", shift = "Ca 3"),
                Person(name = "Phạm Văn D", group = "Nhóm 2", shift = "Ca 3"),
            ),
            items = listOf(
                WorkItem(title = "DATA SP-001", status = "Đang xử lý", module = "DATA"),
                WorkItem(title = "Invoice INV-260831", status = "OK", module = "Hàng xuất"),
                WorkItem(title = "LOT-260831-08", status = "Chưa chốt", module = "Lot"),
                WorkItem(title = "Checklist 3S khu thành phẩm", status = "Đang xử lý", module = "3S / 3D"),
            ),
            checklist = listOf(
                ChecklistEntry(title = "Sàng lọc"),
                ChecklistEntry(title = "Sắp xếp"),
                ChecklistEntry(title = "Sạch sẽ"),
                ChecklistEntry(title = "Duy trì"),
                ChecklistEntry(title = "Kỷ luật"),
                ChecklistEntry(title = "3D / Bất thường"),
            ),
            history = listOf(
                HistoryEntry(action = "KHỞI TẠO", module = "Hệ thống", detail = "Tạo dữ liệu mẫu lần đầu"),
            ),
        )
    }

    fun exportJson(state: AppState): String {
        val root = stateToJson(state)
        root.put("backupVersion", BACKUP_VERSION)
        root.put("app", "CongViecPro")
        root.put("exportedAt", System.currentTimeMillis())
        return root.toString(2)
    }

    fun exportBackupTo(output: java.io.OutputStream, state: AppState) {
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("backup.json"))
            zip.write(exportJson(state).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
            val photoDir = File(context.filesDir, "photos")
            if (photoDir.exists()) {
                photoDir.listFiles()?.filter { it.isFile }?.forEach { file ->
                    zip.putNextEntry(ZipEntry("photos/${file.name}"))
                    FileInputStream(file).use { it.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        }
    }

    fun importBackup(input: java.io.InputStream): AppState {
        val temp = File(context.cacheDir, "backup_${System.currentTimeMillis()}.zip")
        input.use { source -> FileOutputStream(temp).use { source.copyTo(it) } }
        var json: String? = null
        val photoDir = File(context.filesDir, "photos").apply { mkdirs() }
        ZipInputStream(FileInputStream(temp).buffered()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name.contains("..") || name.startsWith("/") || name.startsWith("\\")) {
                    zip.closeEntry()
                    entry = zip.nextEntry
                    continue
                }
                if (!entry.isDirectory && name == "backup.json") {
                    json = zip.readBytes().toString(Charsets.UTF_8)
                } else if (!entry.isDirectory && name.startsWith("photos/")) {
                    val outFile = File(photoDir, File(name).name)
                    FileOutputStream(outFile).use { zip.copyTo(it) }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        temp.delete()
        return stateFromJson(JSONObject(json ?: error("Thiếu backup.json")))
    }

    fun importJson(text: String): AppState = stateFromJson(JSONObject(text))

    private fun stateToJson(s: AppState): JSONObject = JSONObject().apply {
        put("workBlocks", JSONArray().apply { s.workBlocks.forEach { put(workToJson(it)) } })
        put("people", JSONArray().apply { s.people.forEach { put(personToJson(it)) } })
        put("items", JSONArray().apply { s.items.forEach { put(itemToJson(it)) } })
        put("otEntries", JSONArray().apply { s.otEntries.forEach { put(otToJson(it)) } })
        put("checklist", JSONArray().apply { s.checklist.forEach { put(checkToJson(it)) } })
        put("history", JSONArray().apply { s.history.forEach { put(historyToJson(it)) } })
    }

    private fun workToJson(x: WorkBlock) = JSONObject().apply {
        put("id", x.id); put("name", x.name); put("done", x.done); put("total", x.total); put("note", x.note)
        put("photoUris", JSONArray(x.photoUris))
    }

    private fun personToJson(x: Person) = JSONObject().apply {
        put("id", x.id); put("name", x.name); put("group", x.group); put("shift", x.shift); put("present", x.present)
    }

    private fun itemToJson(x: WorkItem) = JSONObject().apply {
        put("id", x.id); put("title", x.title); put("status", x.status); put("module", x.module); put("note", x.note)
        put("photoUris", JSONArray(x.photoUris))
    }

    private fun otToJson(x: OtEntry) = JSONObject().apply {
        put("id", x.id); put("personName", x.personName); put("date", x.date)
        put("hours", x.hours); put("type", x.type); put("note", x.note)
    }

    private fun checkToJson(x: ChecklistEntry) = JSONObject().apply {
        put("id", x.id); put("title", x.title); put("checked", x.checked); put("note", x.note)
        put("photoUris", JSONArray(x.photoUris))
    }

    private fun historyToJson(x: HistoryEntry) = JSONObject().apply {
        put("id", x.id); put("time", x.time); put("action", x.action); put("module", x.module); put("detail", x.detail)
    }

    private fun strings(o: JSONObject, key: String): List<String> {
        val a = o.optJSONArray(key) ?: return emptyList()
        return (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() }
    }

    private fun stateFromJson(o: JSONObject): AppState {
        fun arr(key: String) = o.optJSONArray(key) ?: JSONArray()
        val wb = arr("workBlocks").let { a ->
            (0 until a.length()).map { i ->
                val x = a.getJSONObject(i)
                WorkBlock(
                    x.optString("id", UUID.randomUUID().toString()),
                    x.optString("name"),
                    x.optInt("done"),
                    x.optInt("total"),
                    x.optString("note"),
                    strings(x, "photoUris"),
                )
            }
        }
        val people = arr("people").let { a ->
            (0 until a.length()).map { i ->
                val x = a.getJSONObject(i)
                Person(
                    x.optString("id", UUID.randomUUID().toString()),
                    x.optString("name"),
                    x.optString("group", "Nhóm 1"),
                    x.optString("shift", "Ca 1"),
                    x.optBoolean("present"),
                )
            }
        }
        val items = arr("items").let { a ->
            (0 until a.length()).map { i ->
                val x = a.getJSONObject(i)
                WorkItem(
                    x.optString("id", UUID.randomUUID().toString()),
                    x.optString("title"),
                    x.optString("status", "Đang xử lý"),
                    x.optString("module", "DATA"),
                    x.optString("note"),
                    strings(x, "photoUris"),
                )
            }
        }
        val ots = arr("otEntries").let { a ->
            (0 until a.length()).map { i ->
                val x = a.getJSONObject(i)
                OtEntry(
                    x.optString("id", UUID.randomUUID().toString()),
                    x.optString("personName"),
                    x.optString("date"),
                    x.optDouble("hours"),
                    x.optString("type", "OT"),
                    x.optString("note"),
                )
            }
        }
        val checks = arr("checklist").let { a ->
            (0 until a.length()).map { i ->
                val x = a.getJSONObject(i)
                ChecklistEntry(
                    x.optString("id", UUID.randomUUID().toString()),
                    x.optString("title"),
                    x.optBoolean("checked"),
                    x.optString("note"),
                    strings(x, "photoUris"),
                )
            }
        }
        val hist = arr("history").let { a ->
            (0 until a.length()).map { i ->
                val x = a.getJSONObject(i)
                HistoryEntry(
                    x.optString("id", UUID.randomUUID().toString()),
                    x.optLong("time"),
                    x.optString("action"),
                    x.optString("module"),
                    x.optString("detail"),
                )
            }
        }
        return AppState(wb, people, items, ots, checks, hist)
    }
}

class AppViewModel(private val repo: AppRepository) : ViewModel() {
    var selected by mutableStateOf("Dashboard")
    var state by mutableStateOf(repo.load())
        private set
    var message by mutableStateOf<String?>(null)

    private fun commit(newState: AppState, action: String, module: String, detail: String) {
        val history = (newState.history + HistoryEntry(action = action, module = module, detail = detail)).takeLast(500)
        state = newState.copy(history = history)
        repo.save(state)
    }

    fun addPerson(name: String, group: String, shift: String) =
        commit(state.copy(people = state.people + Person(name = name, group = group, shift = shift)), "THÊM", "Nhân sự", "$name • $group • $shift")

    fun updatePerson(p: Person) =
        commit(state.copy(people = state.people.map { if (it.id == p.id) p else it }), "SỬA", "Nhân sự", p.name)

    fun deletePerson(p: Person) =
        commit(state.copy(people = state.people.filterNot { it.id == p.id }), "XÓA", "Nhân sự", p.name)

    fun togglePresent(p: Person) = updatePerson(p.copy(present = !p.present))

    fun addWork(name: String, total: Int, note: String) =
        commit(state.copy(workBlocks = state.workBlocks + WorkBlock(name = name, total = total, note = note)), "THÊM", "Công việc", name)

    fun updateWork(b: WorkBlock) =
        commit(state.copy(workBlocks = state.workBlocks.map { if (it.id == b.id) b else it }), "SỬA", "Công việc", b.name)

    fun deleteWork(b: WorkBlock) =
        commit(state.copy(workBlocks = state.workBlocks.filterNot { it.id == b.id }), "XÓA", "Công việc", b.name)

    fun addWorkPhoto(id: String, uri: String) =
        commit(state.copy(workBlocks = state.workBlocks.map { if (it.id == id) it.copy(photoUris = it.photoUris + uri) else it }), "THÊM ẢNH", "Công việc", id)

    fun addItem(title: String, module: String, status: String, note: String) =
        commit(state.copy(items = state.items + WorkItem(title = title, module = module, status = status, note = note)), "THÊM", module, title)

    fun updateItem(i: WorkItem) =
        commit(state.copy(items = state.items.map { if (it.id == i.id) i else it }), "SỬA", i.module, i.title)

    fun deleteItem(i: WorkItem) =
        commit(state.copy(items = state.items.filterNot { it.id == i.id }), "XÓA", i.module, i.title)

    fun addItemPhoto(id: String, uri: String) =
        commit(state.copy(items = state.items.map { if (it.id == id) it.copy(photoUris = it.photoUris + uri) else it }), "THÊM ẢNH", "Hàng/DATA", id)

    fun addOt(person: String, date: String, hours: Double, type: String, note: String) =
        commit(state.copy(otEntries = state.otEntries + OtEntry(personName = person, date = date, hours = hours, type = type, note = note)), "THÊM", "OT / AMH", "$person • $hours h")

    fun deleteOt(x: OtEntry) =
        commit(state.copy(otEntries = state.otEntries.filterNot { it.id == x.id }), "XÓA", "OT / AMH", "${x.personName} • ${x.hours} h")

    fun toggleCheck(x: ChecklistEntry) =
        commit(state.copy(checklist = state.checklist.map { if (it.id == x.id) x.copy(checked = !x.checked) else it }), "SỬA", "3S / 3D", x.title)

    fun addCheckPhoto(id: String, uri: String) =
        commit(state.copy(checklist = state.checklist.map { if (it.id == id) it.copy(photoUris = it.photoUris + uri) else it }), "THÊM ẢNH", "3S / 3D", id)

    fun importState(s: AppState) {
        commit(s, "IMPORT", "Backup", "Nhập backup thành công")
    }

    fun backupText(): String = repo.exportJson(state)

    fun reset() {
        repo.clear()
        commit(repo.defaultState(), "RESET", "Hệ thống", "Đặt lại dữ liệu mẫu")
    }

    fun addPhoto(target: String, uri: String) {
        when {
            target.startsWith("work:") -> addWorkPhoto(target.removePrefix("work:"), uri)
            target.startsWith("item:") -> addItemPhoto(target.removePrefix("item:"), uri)
            target.startsWith("check:") -> addCheckPhoto(target.removePrefix("check:"), uri)
        }
    }
}

class VMFactory(private val repo: AppRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(repo) as T
}

fun nextStatus(status: String, module: String) = when (module) {
    "Lot" -> if (status == "Chưa chốt") "Đã chốt" else "Chưa chốt"
    else -> if (status == "OK") "Đang xử lý" else "OK"
}
