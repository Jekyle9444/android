package mega.privacy.android.domain.entity

enum class RegexPatternType {
    RESTRICTED,
    WHITELISTED_URL,
    FILE_LINK,
    CONFIRMATION_LINK,
    FOLDER_LINK,
    CHAT_LINK,
    PASSWORD_LINK,
    ACCOUNT_INVITATION_LINK,
    EXPORT_MASTER_KEY_LINK,
    NEW_MESSAGE_CHAT_LINK,
    CANCEL_ACCOUNT_LINK,
    VERIFY_CHANGE_MAIL_LINK,
    RESET_PASSWORD_LINK,
    PENDING_CONTACTS_LINK,
    HANDLE_LINK,
    CONTACT_LINK,
    MEGA_DROP_LINK,
    MEGA_FILE_REQUEST_LINK,
    MEGA_BLOG_LINK,
    REVERT_CHANGE_PASSWORD_LINK,
    EMAIL_VERIFY_LINK,
    WEB_SESSION_LINK,
    BUSINESS_INVITE_LINK,
    ALBUM_LINK
}