import {Injectable} from '@angular/core';
import {BaseService} from "./base.service";
import {DepartmentDTO} from "../interfaces/department";
import {Observable} from "rxjs";
import {GroupDTO} from "../interfaces/group";


@Injectable({providedIn: 'root'})
export class DepartmentService extends BaseService<DepartmentDTO> {

    constructor() {
        super('department');
    }

    getActiveDepartments(): Observable<DepartmentDTO[]> {
        return this.httpClient.get<DepartmentDTO[]>(
            `${BaseService.CONTEXT_PATH}/_active`
        );
    }

    getActiveDepartmentsByGroup(groupName: string): Observable<DepartmentDTO[]> {
        return this.httpClient.get<DepartmentDTO[]>(
            `${BaseService.CONTEXT_PATH}/department/_active/by-group/${encodeURIComponent(groupName)}`
        );
    }
}
