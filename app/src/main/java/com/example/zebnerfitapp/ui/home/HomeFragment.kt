package com.example.zebnerfitapp.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission  // Correct import
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.lifecycleScope
import com.example.zebnerfitapp.databinding.FragmentHomeBinding
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var healthConnectClient: HealthConnectClient

    private val healthPermissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class)
    )

    private val permissionStrings = setOf(
        "android.permission.health.READ_STEPS"
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { granted ->
        if (granted.values.all { it }) {
            lifecycleScope.launch { readStepCount() }
        } else {
            binding.textHome.text = "Permission denied"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        healthConnectClient = HealthConnectClient.getOrCreate(requireContext())

        lifecycleScope.launch {
            try {
                // Get all granted permissions
                val grantedPermissions = healthConnectClient.permissionController.getGrantedPermissions()

                // Check if our required permissions are granted by comparing the sets
                val allGranted = grantedPermissions.containsAll(permissionStrings)

                if (allGranted) {
                    readStepCount()
                } else {
                    // Convert to array for the permission launcher
                    permissionLauncher.launch(permissionStrings.toTypedArray())
                }
            } catch (e: Exception) {
                binding.textHome.text = "Error checking permissions: ${e.message}"
            }
        }
    }

    private suspend fun readStepCount() {
        val startOfDay = ZonedDateTime.now()
            .toLocalDate()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()

        val now = ZonedDateTime.now().toInstant()

        val response = healthConnectClient.readRecords(
            ReadRecordsRequest(
                StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, now)
            )
        )

        val totalSteps = response.records.sumOf { it.count }
        binding.textHome.text = "Steps today: $totalSteps"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
