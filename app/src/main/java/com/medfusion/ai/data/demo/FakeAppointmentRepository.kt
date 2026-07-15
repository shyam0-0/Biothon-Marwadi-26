package com.medfusion.ai.data.demo

import com.medfusion.ai.core.util.AppError
import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.data.repository.sortedForQueue
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.AvailabilitySlot
import com.medfusion.ai.domain.model.Doctor
import com.medfusion.ai.domain.model.UrgencyLevel
import com.medfusion.ai.domain.repository.AppointmentRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** In-memory appointments for Demo Mode, seeded so the doctor queue isn't empty. */
@Singleton
class FakeAppointmentRepository @Inject constructor() : AppointmentRepository {

    private val appointments = MutableStateFlow(listOf(DemoData.seedAppointment()))

    override suspend fun getAvailability(date: String): Resource<List<AvailabilitySlot>> =
        Resource.Success(
            DemoData.defaultSlots.map {
                AvailabilitySlot(DemoData.DOCTOR_ID, DemoData.DOCTOR_NAME, date, it)
            }
        )

    override suspend fun getDoctorsBySpecialty(specialty: String): Resource<List<Doctor>> =
        Resource.Success(
            listOf(
                Doctor(DemoData.DOCTOR_ID, DemoData.DOCTOR_NAME, specialty, 12, 4.8, "MBBS, MD"),
                Doctor("demo-doctor-2", "Dr. R. Menon", specialty, 8, 4.6, "MBBS, DNB"),
            )
        )

    override suspend fun getDoctorAvailability(doctorId: String, date: String): Resource<List<AvailabilitySlot>> =
        Resource.Success(
            DemoData.defaultSlots.map { AvailabilitySlot(doctorId, DemoData.DOCTOR_NAME, date, it) }
        )

    override suspend fun getAppointment(appointmentId: String): Resource<Appointment> {
        val appointment = appointments.value.firstOrNull { it.id == appointmentId }
            ?: return Resource.Error(AppError.NotFound("Appointment not found."))
        return Resource.Success(appointment)
    }

    override suspend fun bookAppointment(
        caseId: String?,
        doctorId: String,
        doctorName: String,
        date: String,
        timeSlot: String,
        message: String,
        urgency: UrgencyLevel,
        specialty: String?,
    ): Resource<Appointment> {
        val appointment = Appointment(
            id = UUID.randomUUID().toString(),
            patientId = DemoData.PATIENT_ID,
            patientName = DemoData.PATIENT_NAME,
            doctorId = doctorId,
            doctorName = doctorName,
            date = date,
            timeSlot = timeSlot,
            message = message.trim(),
            urgencyLevel = urgency,
            status = AppointmentStatus.PENDING,
            caseId = caseId,
            createdAtMillis = System.currentTimeMillis(),
            specialty = specialty,
        )
        appointments.value = appointments.value + appointment
        return Resource.Success(appointment)
    }

    /** Exposes internal mutation so the demo consultation repo can update appointments. */
    fun updateAppointment(appointmentId: String, transform: (Appointment) -> Appointment) {
        appointments.value = appointments.value.map {
            if (it.id == appointmentId) transform(it) else it
        }
    }

    override fun observeDoctorQueue(doctorId: String): Flow<List<Appointment>> =
        appointments.map { list -> list.filter { it.doctorId == doctorId || it.doctorId == DemoData.DOCTOR_ID }.sortedForQueue() }

    override suspend fun updateStatus(
        appointmentId: String,
        status: AppointmentStatus,
        newDate: String?,
        newTimeSlot: String?,
    ): Resource<Unit> {
        appointments.value = appointments.value.map {
            if (it.id == appointmentId) it.copy(
                status = status,
                date = newDate ?: it.date,
                timeSlot = newTimeSlot ?: it.timeSlot,
            ) else it
        }
        return Resource.Success(Unit)
    }

    override fun observePatientAppointments(patientId: String): Flow<List<Appointment>> =
        appointments.map { list -> list.sortedByDescending { it.createdAtMillis } }

    override suspend fun getOrCreateRoom(appointmentId: String): Resource<String> {
        val url = "https://medfusion.daily.co/demo-${appointmentId.take(8)}"
        appointments.value = appointments.value.map {
            if (it.id == appointmentId) it.copy(roomUrl = url) else it
        }
        return Resource.Success(url)
    }
}
