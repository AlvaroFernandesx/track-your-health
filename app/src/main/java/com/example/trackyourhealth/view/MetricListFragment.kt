package com.example.trackyourhealth.view

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.example.trackyourhealth.R
import com.example.trackyourhealth.databinding.FragmentMetricListBinding
import com.example.trackyourhealth.databinding.MetricListItemBinding
import com.example.trackyourhealth.model.Metric
import com.example.trackyourhealth.viewModel.MainViewModel
import com.firebase.ui.auth.AuthUI
import com.google.android.material.snackbar.Snackbar

class MetricListFragment : Fragment() {
    private lateinit var binding: FragmentMetricListBinding
    private val viewModel: MainViewModel by activityViewModels()

    private inner class MetricAdapter(val metrics: List<Metric>) :
        RecyclerView.Adapter<MetricAdapter.MetricHolder>() {

        private inner class MetricHolder(itemBinding: MetricListItemBinding) :
            RecyclerView.ViewHolder(itemBinding.root) {
            val name = itemBinding.contactListItemFullname

            init {
                itemBinding.root.setOnClickListener {
                    MetricListFragmentDirections
                        .showMetricDetails(getItemId(bindingAdapterPosition)).let {
                            findNavController().navigate(it)
                        }
                }
            }
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): MetricHolder =
            MetricHolder(
                MetricListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )

        override fun onBindViewHolder(holder: MetricHolder, position: Int) {
            val contact = metrics[position]
            holder.name.text = contact.title
        }

        override fun getItemCount(): Int = metrics.size

        override fun getItemId(position: Int): Long = metrics[position].id

        override fun onViewRecycled(holder: MetricHolder) {
            super.onViewRecycled(holder)
            Log.d("APP", "Recycled holder at position ${holder.bindingAdapterPosition}")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMetricListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_list, menu)
    }

    private fun refresh() {
        binding.recyclerviewMetricList.apply {
            viewModel.getMetrics().observe(viewLifecycleOwner) { status ->
                when (status) {
                    is MainViewModel.Status.Success -> {
                        val metrics = (status.result as MainViewModel.Result.MetricList).value

                        swapAdapter(MetricAdapter(metrics.sortedBy { it.id }), false)
                        addItemDecoration(
                            DividerItemDecoration(
                                this.context,
                                DividerItemDecoration.VERTICAL
                            )
                        )
                        binding.progressHorizontal.visibility = View.INVISIBLE
                    }
                    is MainViewModel.Status.Loading -> {
                        binding.progressHorizontal.visibility = View.VISIBLE
                    }
                    is MainViewModel.Status.Failure -> {
                        Log.e("VIEW", "Failed to fetch items", status.e)
                        Snackbar.make(binding.root, "Failed to list items", Snackbar.LENGTH_LONG)
                            .show()
                        binding.progressHorizontal.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onStart() {
        super.onStart()
        refresh()
        binding.swipeRefreshLayout.isRefreshing = false
        binding.swipeRefreshLayout.isEnabled = false
        binding.fabAddContact.setOnClickListener {
            MetricListFragmentDirections.addMetric().let {
                findNavController().navigate(it)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_sign_out -> AuthUI.getInstance().signOut(requireContext()).addOnCompleteListener {
                MetricListFragmentDirections.onSignOut().let {
                    findNavController().navigate(it)
                }
            }
        }
        return true
    }
}
