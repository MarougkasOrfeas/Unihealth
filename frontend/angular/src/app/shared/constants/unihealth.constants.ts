export const UNIHEALTH_CONSTANTS = {
    // Base app context
    UNIHEALTH_API: '/api',
    // User endpoints
    USER_API: {
        BASE: '/user',
        PAGE: '/user/_page',
        JUST_LOGGED_IN: '/user/_just_logged_in',
        SET_USER_STATUS: '/user/_set_user_status',
        SUGGEST_USERNAME: '/user/_suggest_username',
        CHECK_USERNAME_EXISTS: '/user/_check_username_exists',
    },

    TRANSLATION_API: {
        BASE: '/translation',
        LANGUAGES: '/translation/languages',
    },

    // Routes Keywords
    ROUTE_HOME: 'home',

    // Toast defaults
    TOAST: {
        DURATION_MS: 5000,
        HORIZONTAL_POSITION: 'right' as const,
        VERTICAL_POSITION: 'bottom' as const,
        ACTION_LABEL: 'Schließen',
    },
}
