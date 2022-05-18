package com.example.trackyourhealth.view

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navOptions
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.example.trackyourhealth.R
import com.example.trackyourhealth.R.color.*
import com.example.trackyourhealth.databinding.FragmentMetricDetailBinding
import com.example.trackyourhealth.databinding.MetricDetailItemBinding
import com.example.trackyourhealth.databinding.MetricListItemBinding
import com.example.trackyourhealth.model.DailyRecords
import com.example.trackyourhealth.model.Metric
import com.example.trackyourhealth.viewModel.MainViewModel
import com.google.android.material.snackbar.Snackbar
import java.util.*

class MetricDetailFragment : Fragment() {
    private lateinit var binding: FragmentMetricDetailBinding
    private val viewModel: MainViewModel by activityViewModels()
    private val args: MetricDetailFragmentArgs by navArgs()
    private val metricCache = MutableLiveData<Metric>()

    private inner class MetricDetailAdapter(val data: List<DailyRecords>) :
        RecyclerView.Adapter<MetricDetailFragment.MetricDetailAdapter.MetricDetailHolder>() {

        private inner class MetricDetailHolder(itemBinding: MetricDetailItemBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {
            val titleText = itemBinding.titleText
            val dateText = itemBinding.dateText
            val valueText = itemBinding.valueText
            val background = itemBinding.linearLayout

            init {
                itemBinding.root.setOnClickListener {
                    MetricDetailFragmentDirections.showChangeMetric(
                        getItemId(bindingAdapterPosition)
                    ).let {
                        findNavController().navigate(it)
                    }
                }
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): MetricDetailHolder =
            MetricDetailHolder(
                MetricDetailItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: MetricDetailHolder, position: Int) {
            val parameter = data[position]
            holder.titleText.text = parameter.metric
            holder.dateText.text = DailyRecords.formatDate(parameter.date ?: Date())
            when {
                parameter.valueInRelationPrevious > 0.0 -> {
                    holder.background.setBackgroundColor(resources.getColor(green, null))
                    holder.valueText.text = "+".plus( parameter.valueInRelationPrevious.toString())
                }
                parameter.valueInRelationPrevious < 0.0 -> {
                    holder.background.setBackgroundColor(resources.getColor(red, null))
                    holder.valueText.text = "-".plus( parameter.valueInRelationPrevious.toString())
                }
                else -> {
                    holder.background.setBackgroundColor(resources.getColor(yellow, null))
                    holder.valueText.text = ""
                }
            }

        }

        override fun getItemCount(): Int = data.size

        override fun onViewRecycled(holder: MetricDetailHolder) {
            super.onViewRecycled(holder)
            Log.d("APP", "Recycled holder at position ${holder.bindingAdapterPosition}")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMetricDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_detail, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_delete -> {
                showDialog()
            }
            R.id.action_change -> {
                change()
            }
        }
        return true
    }

    private fun change() {
        args.metricId.takeIf { it >= 0 }?.also { metricId ->
            MetricDetailFragmentDirections.showChangeMetric(metricId).let {
                findNavController().navigate(
                    it,
                    navOptions {
                        popUpTo(R.id.destination_list)
                    }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        binding.swipeRefreshLayout.isRefreshing = false
        binding.swipeRefreshLayout.isEnabled = false
        binding.swipeRefreshLayout.setOnRefreshListener {
            makeRequest()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        makeRequest()
    }

    private fun makeRequest() {
        binding.recyclerviewContactList.apply {
            try {
                args.metricId.takeIf { it >= 0 }?.also { metricId ->
                    viewModel.getById(metricId).observe(viewLifecycleOwner) { status ->
                        when (status) {
                            is MainViewModel.Status.Success -> try {
                                val metric = (status.result as MainViewModel.Result.SingleMetric).value
                                val listOrder = metric.listQualitativeMetric?.sortedBy { it.date }
                                metric.listQualitativeMetric = listOrder
                                metricCache.value = metric
                                setupTitle()
                                metric.listQualitativeMetric?.let {
                                    configureWithRules(it)
                                }

                                binding.progressHorizontal.visibility = View.INVISIBLE
                                swapAdapter(metric.listQualitativeMetric?.let {
                                    MetricDetailAdapter(
                                        it
                                    )
                                }, false)
                            } catch (e: Exception) {
                                Log.e("FRAGMENT", "Failed to render item", e)
                                notifyError("Failed to show item")
                            }
                            is MainViewModel.Status.Failure -> {
                                Log.e("VIEW", "Failed to create item detail view", status.e)
                                view.let {
                                    it?.let { it1 ->
                                        Snackbar.make(
                                            it1.rootView, "Failed to fetch item",
                                            Snackbar.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                            is MainViewModel.Status.Loading -> {
                                binding.progressHorizontal.visibility = View.VISIBLE
                            }
                        }
                    }
                } ?: throw Exception("Could not obtain a valid contact id")
            } catch (e: Exception) {
                Log.e("VIEW", "Failed to create item detail view", e)
                view.let {
                    it?.let { it1 ->
                        Snackbar.make(
                            it1.rootView, "No valid id was provided",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun configureWithRules(list: List<DailyRecords>) {
        val doubleList = list.map { it.metric?.toDoubleOrNull() }
        if (doubleList.filterNotNull().size == list.size) {
            var baseValue = doubleList.first() ?: 0.0
            for ((index,value) in doubleList.withIndex()) {
                val difValue = value?.minus(baseValue) ?: 0.0
                list[index].valueInRelationPrevious = difValue
                baseValue = value ?: 0.0
            }
        }
    }

    private fun notifyError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    private fun setupTitle() = metricCache.observe(viewLifecycleOwner) { metric ->
        binding.title.visibility = View.VISIBLE
        binding.title.text = metric.title
    }

    private fun showDialog() {
        val dialogBuilder = AlertDialog.Builder(activity)
        dialogBuilder.setMessage("Do you want to delete this contact ?")
            .setCancelable(true)
            .setPositiveButton("Yes") { _, _ ->
                remove()
            }
            .setNegativeButton("No"     ) { dialog, _ ->
                dialog.cancel()
            }
        val alert = dialogBuilder.create()
        alert.setTitle("Delete")
        alert.show()
    }

    private fun remove() = metricCache.observe(viewLifecycleOwner) { metric ->
        viewModel.remove(metric.id).observe(viewLifecycleOwner) { status ->
            when (status) {
                is MainViewModel.Status.Success -> {
                    MetricDetailFragmentDirections.onRemoveSuccess().let {
                        findNavController().navigate(it)
                    }
                }
                is MainViewModel.Status.Loading -> {
                    binding.progressHorizontal.visibility = View.VISIBLE
                }
                is MainViewModel.Status.Failure -> {
                    Log.e("FRAGMENT", "Failed to remove item", status.e)
                    notifyError("Failed to remove item")
                }
            }
        }
    }

}


