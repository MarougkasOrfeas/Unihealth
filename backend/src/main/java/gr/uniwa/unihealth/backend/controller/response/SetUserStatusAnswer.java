package gr.uniwa.unihealth.backend.controller.response;


import gr.uniwa.unihealth.backend.model.enums.UserStatus;

public record SetUserStatusAnswer(UserStatus userStatus, String reasonForStatusChange) {
}
