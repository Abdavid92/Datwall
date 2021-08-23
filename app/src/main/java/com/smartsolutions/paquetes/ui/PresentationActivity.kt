package com.smartsolutions.paquetes.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.smartsolutions.paquetes.R
import com.smartsolutions.paquetes.databinding.FragmentPresentationBinding
import com.stephentuso.welcome.*

class PresentationActivity : WelcomeActivity() {

    override fun configuration(): WelcomeConfiguration {

        return WelcomeConfiguration.Builder(this)
            .animateButtons(true)
            .backButtonNavigatesPages(true)
            .canSkip(false)
            .defaultBackgroundColor(R.color.color_primary)
            .page(
                CustomPage(
                    R.drawable.presentation_page_0,
                    R.string.page_0_title,
                    null
                )
            )
            .page(
                CustomPage(
                    R.drawable.presentation_page_1,
                    R.string.page_1_title,
                    R.string.page_1_description
                )
            )
            .page(
                CustomPage(
                    R.drawable.presentation_page_2,
                    R.string.page_2_title,
                    R.string.page_2_description
                )
            )
            .page(
                CustomPage(
                    R.drawable.presentation_page_3,
                    R.string.page_3_title,
                    R.string.page_3_description
                )
            )
            .build()
    }

    class CustomPage(
        @DrawableRes
        val drawableResId: Int,
        @StringRes
        val titleResId: Int?,
        @StringRes
        val descriptionResId: Int?
    ) : FragmentWelcomePage() {

        override fun fragment(): Fragment {
            return CustomWelcomeFragment.newInstance(
                drawableResId,
                titleResId,
                descriptionResId
            )
        }
    }

    class CustomWelcomeFragment : Fragment(), WelcomePage.OnChangeListener {

        private lateinit var binding: FragmentPresentationBinding

        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            binding = FragmentPresentationBinding.inflate(
                inflater,
                container,
                false
            )
            return binding.root
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)

            arguments?.let {
                binding.welImage.setImageResource(it.getInt(DRAWABLE_KEY))

                it.get(TITLE_KEY)?.let { title ->
                    title as Int

                    binding.welTitle.text = HtmlCompat.fromHtml(
                        getString(title),
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                }

                it.get(DESCRIPTION_KEY)?.let { description ->
                    description as Int

                    binding.welDescription.text = HtmlCompat.fromHtml(
                        getString(description),
                        HtmlCompat.FROM_HTML_MODE_COMPACT
                    )
                }
            }
        }

        override fun onWelcomeScreenPageScrolled(pageIndex: Int, offset: Float, offsetPixels: Int) {
            binding.welImage.translationX = -offsetPixels * 0.8f
        }

        override fun onWelcomeScreenPageSelected(pageIndex: Int, selectedPageIndex: Int) {
        }

        override fun onWelcomeScreenPageScrollStateChanged(pageIndex: Int, state: Int) {
        }

        companion object {

            private const val DRAWABLE_KEY = "drawable"
            private const val TITLE_KEY = "title"
            private const val DESCRIPTION_KEY = "description"

            fun newInstance(
                @DrawableRes
                drawableResId: Int,
                @StringRes
                titleResId: Int?,
                @StringRes
                descriptionResId: Int?
            ): CustomWelcomeFragment {
                val args = Bundle().apply {
                    putInt(DRAWABLE_KEY, drawableResId)
                    titleResId?.let {
                        putInt(TITLE_KEY, it)
                    }
                    descriptionResId?.let {
                        putInt(DESCRIPTION_KEY, it)
                    }
                }

                return CustomWelcomeFragment().apply {
                    arguments = args
                }
            }
        }
    }
}