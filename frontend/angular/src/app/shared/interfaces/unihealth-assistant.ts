import {BaseEntity} from "./baseEntity";

export interface UnihealthAssistantItem extends BaseEntity {
    section: 'FAQ' | 'SERVICES' | 'EMERGENCY' | 'CLINICS';
    title: string;
    brief?: string;
    content?: string;
    actionLabel?: string;
    actionValue?: string;
    displayOrder?: number;
    active?: boolean;
    expanded?: boolean;
}

export interface UnihealthAssistantContent {
    faq: UnihealthAssistantItem[];
    services: UnihealthAssistantItem[];
    emergency: UnihealthAssistantItem[];
    clinics: UnihealthAssistantItem[];
}
