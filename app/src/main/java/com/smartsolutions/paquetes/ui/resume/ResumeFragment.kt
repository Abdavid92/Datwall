package com.smartsolutions.paquetes.ui.resume

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.smartsolutions.paquetes.R

class ResumeFragment : Fragment() {

    companion object {
        fun newInstance() = ResumeFragment()
    }

    private lateinit var viewModel: ResumeViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_resume, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(ResumeViewModel::class.java)
        // TODO: Use the ViewModel
    }

}