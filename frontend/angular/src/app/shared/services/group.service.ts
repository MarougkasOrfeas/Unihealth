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

}
