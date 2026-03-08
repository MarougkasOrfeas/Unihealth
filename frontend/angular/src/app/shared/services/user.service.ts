import {Injectable} from '@angular/core';
import {BaseService} from './base.service';
import {User} from '../interfaces/user';

@Injectable({providedIn: 'root'})
export class UserService extends BaseService<User> {

  constructor() {
    super('user');
  }

  justLoggedIn(){
  }
}