package com.example.labassignment2_cse489.broadcast

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.labassignment2_cse489.R
import com.example.labassignment2_cse489.databinding.FragmentBroadcastBinding

/**
 * Screen 1 of the broadcast flow (the "Broadcast Receiver" drawer item): a Spinner with the two
 * broadcast types and a Proceed button that opens Activity 2 for the selected type.
 */
class BroadcastFragment : Fragment(R.layout.fragment_broadcast) {

    // ViewBinding for the fragment's view. It only exists between onViewCreated and
    // onDestroyView, because a fragment can outlive its view (e.g. on the back stack).
    private var _binding: FragmentBroadcastBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BroadcastViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentBroadcastBinding.bind(view)

        setUpSpinner()
        binding.proceedButton.setOnClickListener { openSelectedActivity() }

        viewModel.selectedMode.observe(viewLifecycleOwner) { mode ->
            binding.modeDescription.setText(
                when (mode) {
                    BroadcastMode.CUSTOM -> R.string.broadcast_custom_description
                    BroadcastMode.BATTERY -> R.string.broadcast_battery_description
                },
            )
        }
    }

    private fun setUpSpinner() {
        // The ArrayAdapter takes its two items from the string-array in res/values/arrays.xml.
        val adapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.broadcast_types,
            R.layout.item_spinner,
        )
        adapter.setDropDownViewResource(R.layout.item_spinner)

        val spinner = binding.broadcastTypeSpinner
        spinner.adapter = adapter
        // Show the remembered choice, e.g. after rotation.
        spinner.setSelection(viewModel.currentMode.ordinal, false)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                BroadcastMode.fromPosition(position)?.let(viewModel::select)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    /** Opens Activity 2 with an explicit Intent that carries the selected mode. */
    private fun openSelectedActivity() {
        startActivity(BroadcastInputActivity.newIntent(requireContext(), viewModel.currentMode))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
