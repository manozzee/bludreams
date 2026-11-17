package com.lagradost.cloudstream3.ui.player.source_priority

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import com.lagradost.cloudstream3.databinding.PlayerQualityProfileItemBinding
import com.lagradost.cloudstream3.ui.BaseDiffCallback
import com.lagradost.cloudstream3.ui.NoStateAdapter
import com.lagradost.cloudstream3.ui.ViewHolderState
import com.lagradost.cloudstream3.utils.setText

class ProfilesAdapter(
    val usedProfile: Int?,
    val clickCallback: (oldIndex: Int?, newIndex: Int) -> Unit,
) :
    NoStateAdapter<QualityDataHelper.QualityProfile>(diffCallback = BaseDiffCallback(itemSame = { a, b ->
        a.id == b.id
    })) {

    companion object {
        private val artColors = intArrayOf(
            0xFF00897B.toInt(),
            0xFF1E88E5.toInt(),
            0xFF3949AB.toInt(),
            0xFF8E24AA.toInt(),
            0xFFD81B60.toInt(),
            0xFFE53935.toInt(),
            0xFFFB8C00.toInt(),
        )
    }

    override fun onCreateContent(parent: ViewGroup): ViewHolderState<Any> {
        return ViewHolderState(
            PlayerQualityProfileItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onClearView(holder: ViewHolderState<Any>) {
        when (val binding = holder.view) {
            is PlayerQualityProfileItemBinding -> {
                clearImage(binding.profileImageBackground)
            }
        }
    }

    override fun onBindContent(
        holder: ViewHolderState<Any>,
        item: QualityDataHelper.QualityProfile,
        position: Int
    ) {
        val binding = holder.view as? PlayerQualityProfileItemBinding ?: return

        val priorityText: TextView = binding.profileText
        val profileBg: ImageView = binding.profileImageBackground
        val wifiText: TextView = binding.textIsWifi
        val dataText: TextView = binding.textIsMobileData
        val downloadText: TextView = binding.textIsDownloadData
        val outline: View = binding.outline
        val cardView: View = binding.cardView

        priorityText.setText(item.name)
        dataText.isVisible = item.types.contains(QualityDataHelper.QualityProfileType.Data)
        wifiText.isVisible = item.types.contains(QualityDataHelper.QualityProfileType.WiFi)
        downloadText.isVisible = item.types.contains(QualityDataHelper.QualityProfileType.Download)

        fun setCurrentItem() {
            val prevIndex = currentItem
            // Prevent UI bug when re-selecting the item quickly
            if (prevIndex == position) {
                return
            }
            currentItem = position
            clickCallback.invoke(prevIndex, position)
        }

        outline.isVisible = currentItem == position
        val baseColor = artColors[position % artColors.size]
        val darkColor = ColorUtils.blendARGB(baseColor, Color.BLACK, 0.45f)
        profileBg.setImageDrawable(
            GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(baseColor, darkColor),
            )
        )
        val badgeTint = ColorStateList.valueOf(darkColor)
        wifiText.backgroundTintList = badgeTint
        dataText.backgroundTintList = badgeTint
        downloadText.backgroundTintList = badgeTint

        val textStyle =
            if (item.id == usedProfile) {
                Typeface.BOLD
            } else {
                Typeface.NORMAL
            }

        priorityText.setTypeface(null, textStyle)

        cardView.setOnClickListener {
            setCurrentItem()
        }
    }

    private var currentItem: Int? = null

    fun getCurrentProfile(): QualityDataHelper.QualityProfile? {
        return currentItem?.let { index -> immutableCurrentList.getOrNull(index) }
    }
}