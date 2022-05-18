package com.example.trackyourhealth.view

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.example.trackyourhealth.R
import com.example.trackyourhealth.databinding.FragmentMetricAddBinding
import com.example.trackyourhealth.model.DailyRecords
import com.example.trackyourhealth.model.Metric
import com.example.trackyourhealth.viewModel.MainViewModel
import com.google.android.material.snackbar.Snackbar
import java.util.*

class MetricAddFragment : Fragment() {
    private lateinit var binding: FragmentMetricAddBinding
    private val viewModel: MainViewModel by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMetricAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        binding.deadline.text = DailyRecords.formatDate(Date())
        binding.deadline.setOnClickListener {
            DatePickerFragment(binding.deadline).show(
                requireActivity().supportFragmentManager,
                null
            )
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_add, menu)
    }

    private fun parseForm() = Metric(
        id = 0,
        title = binding.edittextName.text.toString(),
        listQualitativeMetric = listOf(DailyRecords(binding.edittextInfo.text.toString(), DailyRecords.parseDate(binding.deadline.text.toString()))),
    )

    private fun add() {
        parseForm().let { metric ->
            viewModel.add(metric).observe(viewLifecycleOwner) { status ->
                when (status) {
                    is MainViewModel.Status.Success -> {
                        val insertedId = (status.result as MainViewModel.Result.Id).value
                        MetricAddFragmentDirections.onAddSuccess(insertedId).let {
                            findNavController().navigate(it, navOptions {
                                popUpTo(R.id.destination_list)
                            })
                        }
                        binding.progressHorizontal.visibility = View.INVISIBLE
                    }
                    is MainViewModel.Status.Loading -> {
                        binding.progressHorizontal.visibility = View.VISIBLE
                    }
                    is MainViewModel.Status.Failure -> {
                        Log.e("FRAGMENT", "Failed to add item", status.e)
                        Snackbar.make(binding.root, "Failed to add item", Snackbar.LENGTH_LONG)
                            .show()
                        binding.progressHorizontal.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    private fun validate(): Boolean =
        binding.edittextName.let { field ->
            field.text.toString().trim().isNotEmpty().also {
                field.error = if (!it) "Required field" else null
            }
        }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                if (validate()) add()
            }
        }
        return true
    }
}
