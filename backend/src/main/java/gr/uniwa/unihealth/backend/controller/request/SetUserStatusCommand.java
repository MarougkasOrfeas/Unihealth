package gr.uniwa.unihealth.backend.controller.request;


import gr.uniwa.unihealth.backend.model.enums.UserStatus;

public record SetUserStatusCommand(String id, UserStatus userStatus, String reason) {
}
