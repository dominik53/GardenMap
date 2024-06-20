package com.example.garden_map.ui.dashboard

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.garden_map.databinding.FragmentDashboardBinding
import android.widget.Button

class DashboardFragment : Fragment() {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private var listener: OnButtonClickListener? = null
    private var showAddMarkerButton = 0
    private var showAddTreeButton = 0

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
                ViewModelProvider(this).get(DashboardViewModel::class.java)

        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // reload showAddMarkerButton
        savedInstanceState?.let {
            showAddMarkerButton = it.getInt("showAddMarkerButton", 0)
            showAddTreeButton = it.getInt("showAddTreeButton",0)
        }

        val textView2: TextView = binding.textView2
        textView2.text = if (showAddMarkerButton == 0) {
            "Edytuj granice"
        } else {
            "Zakończ edytowanie granic"
        }

        val textView3: TextView = binding.textView3
        textView3.text = if (showAddTreeButton == 0) {
            "Dodaj drzewa"
        } else {
            "Zakończ dodawanie drzew"
        }

        // Find buttons by their IDs
        val button1: Button = binding.button1
        button1.setOnClickListener {
            listener?.onButton1Click()
        }

        val button2: Button = binding.button2
        button2.setOnClickListener {
            toggleButton2Text()
            listener?.onButton2Click()
        }

        val button3: Button = binding.button3
        button3.setOnClickListener {
            toggleButton3Text()
            listener?.onButton3Click()
        }

        return root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the current value of showAddMarkerButton
        outState.putInt("showAddMarkerButton", showAddMarkerButton)
        outState.putInt("showAddTreeButton", showAddTreeButton)
    }

    private fun toggleButton2Text() {
        // Toggle the value of showAddMarkerButton and update textView2
        showAddMarkerButton = if (showAddMarkerButton == 0 && showAddTreeButton == 0) 1 else 0
        val textView2: TextView = binding.textView2
        textView2.text = if (showAddMarkerButton == 0 || showAddTreeButton == 1) {
            "Edytuj granice"
        } else {
            "Zakończ edytowanie granic"
        }
    }

    private fun toggleButton3Text() {
        // Toggle the value of showAddTreeButton and update textView3
        showAddTreeButton = if (showAddTreeButton == 0 && showAddMarkerButton == 0) 1 else 0
        val textView3: TextView = binding.textView3
        textView3.text = if (showAddTreeButton == 0 || showAddMarkerButton == 1) {
            "Dodaj drzewa"
        } else {
            "Zakończ dodawanie drzew"
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnButtonClickListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnButtonClickListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    interface OnButtonClickListener {
        fun onButton1Click()
        fun onButton2Click()

        fun onButton3Click()
    }
}