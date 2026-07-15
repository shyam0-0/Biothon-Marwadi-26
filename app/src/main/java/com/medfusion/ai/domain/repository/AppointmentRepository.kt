package com.medfusion.ai.domain.repository

import com.medfusion.ai.core.util.Resource
import com.medfusion.ai.domain.model.Appointment
import com.medfusion.ai.domain.model.AppointmentStatus
import com.medfusion.ai.domain.model.AvailabilitySlot
import com.medfusion.ai.domain.model.Doctor
import com.medfusion.ai.domain.model.UrgencyLevel
import kotlinx.coroutines.flow.Flow

/** Bookings and doctor availability (Phase 7) + the doctor queue (Phase 8). */
interface AppointmentRepository {

    /** Available slots across doctors for a given ISO date. */
    suspend fun getAvailability(date: String): Resource<List<AvailabilitySlot>>

    /** Doctors matching a recommended specialty (Phase 2 booking flow). */
    suspend fun getDoctorsBySpecialty(specialty: String): Resource<List<Doctor>>

    /** Available slots for one specific doctor on an ISO date. */
    suspend fun getDoctorAvailability(doctorId: String, date: String): Resource<List<AvailabilitySlot>>

    /** Books an appointment for the signed-in patient; status starts PENDING. */
    suspend fun bookAppointment(
        caseId: String?,
        doctorId: String,
        doctorName: String,
        date: String,
        timeSlot: String,
        message: String,
        urgency: UrgencyLevel,
        specialty: String? = null,
    ): Resource<Appointment>

    /** Loads a single appointment by id. */
    suspend fun getAppointment(appointmentId: String): Resource<Appointment>

    /**
     * Live queue of a doctor's appointments, sorted by urgency (red > yellow >
     * green) then requested date/time — the "smart queue".
     */
    fun observeDoctorQueue(doctorId: String): Flow<List<Appointment>>

    /** Updates an appointment's status (accept / reschedule / decline). */
    suspend fun updateStatus(
        appointmentId: String,
        status: AppointmentStatus,
        newDate: String? = null,
        newTimeSlot: String? = null,
    ): Resource<Unit>

    /** Live list of the signed-in patient's appointments (most recent first). */
    fun observePatientAppointments(patientId: String): Flow<List<Appointment>>

    /**
     * Returns the appointment's video room URL, creating it via /create-room and
     * persisting it on first use so patient and doctor share the same room.
     */
    suspend fun getOrCreateRoom(appointmentId: String): Resource<String>
}
