import {Injectable} from '@angular/core';
import {BaseService} from "./base.service";
import {DepartmentDTO} from "../interfaces/department";
import {Observable} from "rxjs";


@Injectable({providedIn: 'root'})
export class DepartmentService extends BaseService<DepartmentDTO> {

    constructor() {
        super('department');
    }

    getActiveDepartments(): Observable<DepartmentDTO[]> {
        return this.httpClient.get<DepartmentDTO[]>(`${this.basePath}/_active`);
    }

    getActiveDepartmentsByGroup(groupName: string): Observable<DepartmentDTO[]> {
        return this.httpClient.get<DepartmentDTO[]>(
            `${this.basePath}/_active/by-group/${encodeURIComponent(groupName)}`
        );
    }

    /**
     * Activates or deactivates a department. A dedicated endpoint because `active` is deliberately
     * ignored by the update mapper — the status is not an editable form field.
     */
    setActive(id: string, active: boolean): Observable<boolean> {
        return this.httpClient.put<boolean>(`${this.basePath}/_set_department_status`, {id, active});
    }
}
