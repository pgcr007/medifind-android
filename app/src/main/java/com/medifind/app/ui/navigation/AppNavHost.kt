package com.medifind.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.medifind.app.data.remote.MedicineResponse
import com.medifind.app.ui.screens.*
import androidx.compose.runtime.remember

@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {

    NavHost(navController = navController, startDestination = NavRoutes.LOGIN) {

        composable(NavRoutes.LOGIN) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.LOGIN) { inclusive = true }
                    }
                },
                onNavigateToRegister = { navController.navigate(NavRoutes.REGISTER) }
            )
        }

        composable(NavRoutes.REGISTER) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(NavRoutes.REGISTER) { inclusive = true }
                    }
                },
                onNavigateToLogin = { navController.popBackStack() }
            )
        }

        composable(NavRoutes.HOME) {
            var selectedMedicine: MedicineResponse? = null
            HomeScreen(
                onMedicineSelected = { medicine ->
                    selectedMedicine = medicine
                    navController.navigate(NavRoutes.pharmacyResults(medicine._id, medicine.name))
                }
            )
        }

        composable(NavRoutes.PHARMACY_RESULTS) { backStackEntry ->
            val medicineId = backStackEntry.arguments?.getString("medicineId") ?: ""
            val medicineName = backStackEntry.arguments?.getString("medicineName") ?: ""
            PharmacyResultsScreen(
                medicineId = medicineId,
                medicineName = medicineName,
                onPharmacySelected = { pharmacy ->
                    SelectedPharmacyHolder.pharmacy = pharmacy
                    navController.navigate(NavRoutes.PHARMACY_DETAIL)
                }
            )
        }

        composable(NavRoutes.PHARMACY_DETAIL) { backStackEntry ->
            // Get medicineId/medicineName from the previous PHARMACY_RESULTS entry on the back stack
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(NavRoutes.PHARMACY_RESULTS)
            }
            val medicineId = parentEntry.arguments?.getString("medicineId") ?: ""
            val medicineName = parentEntry.arguments?.getString("medicineName") ?: ""
            val pharmacy = SelectedPharmacyHolder.pharmacy

            if (pharmacy != null) {
                PharmacyDetailScreen(
                    pharmacy = pharmacy,
                    medicineId = medicineId,
                    medicineName = medicineName,
                    onReservationComplete = {
                        navController.navigate(
                            NavRoutes.reservationConfirmation(pharmacy.name, medicineName)
                        ) {
                            popUpTo(NavRoutes.HOME)
                        }
                    }
                )
            }
        }

        composable(NavRoutes.RESERVATION_CONFIRMATION) { backStackEntry ->
            val pharmacyName = backStackEntry.arguments?.getString("pharmacyName") ?: ""
            val medicineName = backStackEntry.arguments?.getString("medicineName") ?: ""
            ReservationConfirmationScreen(
                pharmacyName = pharmacyName,
                medicineName = medicineName,
                onDone = {
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }
    }
}