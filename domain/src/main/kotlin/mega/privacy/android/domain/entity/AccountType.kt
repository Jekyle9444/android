package mega.privacy.android.domain.entity

/**
 * Enum class for account types [UserAccount]
 */
enum class AccountType {

    /**
     * FREE
     */
    FREE,

    /**
     * PRO_LITE
     */
    PRO_LITE,

    /**
     * PRO_I
     */
    PRO_I,

    /**
     * PRO_II
     */
    PRO_II,

    /**
     * PRO_III
     */
    PRO_III,

    /**
     * PRO_FLEXI
     */
    PRO_FLEXI,

    /**
     * BUSINESS
     */
    BUSINESS,

    /**
     *
     */
    UNKNOWN;

    /**
     * Convenient method to check paid account type
     */
    val isPaid: Boolean get() = this != FREE && this != UNKNOWN
}