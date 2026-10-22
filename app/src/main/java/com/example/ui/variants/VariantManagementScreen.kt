package com.example.ui.variants

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.model.VariantGroupEntity
import com.example.domain.model.VariantOption
import com.example.domain.model.VariantParser
import com.example.ui.PosViewModel
import com.example.ui.components.CurrencyUtils
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.BrandBlue
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.CardBg
import com.example.ui.theme.PastelPurpleBg
import com.example.ui.theme.PastelPurpleIcon
import com.example.ui.theme.ScaffoldBg
import com.example.ui.theme.SurfaceSubtle
import com.example.ui.theme.TextDarkMuted
import com.example.ui.theme.TextDarkPrimary
import com.example.ui.theme.TextDarkSecondary
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VariantManagementScreen(
    viewModel: PosViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val variantGroups by viewModel.variantGroups.collectAsStateWithLifecycle()

    var editingGroup by remember { mutableStateOf<VariantGroupEntity?>(null) }
    var isAddModalOpen by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<VariantGroupEntity?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ScaffoldBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = TextDarkPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "Manajemen Varian",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDarkPrimary
                        )
                        Text(
                            text = "Atur opsi Size, Type, Beans, Grind, Add-ons",
                            fontSize = 11.sp,
                            color = TextDarkSecondary
                        )
                    }
                }

                Button(
                    onClick = { isAddModalOpen = true },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("add_variant_group_btn")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Tambah", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }

            // List of Variant Groups
            if (variantGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada grup varian. Tambahkan varian pertama!",
                        color = TextDarkSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(variantGroups, key = { it.id }) { group ->
                        VariantGroupCard(
                            group = group,
                            onEdit = { editingGroup = group },
                            onDelete = { groupToDelete = group }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Modal Add or Edit
    if (isAddModalOpen) {
        VariantGroupEditorBottomSheet(
            existing = null,
            onDismiss = { isAddModalOpen = false },
            onSave = { newGroup ->
                viewModel.addVariantGroup(newGroup)
                isAddModalOpen = false
            }
        )
    }

    editingGroup?.let { grp ->
        VariantGroupEditorBottomSheet(
            existing = grp,
            onDismiss = { editingGroup = null },
            onSave = { updated ->
                viewModel.updateVariantGroup(updated)
                editingGroup = null
            }
        )
    }

    // Delete Confirmation Dialog
    groupToDelete?.let { grp ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Hapus Grup Varian?") },
            text = { Text("Grup varian \"${grp.name}\" akan dihapus dari sistem.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVariantGroup(grp)
                        groupToDelete = null
                    }
                ) {
                    Text("Hapus", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VariantGroupCard(
    group: VariantGroupEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val options = remember(group.optionsJson) {
        parseOptionsFromJson(group.optionsJson)
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(PastelPurpleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = null,
                            tint = PastelPurpleIcon,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = group.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )

                        Text(
                            text = if (group.isMultiSelect) "Multi-Pilih (Bisa pilih banyak)" else "Pilihan Tunggal (Radio)",
                            fontSize = 11.sp,
                            color = if (group.isMultiSelect) BrandBlue else TextDarkSecondary
                        )
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = BrandBlue, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(imageVector = Icons.Default.DeleteOutline, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle)
            Spacer(modifier = Modifier.height(12.dp))

            // Options Flow Chips
            Text(
                text = "Pilihan Opsi (${options.size}):",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextDarkMuted
            )

            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                options.forEach { opt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceSubtle)
                            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = opt.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextDarkPrimary
                            )
                            if (opt.priceAdjustment > 0) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+${CurrencyUtils.formatIdr(opt.priceAdjustment)}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlue
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariantGroupEditorBottomSheet(
    existing: VariantGroupEntity?,
    onDismiss: () -> Unit,
    onSave: (VariantGroupEntity) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var groupName by remember { mutableStateOf(existing?.name ?: "") }
    var isMultiSelect by remember { mutableStateOf(existing?.isMultiSelect ?: false) }

    val optionsList = remember {
        mutableStateListOf<Pair<String, Double>>().apply {
            if (existing != null) {
                addAll(parseOptionsFromJson(existing.optionsJson).map { Pair(it.name, it.priceAdjustment) })
            } else {
                add(Pair("Regular", 0.0))
                add(Pair("Large", 5000.0))
            }
        }
    }

    var newOptionName by remember { mutableStateOf("") }
    var newOptionPriceText by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = CardBg
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existing == null) "Tambah Grup Varian" else "Edit Grup Varian",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextDarkPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextDarkSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nama Grup
            Text("Nama Grup Varian", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                placeholder = { Text("Contoh: Size, Beans Type, Grind Size, Add-ons") },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue, unfocusedBorderColor = BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Multi-select Switch
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceSubtle)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Dapat Pilih Banyak (Multi-Select)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                    Text("Cocok untuk Add-ons atau Topping tambahan", fontSize = 11.sp, color = TextDarkSecondary)
                }

                Switch(
                    checked = isMultiSelect,
                    onCheckedChange = { isMultiSelect = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandBlue)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Existing Options
            Text("Daftar Pilihan Opsi", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            optionsList.forEachIndexed { index, (optName, optPrice) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = optName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextDarkPrimary)
                        Text(
                            text = if (optPrice > 0) "+${CurrencyUtils.formatIdr(optPrice)}" else "Gratis (+Rp 0)",
                            fontSize = 12.sp,
                            color = if (optPrice > 0) BrandBlue else TextDarkSecondary
                        )
                    }

                    IconButton(
                        onClick = { optionsList.removeAt(index) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Hapus", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                    }
                }
                HorizontalDivider(thickness = 0.5.dp, color = BorderSubtle)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Add new option row
            Text("Tambah Opsi Baru ke Grup", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newOptionName,
                    onValueChange = { newOptionName = it },
                    placeholder = { Text("Nama (ex: Large)", fontSize = 12.sp, maxLines = 1) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue, unfocusedBorderColor = BorderSubtle),
                    modifier = Modifier.weight(1.3f)
                )

                OutlinedTextField(
                    value = newOptionPriceText,
                    onValueChange = { newOptionPriceText = it.filter { c -> c.isDigit() } },
                    placeholder = { Text("+Rp", fontSize = 12.sp, maxLines = 1) },
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandBlue, unfocusedBorderColor = BorderSubtle),
                    modifier = Modifier.weight(0.9f)
                )

                IconButton(
                    onClick = {
                        if (newOptionName.isNotBlank()) {
                            val priceVal = newOptionPriceText.toDoubleOrNull() ?: 0.0
                            optionsList.add(Pair(newOptionName.trim(), priceVal))
                            newOptionName = ""
                            newOptionPriceText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(BrandBlue)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (groupName.isNotBlank() && optionsList.isNotEmpty()) {
                        val jsonArray = JSONArray()
                        for ((name, price) in optionsList) {
                            val obj = JSONObject()
                            obj.put("name", name)
                            obj.put("price", price)
                            jsonArray.put(obj)
                        }

                        val entity = VariantGroupEntity(
                            id = existing?.id ?: ("vg_" + System.currentTimeMillis()),
                            name = groupName.trim(),
                            isMultiSelect = isMultiSelect,
                            optionsJson = jsonArray.toString(),
                            applicableCategoryIds = existing?.applicableCategoryIds ?: "all",
                            sortOrder = existing?.sortOrder ?: 10
                        )
                        onSave(entity)
                    }
                },
                enabled = groupName.isNotBlank() && optionsList.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = if (existing == null) "Simpan Grup Varian" else "Perbarui Grup Varian",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

fun parseOptionsFromJson(json: String): List<VariantOption> {
    if (json.isBlank()) return emptyList()
    val list = mutableListOf<VariantOption>()
    try {
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                VariantOption(
                    name = obj.optString("name", ""),
                    priceAdjustment = obj.optDouble("price", 0.0)
                )
            )
        }
    } catch (_: Exception) {}
    return list
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VariantManagementModalSheet(
    viewModel: PosViewModel,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val variantGroups by viewModel.variantGroups.collectAsStateWithLifecycle()

    var editingGroup by remember { mutableStateOf<VariantGroupEntity?>(null) }
    var isAddModalOpen by remember { mutableStateOf(false) }
    var groupToDelete by remember { mutableStateOf<VariantGroupEntity?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = CardBg,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(BorderSubtle)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Manajemen Varian", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextDarkPrimary)
                    Text(text = "Atur opsi Size, Type, Beans, Grind, Add-ons", fontSize = 12.sp, color = TextDarkSecondary)
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextDarkSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = { isAddModalOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tambah Grup Varian Baru", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (variantGroups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "Belum ada grup varian tersimpan", color = TextDarkSecondary, fontSize = 13.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    variantGroups.forEach { group ->
                        VariantGroupCard(
                            group = group,
                            onEdit = { editingGroup = group },
                            onDelete = { groupToDelete = group }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (isAddModalOpen) {
        VariantGroupEditorBottomSheet(
            existing = null,
            onDismiss = { isAddModalOpen = false },
            onSave = { newGroup ->
                viewModel.addVariantGroup(newGroup)
                isAddModalOpen = false
            }
        )
    }

    editingGroup?.let { grp ->
        VariantGroupEditorBottomSheet(
            existing = grp,
            onDismiss = { editingGroup = null },
            onSave = { updated ->
                viewModel.updateVariantGroup(updated)
                editingGroup = null
            }
        )
    }

    groupToDelete?.let { grp ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Hapus Grup Varian?") },
            text = { Text("Grup varian \"${grp.name}\" akan dihapus dari sistem.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteVariantGroup(grp)
                        groupToDelete = null
                    }
                ) {
                    Text("Hapus", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }
}
