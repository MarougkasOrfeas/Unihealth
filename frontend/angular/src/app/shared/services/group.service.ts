import {Injectable} from "@angular/core";
import {BaseService} from "./base.service";
import {GroupDTO} from "../interfaces/group";


@Injectable({providedIn: 'root'})
export class GroupService extends BaseService<GroupDTO> {

    constructor() {
        super('group');
    }
}
