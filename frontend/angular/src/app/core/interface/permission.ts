export enum Permission {
    ADMIN = 'ADMIN',
}

export const Categories = {
    SYSTEM_ADMINISTRATION: [Permission.ADMIN],
} as const;

export type CategoryName = keyof typeof Categories;

const getEnumKeyByValue = (val: string) => {
    return Object.keys(Permission).find(key => Permission[key as keyof typeof Permission] === val);
};

export const CategoriesPermissions = Object.entries(Categories).reduce((acc, [catName, perms]) => {

    const permissionObject: Record<string, string> = {};

    perms.forEach(p => {
        const key = getEnumKeyByValue(p);
        if (key) {
            permissionObject[key] = 'permission.' + key;
        }
    });

    catName = 'permission.categories.' + catName;

    acc[catName] = permissionObject;
    return acc;
}, {} as Record<string, any>);
