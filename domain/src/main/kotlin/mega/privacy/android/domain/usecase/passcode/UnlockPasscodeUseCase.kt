package mega.privacy.android.domain.usecase.passcode

import kotlinx.coroutines.flow.firstOrNull
import mega.privacy.android.domain.entity.passcode.UnlockPasscodeRequest
import mega.privacy.android.domain.repository.security.PasscodeRepository
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import javax.inject.Inject

/**
 * Unlock passcode use case
 */
class UnlockPasscodeUseCase @Inject constructor(
    private val passcodeRepository: PasscodeRepository,
    private val logoutUseCase: LogoutUseCase,
) {
    /**
     * Invoke
     *
     * @param request
     */
    suspend operator fun invoke(request: UnlockPasscodeRequest) {
        val correct = when (request) {
            is UnlockPasscodeRequest.PasscodeRequest -> passcodeRepository.checkPasscode(request.value)
            is UnlockPasscodeRequest.PasswordRequest -> passcodeRepository.checkPassword(request.value)
        }

        if (correct) {
            passcodeRepository.setFailedAttempts(0)
            passcodeRepository.setLocked(false)
        } else {
            val failedAttempts = passcodeRepository.monitorFailedAttempts().firstOrNull() ?: 0
            if (failedAttempts >= 9) {
                logoutUseCase()
            } else {
                passcodeRepository.setFailedAttempts(failedAttempts + 1)
            }
        }
    }
}
