package com.lagradost.cloudstream3.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import coil3.load
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.databinding.FragmentEasterEggMonkeBinding
import com.lagradost.cloudstream3.ui.settings.AppIconRegistry
import com.lagradost.cloudstream3.utils.UIHelper.hideSystemUI
import com.lagradost.cloudstream3.utils.UIHelper.showSystemUI
import com.lagradost.cloudstream3.utils.UIHelper.toPx
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class EasterEggMonkeFragment : BaseFragment<FragmentEasterEggMonkeBinding>(
    BaseFragment.BindingCreator.Inflate(FragmentEasterEggMonkeBinding::inflate)
) {

    // planet of monks
    private fun currentMonkeys(): List<Int> {
        context?.let { AppIconRegistry.ensureLoaded(it) }
        return listOf(
            R.drawable.monke_benene,
            R.drawable.monke_burrito,
            R.drawable.monke_coco,
            R.drawable.monke_cookie,
            R.drawable.monke_flusdered,
            R.drawable.monke_funny,
            R.drawable.monke_like,
            R.drawable.monke_party,
            R.drawable.monke_sob,
            R.drawable.monke_drink,
            R.drawable.benene,
            AppIconRegistry.currentIcon.value.foregroundRes,
            R.drawable.quick_novel_icon,
        )
    }

    private val activeMonkeys = mutableListOf<ImageView>()
    private var spawningJob: Job? = null

    override fun fixLayout(view: View) = Unit

    override fun onBindingCreated(binding: FragmentEasterEggMonkeBinding) {
        activity?.hideSystemUI()
        spawningJob = lifecycleScope.launch {
            delay(1000)
            while (isActive) {
                spawnMonkey(binding)
                delay(500)
            }
        }
    }

    private fun spawnMonkey(binding: FragmentEasterEggMonkeBinding) {
        val ctx = context ?: return
        val activeIcon = AppIconRegistry.currentIcon.value
        val pickedDrawable = currentMonkeys().random()
        val isLargeCustomIcon = pickedDrawable == activeIcon.foregroundRes

        val newMonkey = ImageView(ctx).apply {
            isVisible = true
        }

        val initialScale = Random.nextFloat() * 1.5f + 0.5f
        newMonkey.scaleX = initialScale
        newMonkey.scaleY = initialScale

        val monkeyW: Float
        val monkeyH: Float
        val layoutParams: FrameLayout.LayoutParams
        if (isLargeCustomIcon) {
            val sizePx = 72.toPx
            newMonkey.load(pickedDrawable)
            monkeyW = sizePx * initialScale
            monkeyH = sizePx * initialScale
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)
        } else {
            newMonkey.setImageResource(pickedDrawable)
            newMonkey.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            monkeyW = newMonkey.measuredWidth * initialScale
            monkeyH = newMonkey.measuredHeight * initialScale
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
            )
        }

        newMonkey.x = Random.nextFloat() * (binding.frame.width.toFloat() - monkeyW)
        newMonkey.y = Random.nextFloat() * (binding.frame.height.toFloat() - monkeyH)

        binding.frame.addView(newMonkey, layoutParams)

        activeMonkeys.add(newMonkey)

        newMonkey.alpha = 0f
        ObjectAnimator.ofFloat(newMonkey, View.ALPHA, 0f, 1f).apply {
            duration = Random.nextLong(1000, 2500)
            interpolator = AccelerateInterpolator()
            start()
        }

        @SuppressLint("ClickableViewAccessibility")
        newMonkey.setOnTouchListener { view, event -> handleTouch(view, event, binding) }

        startFloatingAnimation(newMonkey, binding)
    }

    private fun startFloatingAnimation(monkey: ImageView, binding: FragmentEasterEggMonkeBinding) {
        val floatUpAnimator = ObjectAnimator.ofFloat(
            monkey, View.TRANSLATION_Y, monkey.y, -monkey.height.toFloat()
        ).apply {
            duration = Random.nextLong(8000, 15000)
            interpolator = LinearInterpolator()
        }

        floatUpAnimator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.frame.removeView(monkey)
                activeMonkeys.remove(monkey)
            }
        })

        floatUpAnimator.start()
        monkey.tag = floatUpAnimator
    }

    private fun handleTouch(
        view: View,
        event: MotionEvent,
        binding: FragmentEasterEggMonkeBinding
    ): Boolean {
        val monkey = view as ImageView
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                (monkey.tag as? ObjectAnimator)?.pause()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // Update both X and Y positions properly
                monkey.x = event.rawX - monkey.width / 2
                monkey.y = event.rawY - monkey.height / 2

                // Check if monkey touches the screen edge
                if (isTouchingEdge(monkey, binding)) {
                    removeMonkey(monkey, binding)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isTouchingEdge(monkey, binding)) {
                    removeMonkey(monkey, binding)
                } else {
                    startFloatingAnimation(monkey, binding)
                }
                return true
            }
        }
        return false
    }

    private fun isTouchingEdge(monkey: ImageView, binding: FragmentEasterEggMonkeBinding): Boolean {
        return monkey.x <= 0 || monkey.x + monkey.width >= binding.frame.width ||
                monkey.y <= 0 || monkey.y + monkey.height >= binding.frame.height
    }

    private fun removeMonkey(monkey: ImageView, binding: FragmentEasterEggMonkeBinding) {
        // Fade out and remove the monkey
        ObjectAnimator.ofFloat(monkey, View.ALPHA, 1f, 0f).apply {
            duration = 300
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    binding.frame.removeView(monkey)
                    activeMonkeys.remove(monkey)
                }
            })
            start()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.showSystemUI()
        spawningJob?.cancel()
    }
}
