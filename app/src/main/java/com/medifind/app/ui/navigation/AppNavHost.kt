package com.medifind.app.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.medifind.app.ui.screens.*
import androidx.compose.ui.Modifier
import android.net.Uri

@SuppressLint("MissingPermission")
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController(), modifier: Modifier = Modifier) {

    NavHost(navController = navController, startDestination = NavRoutes.LOGIN, modifier = modifier) {

        composable(NavRoutes.CHAT) {
            ChatScreen()
        }

        composable(NavRoutes.PRESCRIPTION_SCAN) {
            PrescriptionScanScreen(
                onMedicineNameSelected = { medicineName ->
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                    // For now, navigating back to Home; pre-filling search is a nice-to-have
                    // we can add via the SearchViewModel if you'd like a smoother handoff.
                }
            )
        }

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
            HomeScreen(
                onMedicineSelected = { medicine ->
                    navController.navigate(NavRoutes.pharmacyResults(medicine._id, Uri.encode(medicine.name)))
                },
                onProfileClick = { navController.navigate(NavRoutes.PROFILE) },
                onScanClick = { navController.navigate(NavRoutes.PRESCRIPTION_SCAN) },
                onBarcodeScanClick = { navController.navigate(NavRoutes.BARCODE_SCAN) },
                onChatClick = { navController.navigate(NavRoutes.CHAT) },
                onRemindersClick = { navController.navigate(NavRoutes.REMINDERS) },
                onVaultClick = { navController.navigate(NavRoutes.PRESCRIPTION_VAULT) }
            )
        }

        composable(NavRoutes.PHARMACY_RESULTS) { backStackEntry ->
            val medicineId = backStackEntry.arguments?.getString("medicineId") ?: ""
            val medicineName = Uri.decode(backStackEntry.arguments?.getString("medicineName") ?: "")
            PharmacyResultsScreen(
                medicineId = medicineId,
                medicineName = medicineName,
                onPharmacySelected = { pharmacy ->
                    SelectedPharmacyHolder.pharmacy = pharmacy
                    navController.navigate(NavRoutes.PHARMACY_DETAIL)
                },
                onComparePricesClick = {
                    navController.navigate(NavRoutes.priceComparison(medicineId, medicineName))
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
            val pharmacyName = Uri.decode(backStackEntry.arguments?.getString("pharmacyName") ?: "")
            val medicineName = Uri.decode(backStackEntry.arguments?.getString("medicineName") ?: "")
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

        composable(NavRoutes.PROFILE) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(NavRoutes.LOGIN) {
                        popUpTo(0)
                    }
                }
            )
        }
        composable(NavRoutes.REMINDERS) {
            ReminderScreen(
                onAddReminder = { navController.navigate(NavRoutes.ADD_REMINDER) }
            )
        }

        composable(NavRoutes.ADD_REMINDER) {
            AddReminderScreen(
                onReminderCreated = { navController.popBackStack() }
            )
        }
        composable(NavRoutes.PRESCRIPTION_VAULT) {
            PrescriptionVaultScreen(
                onAddNew = { navController.navigate(NavRoutes.PRESCRIPTION_SCAN_VAULT) },
                onOpenDetail = { prescription ->
                    SelectedPrescriptionHolder.prescription = prescription
                    navController.navigate(NavRoutes.PRESCRIPTION_DETAIL)
                }
            )
        }

        composable(NavRoutes.PRESCRIPTION_SCAN_VAULT) {
            PrescriptionScanScreen(
                mode = ScanMode.VAULT,
                onSavedToVault = {
                    navController.navigate(NavRoutes.PRESCRIPTION_VAULT) {
                        popUpTo(NavRoutes.PRESCRIPTION_VAULT) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.BARCODE_SCAN) {
            PrescriptionScanScreen(
                mode = ScanMode.BARCODE,
                onMedicineNameSelected = { value ->
                    PendingSearchQueryHolder.query = value
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.HOME) { inclusive = true }
                    }
                }
            )
        }

        composable(NavRoutes.PRESCRIPTION_DETAIL) {
            val prescription = SelectedPrescriptionHolder.prescription
            if (prescription != null) {
                PrescriptionDetailScreen(
                    prescription = prescription,
                    onBack = { navController.popBackStack() },
                    onDeleted = {
                        navController.navigate(NavRoutes.PRESCRIPTION_VAULT) {
                            popUpTo(NavRoutes.PRESCRIPTION_VAULT) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(NavRoutes.PRICE_COMPARISON) { backStackEntry ->
            val medicineId = backStackEntry.arguments?.getString("medicineId") ?: ""
            val medicineName = Uri.decode(backStackEntry.arguments?.getString("medicineName") ?: "")
            PriceComparisonScreen(medicineId = medicineId, medicineName = medicineName)
        }
    }

}