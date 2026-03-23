import {Component, inject, OnInit} from "@angular/core";
import {FormBuilder, ReactiveFormsModule, Validators} from "@angular/forms";
import {Router} from "@angular/router";
import {NgIf} from "@angular/common";
import {UserService} from "../../shared/services/user.service";
import {User} from "../../shared/interfaces/user";
import {AuthService} from "../../core/auth/auth.service";
import {TranslatePipe} from "@ngx-translate/core";

@Component({
    selector: "app-profile",
    standalone: true,
    imports: [
        ReactiveFormsModule,
        NgIf,
        TranslatePipe
    ],
    templateUrl: "./profile.html",
    styleUrl: "./profile.scss",
})
export class Profile implements OnInit {

    private fb = inject(FormBuilder);
    private router = inject(Router);
    private userService = inject(UserService);
    protected userId!: string;
    user: User | null = null;
    editMode = false;
    private authService = inject(AuthService);

    form = this.fb.group({
        firstname: ['', Validators.required],
        lastname: ['', Validators.required],
        email: ['', [Validators.required, Validators.email]]
    });

    ngOnInit(): void {
        this.loadUser();
    }

    private loadUser(): void {
        this.userService.getUser().subscribe({
            next: (user) => {
                this.user = user;

                this.form.patchValue({
                    firstname: user.firstname,
                    lastname: user.lastname,
                    email: user.email
                });
            },
            error: () => {
                console.error("Failed to load user");
            }
        });
    }

    onEdit(): void {
        this.editMode = true;
    }

    onSave(): void {

        if (this.form.invalid || !this.user) {
            this.form.markAllAsTouched();
            return;
        }

        const value = this.form.getRawValue();

        const updatedUser = {
            ...this.user,
            firstname: value.firstname ?? '',
            lastname: value.lastname ?? '',
            email: value.email ?? ''
        };

        this.editMode = false;

        this.userService.update(this.user.id, updatedUser).subscribe({
            next: () => {
                this.user = updatedUser;
                this.editMode = false;
            },
            error: (err) => {
                console.error('Failed to update profile', err);
            }
        });
    }

    onCancel(): void {
        this.editMode = false;

        if (!this.user) return;

        this.form.patchValue({
            firstname: this.user.firstname,
            lastname: this.user.lastname,
            email: this.user.email
        });
    }

    onEditHealthForm(): void {
        this.router.navigate(['/profile/form']);
    }

    onLogout(): void {
        this.authService.logout();
    }
}
