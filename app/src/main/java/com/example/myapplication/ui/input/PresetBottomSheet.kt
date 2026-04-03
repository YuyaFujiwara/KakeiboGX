package com.example.myapplication.ui.input

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.data.entity.Preset
import com.example.myapplication.data.entity.TransactionType
import com.example.myapplication.databinding.BottomSheetPresetsBinding
import com.example.myapplication.databinding.ItemPresetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class PresetBottomSheet(
    private val currentType: TransactionType,
    private val presets: List<Preset>,
    private val onPresetSelected: (Preset) -> Unit,
    private val onPresetAdded: (Preset) -> Unit,
    private val onPresetDeleted: (Preset) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = BottomSheetPresetsBinding.inflate(inflater, container, false)

        val filteredPresets = presets.filter { it.type == currentType }

        val adapter = PresetAdapter(
            onSelect = { preset ->
                onPresetSelected(preset)
                dismiss()
            },
            onDelete = { preset ->
                onPresetDeleted(preset)
                dismiss()
            }
        )
        binding.rvPresets.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPresets.adapter = adapter
        adapter.submitList(filteredPresets)

        binding.btnAddPreset.setOnClickListener {
            val memo = binding.etPresetMemo.text?.toString()?.trim() ?: ""
            if (memo.isEmpty()) {
                Toast.makeText(requireContext(), "メモを入力してください", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val amountStr = binding.etPresetAmount.text?.toString()?.trim() ?: ""
            val amount = amountStr.toLongOrNull() ?: 0L
            val preset = Preset(
                memo = memo,
                amount = amount,
                type = currentType
            )
            onPresetAdded(preset)
            binding.etPresetMemo.text?.clear()
            binding.etPresetAmount.text?.clear()
            // リスト更新
            val updated = (adapter.currentList + preset).toMutableList()
            adapter.submitList(updated)
            Toast.makeText(requireContext(), "「$memo」を追加しました", Toast.LENGTH_SHORT).show()
        }

        return binding.root
    }
}

class PresetAdapter(
    private val onSelect: (Preset) -> Unit,
    private val onDelete: (Preset) -> Unit
) : ListAdapter<Preset, PresetAdapter.PresetViewHolder>(PresetDiffCallback()) {

    inner class PresetViewHolder(private val binding: ItemPresetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(preset: Preset) {
            binding.tvPresetMemo.text = preset.memo
            binding.tvPresetAmount.text = if (preset.amount > 0) {
                "¥%,d".format(preset.amount)
            } else {
                "金額未設定"
            }
            binding.root.setOnClickListener { onSelect(preset) }
            binding.btnDeletePreset.setOnClickListener { onDelete(preset) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PresetViewHolder {
        val binding = ItemPresetBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return PresetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PresetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class PresetDiffCallback : DiffUtil.ItemCallback<Preset>() {
    override fun areItemsTheSame(oldItem: Preset, newItem: Preset): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Preset, newItem: Preset): Boolean {
        return oldItem == newItem
    }
}
