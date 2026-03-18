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
    RSS_API: {
        BASE: '/rss',
        HOME: '/rss/_home'
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

    PATTERNS: {
        PHONE_PTN: String.raw`^\+?(\(\+?\d+\)|\d+)([\s\-\/]{0,1}(\(\d+\)|\d+))*?(;ext=\d{3,6})?$`,
        EMAIL_PTN: String.raw`^(?!.*?\.\.)[a-zA-Z0-9\._+-]{1,64}@[a-zA-Z0-9\.-]+\.+[a-zA-Z]{2,}$`,
        USERNAME_PTN: String.raw`^[a-zA-Z0-9\._-]+$`,
        FIRSTNAME_LASTNAME_PTN: new RegExp(String.raw`^\p{L}+(?:[ '-]\p{L}+)*$`, 'u'),
    }
}
