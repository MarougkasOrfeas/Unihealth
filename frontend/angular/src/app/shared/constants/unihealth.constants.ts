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
    ROUTE_USERS: 'users',
    ROUTE_GROUPS: 'groups',
    ROUTE_DEPARTMENTS: 'departments',
    ROUTE_USER_DETAILS: 'user-details',

    /** Lexicon keys for entity nouns, interpolated into the `global.message.*` templates. */
    ENTITY: {
        USER: 'global.entity.user',
        GROUP: 'global.entity.group',
        DEPARTMENT: 'global.entity.department',
        EXAM_FILE: 'global.entity.examFile',
        CONVERSATION: 'global.entity.conversation',
        SURVEY: 'global.entity.survey',
    },

    LARGE_EXPORT_THRESHOLD: 100,

    // Toast defaults
    TOAST: {
        DURATION_MS: 5000,
        HORIZONTAL_POSITION: 'right' as const,
        VERTICAL_POSITION: 'bottom' as const,
        ACTION_LABEL_KEY: 'global.close',
    },

    PATTERNS: {
        PHONE_PTN: String.raw`^\+?(\(\+?\d+\)|\d+)([\s\-\/]{0,1}(\(\d+\)|\d+))*?(;ext=\d{3,6})?$`,
        EMAIL_PTN: String.raw`^(?!.*?\.\.)[a-zA-Z0-9\._+-]{1,64}@[a-zA-Z0-9\.-]+\.+[a-zA-Z]{2,}$`,
        USERNAME_PTN: String.raw`^[a-zA-Z0-9\._-]+$`,
        FIRSTNAME_LASTNAME_PTN: new RegExp(String.raw`^\p{L}+(?:[ '-]\p{L}+)*$`, 'u'),
    },

    // Only the operations this application actually performs. The port this came from also carried
    // add / deactivate / reactivate / copy / column-config / external-reference messages, none of
    // which have a caller or a lexicon entry here.
    MESSAGE_KEYS: {
        CREATE_SUCCESS: 'global.message.create.success',
        CREATE_ERROR: 'global.message.create.error',
        UPDATE_SUCCESS: 'global.message.update.success',
        UPDATE_ERROR: 'global.message.update.error',
        DELETE_SUCCESS: 'global.message.delete.success',
        DELETE_ERROR: 'global.message.delete.error',
        DELETE_CONFIRM_TITLE: 'global.message.delete.confirm.title',
        DELETE_CONFIRM_CONTENT: 'global.message.delete.confirm.content',
        DELETE_CONFIRM_CONFIRM: 'global.message.delete.confirm.confirm',
        DISCARD_CONTENT: 'global.message.discard.content',
        SAVE_SUCCESS: 'global.message.save.success',
        SAVE_ERROR: 'global.message.save.error',
        EXPORT_PROGRESS: 'global.message.export.progress',
        EXPORT_SUCCESS: 'global.message.export.success',
        EXPORT_ERROR: 'global.message.export.error',
        NOT_FOUND: 'global.message.not.found',
    },
}
