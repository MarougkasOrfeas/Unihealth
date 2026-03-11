import {Injectable} from '@angular/core';
import {BaseService} from "./base.service";
import {DepartmentDTO} from "../interfaces/department";


@Injectable({providedIn: 'root'})
export class DepartmentService extends BaseService<DepartmentDTO> {

    constructor() {
        super('department');
    }
}
