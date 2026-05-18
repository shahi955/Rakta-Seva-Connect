package com.raktaseva.connect.di

import com.raktaseva.connect.repository.AuthRepository
import com.raktaseva.connect.repository.BloodRequestsRepository
import com.raktaseva.connect.repository.DonationsRepository
import com.raktaseva.connect.repository.EmergencyRequestRepository
import com.raktaseva.connect.repository.FcmRepository
import com.raktaseva.connect.repository.FirestoreRepository
import com.raktaseva.connect.repository.NotificationsRepository
import com.raktaseva.connect.repository.UsersRepository

/**
 * Minimal service locator for ViewModels without Hilt.
 * Swap implementations in tests by replacing lazy delegates (future DI).
 */
object AppGraph {
    val authRepository: AuthRepository by lazy { AuthRepository() }
    val firestoreRepository: FirestoreRepository by lazy { FirestoreRepository() }
    val usersRepository: UsersRepository by lazy { UsersRepository() }
    val bloodRequestsRepository: BloodRequestsRepository by lazy { BloodRequestsRepository() }
    val donationsRepository: DonationsRepository by lazy { DonationsRepository() }
    val notificationsRepository: NotificationsRepository by lazy { NotificationsRepository() }
    val emergencyRequestRepository: EmergencyRequestRepository by lazy { EmergencyRequestRepository() }
    val fcmRepository: FcmRepository by lazy { FcmRepository() }
}
