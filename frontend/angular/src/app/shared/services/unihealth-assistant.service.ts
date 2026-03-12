import {Injectable} from "@angular/core";
import {Observable} from "rxjs";
import {UnihealthAssistantContent} from "../interfaces/unihealth-assistant";
import {BaseService} from "./base.service";

class UnihealthAssistantEndpoints {

    static readonly GET_ASSISTANT_CONTENT = BaseService.CONTEXT_PATH + '/assistant/_content';
}


@Injectable({providedIn: 'root'})
export class UnihealthAssistantService extends BaseService<UnihealthAssistantContent> {

    constructor() {
        super('assistant')
    }

    getContent(): Observable<UnihealthAssistantContent> {
        return this.httpClient.post<UnihealthAssistantContent>(
            UnihealthAssistantEndpoints.GET_ASSISTANT_CONTENT,
            {}
        );
    }
}
