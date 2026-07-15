package com.medfusion.ai.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.medfusion.ai.domain.model.User
import com.medfusion.ai.domain.model.UserRole
import com.medfusion.ai.ui.auth.LoginScreen
import com.medfusion.ai.ui.auth.RegisterScreen
import com.medfusion.ai.ui.dashboard.DoctorDashboardScreen
import com.medfusion.ai.ui.dashboard.PatientDashboardScreen
import com.medfusion.ai.ui.analysis.AnalysisScreen
import com.medfusion.ai.ui.appointment.BookAppointmentScreen
import com.medfusion.ai.ui.appointment.PatientAppointmentsScreen
import com.medfusion.ai.ui.care.CarePlanScreen
import com.medfusion.ai.ui.consultation.DoctorConsultationScreen
import com.medfusion.ai.ui.consultation.PrescriptionScreen
import com.medfusion.ai.ui.landing.RoleSelectionScreen
import com.medfusion.ai.ui.result.ResultScreen
import com.medfusion.ai.ui.settings.SettingsScreen
import com.medfusion.ai.ui.symptom.SymptomAnalysisScreen
import com.medfusion.ai.ui.video.VideoCallScreen
import com.medfusion.ai.ui.vitals.VitalsMonitorScreen
import com.medfusion.ai.ui.upload.UploadResultsScreen

/**
 * Single source of navigation for the app. Auth routes (role selection → login /
 * register → dashboard) are wired here; the patient journey placeholders below
 * are replaced by their real screens in later phases without changing routes.
 */
