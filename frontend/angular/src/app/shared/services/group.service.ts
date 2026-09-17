import {Injectable} from "@angular/core";
import {BaseService} from "./base.service";
import {GroupDTO} from "../interfaces/group";
import {Observable} from "rxjs";


@Injectable({providedIn: 'root'})
export class GroupService extends BaseService<GroupDTO> {

    constructor() {
        super('group');
    }

    getActiveGroups(): Observable<GroupDTO[]> {
        return this.httpClient.get<GroupDTO[]>(
            `${BaseService.CONTEXT_PATH}/group/_active`
        );
    }

    /**
     * Activates or deactivates a school. A dedicated endpoint because `active` is deliberately
     * ignored by the update mapper — the status is not an editable form field.
     */
    setActive(id: string, active: boolean): Observable<boolean> {
        return this.httpClient.put<boolean>(`${this.basePath}/_set_group_status`, {id, active});
    }

}
