package com.example.trackyourhealth.view

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.trackyourhealth.R
import com.example.trackyourhealth.databinding.FragmentMetricChangeBinding
import com.example.trackyourhealth.model.DailyRecords
import com.example.trackyourhealth.model.Metric
import com.example.trackyourhealth.viewModel.MainViewModel
import com.google.android.material.snackbar.Snackbar
import java.util.*

class MetricChangeFragment : Fragment() {
    private lateinit var binding: FragmentMetricChangeBinding
    private val viewModel: MainViewModel by activityViewModels()
    private val args: MetricDetailFragmentArgs by navArgs()
    private val metricCache = MutableLiveData<Metric>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMetricChangeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_add, menu)
    }

    private fun addMetric() = metricCache.observe(viewLifecycleOwner) { metric ->
        val getList: MutableList<DailyRecords> = (metric.listQualitativeMetric ?: emptyList()).toMutableList()
        getList += DailyRecords(binding.edittextInfo.text.toString(), DailyRecords.parseDate(binding.deadline.text.toString()))
        viewModel.update(
            Metric(
                id = metric.id,
                title = metric.title,
                listQualitativeMetric = getList
            )
        ).observe(viewLifecycleOwner) { result ->
            when (result) {
                is MainViewModel.Status.Loading -> {
                    binding.progressHorizontal.visibility = View.VISIBLE
                }
                is MainViewModel.Status.Success -> {
                    MetricChangeFragmentDirections.onChangeSuccess(metric.id).let {
                        findNavController().navigate(it)
                    }
                    binding.progressHorizontal.visibility = View.INVISIBLE
                }
                is MainViewModel.Status.Failure -> {
                    Log.e("VIEW", "Failed to add info", result.e)
                    Snackbar.make(
                        binding.root,
                        "Failed to add info",
                        Snackbar.LENGTH_LONG
                    ).show()
                    binding.progressHorizontal.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun validateInfo(): Boolean = binding.edittextInfo.let { field ->
        field.text.toString().trim().isNotEmpty().also {
            field.error = if (!it) "Required field" else null
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_save -> {
                if (validateInfo()) addMetric()
            }
        }
        return true
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

    private fun notifyError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun fillTable() = metricCache.observe(viewLifecycleOwner) { metric ->
        binding.metricName.text = metric.title
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.getById(args.metricId).observe(viewLifecycleOwner) { status ->
            when (status) {
                is MainViewModel.Status.Success -> try {
                    val metric = (status.result as MainViewModel.Result.SingleMetric).value
                    this.metricCache.value = metric
                    fillTable()
                } catch (e: Exception) {
                    Log.e("FRAGMENT", "Failed to render item", e)
                    notifyError("Failed to show item")
                }
                is MainViewModel.Status.Loading -> {
                }
                is MainViewModel.Status.Failure -> {
                    Log.e(
                        "VIEW",
                        "Failed to obtain and render item with id ${args.metricId}",
                        status.e
                    )
                    notifyError("Failed to fetch and show task")
                }
            }
        }

    }
}