@Composable
fun MedFusionNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.ROLE_SELECTION,
) {
    // Routes a freshly-authenticated user to their dashboard, clearing the auth
    // back stack so the hardware back button can't return to a login screen.
    fun goToDashboard(user: User) {
        val dest = when (user.role) {
            UserRole.PATIENT -> Routes.PATIENT_DASHBOARD
            UserRole.DOCTOR -> Routes.DOCTOR_DASHBOARD
        }
        navController.navigate(dest) {
            popUpTo(Routes.ROLE_SELECTION) { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Routes.ROLE_SELECTION) {
            RoleSelectionScreen(
                onSelectRole = { role ->
                    navController.navigate(
                        if (role == UserRole.PATIENT) Routes.PATIENT_LOGIN else Routes.DOCTOR_LOGIN
                    )
                },
            )
        }

        composable(Routes.PATIENT_LOGIN) {
            LoginScreen(
                role = UserRole.PATIENT,
                onAuthenticated = ::goToDashboard,
                onNavigateToRegister = {
                    navController.navigate(Routes.register(UserRole.PATIENT.wireValue))
                },
            )
        }

        composable(Routes.DOCTOR_LOGIN) {
            LoginScreen(
                role = UserRole.DOCTOR,
                onAuthenticated = ::goToDashboard,
                onNavigateToRegister = {
                    navController.navigate(Routes.register(UserRole.DOCTOR.wireValue))
                },
            )
        }

        composable(
            route = Routes.REGISTER,
            arguments = listOf(
                navArgument(Routes.Args.ROLE) {
                    type = NavType.StringType
                    defaultValue = UserRole.PATIENT.wireValue
                },
            ),
        ) { entry ->
            val role = UserRole.fromWire(entry.arguments?.getString(Routes.Args.ROLE))
                ?: UserRole.PATIENT
            RegisterScreen(
                role = role,
                onAuthenticated = ::goToDashboard,
                onNavigateToLogin = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.PATIENT_DASHBOARD) {
            PatientDashboardScreen(
                onStartTriage = { navController.navigate(Routes.SYMPTOM_TRIAGE) },
                onOpenAppointments = { navController.navigate(Routes.PATIENT_APPOINTMENTS) },
                onOpenCarePlan = { navController.navigate(Routes.CARE_PLAN) },
                onOpenVitals = { navController.navigate(Routes.VITALS_MONITOR) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onLoggedOut = { navController.returnToRoleSelection() },
            )
        }

        composable(Routes.DOCTOR_DASHBOARD) {
            DoctorDashboardScreen(
                onLoggedOut = { navController.returnToRoleSelection() },
                onOpenConsultation = { appointmentId ->
                    navController.navigate(Routes.doctorConsultation(appointmentId))
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.PATIENT_APPOINTMENTS) {
            PatientAppointmentsScreen(
                onViewPrescription = { appointmentId ->
                    navController.navigate(Routes.prescription(appointmentId))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.DOCTOR_CONSULTATION,
            arguments = listOf(navArgument(Routes.Args.APPOINTMENT_ID) { type = NavType.StringType }),
        ) {
            DoctorConsultationScreen(
                onCompleted = { navController.popBackStack() },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.PRESCRIPTION,
            arguments = listOf(navArgument(Routes.Args.APPOINTMENT_ID) { type = NavType.StringType }),
        ) {
            PrescriptionScreen(
                onBack = { navController.popBackStack() },
                onOpenCarePlan = { navController.navigate(Routes.CARE_PLAN) },
            )
        }

        composable(
            route = Routes.VIDEO_CALL,
            arguments = listOf(navArgument(Routes.Args.APPOINTMENT_ID) { type = NavType.StringType }),
        ) {
            VideoCallScreen(onBack = { navController.popBackStack() })
        }

        // --- Patient journey ---------------------------------------------------
        composable(Routes.SYMPTOM_TRIAGE) {
            // AI consultation first; appointments open only when the patient chooses.
            SymptomAnalysisScreen(
                onConsult = { caseId, urgency, specialty ->
                    navController.navigate(Routes.bookAppointment(caseId, urgency, specialty))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.UPLOAD_RESULTS,
            arguments = listOf(navArgument(Routes.Args.CASE_ID) { type = NavType.StringType }),
        ) {
            UploadResultsScreen(
                onAnalysisReady = { caseId ->
                    navController.navigate(Routes.analysis(caseId)) {
                        // Don't leave the upload screen on the back stack.
                        popUpTo(Routes.UPLOAD_RESULTS) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.ANALYSIS,
            arguments = listOf(navArgument(Routes.Args.CASE_ID) { type = NavType.StringType }),
        ) {
            AnalysisScreen(
                onResultReady = { caseId ->
                    navController.navigate(Routes.result(caseId)) {
                        popUpTo(Routes.ANALYSIS) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.RESULT,
            arguments = listOf(navArgument(Routes.Args.CASE_ID) { type = NavType.StringType }),
        ) {
            ResultScreen(
                onBookAppointment = { caseId, urgency ->
                    navController.navigate(Routes.bookAppointment(caseId, urgency))
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.BOOK_APPOINTMENT,
            arguments = listOf(
                navArgument(Routes.Args.CASE_ID) { type = NavType.StringType },
                navArgument(Routes.Args.URGENCY) {
                    type = NavType.StringType
                    defaultValue = "yellow"
                },
                navArgument(Routes.Args.SPECIALTY) {
                    type = NavType.StringType
                    defaultValue = "General Physician"
                },
            ),
        ) {
            BookAppointmentScreen(
                onBooked = {
                    // Return to the patient dashboard after a successful booking.
                    navController.navigate(Routes.PATIENT_DASHBOARD) {
                        popUpTo(Routes.PATIENT_DASHBOARD) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onBack = { navController.popBackStack() },
            )
        }

        // --- Placeholders built in later phases (10, 11) -----------------------
        composable(Routes.CARE_PLAN) {
            CarePlanScreen(
                onBack = { navController.popBackStack() },
                onBookFollowUp = { navController.navigate(Routes.bookFollowUp()) },
            )
        }
        composable(Routes.VITALS_MONITOR) {
            VitalsMonitorScreen(onBack = { navController.popBackStack() })
        }
    }
}

/** Signs-out navigation: clear everything and return to the role picker. */
private fun NavHostController.returnToRoleSelection() {
    navigate(Routes.ROLE_SELECTION) {
        popUpTo(0) { inclusive = true }
        launchSingleTop = true
    }
}
