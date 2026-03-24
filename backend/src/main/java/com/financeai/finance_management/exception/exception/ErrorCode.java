package com.financeai.finance_management.exception.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    UNAUTHENTICATED("405", "Unauthenticated", HttpStatus.UNAUTHORIZED),
    USER_NOT_EXISTED("404", "User Not Existed", HttpStatus.NOT_FOUND),
    PASSWORD_INVALID("403", "Password must be at least {min} characters", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID("401", "Username must be at least {min} characters", HttpStatus.BAD_REQUEST),
    UNCATEGORIED_EXCEPTION("999", "Uncategoried Exception", HttpStatus.INTERNAL_SERVER_ERROR),
    USER_EXISTED("402", "User already existed", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("406", "Unauthorized", HttpStatus.FORBIDDEN),
    INVALID_DOB("407", "You must be at least {min}", HttpStatus.BAD_REQUEST),
    MAX_SIZE_UPLOAD_FILE("424", "Max file size exceeded", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_BUSINESS_FLOW("BUSINESS_01", "INVALID BUSINESS_FLOW", HttpStatus.BAD_REQUEST),
    INVALID_REQUEST("BUSINESS_02", "INVALID REQUEST", HttpStatus.BAD_REQUEST),

    //
    VALUE_TOO_LARGE("VALUE_01", "Invalid value (too large)", HttpStatus.BAD_REQUEST),
    VALUE_TOO_SMALL("VALUE_02", "Invalid value (too small)", HttpStatus.BAD_REQUEST),

    //COMMON
    DATASOURCE_ALREADY_EXISTS("COMMON_001", "Data source already exists", HttpStatus.BAD_REQUEST),
    DATASOURCE_NOT_FOUND("COMMON_002", "Data source not found", HttpStatus.BAD_REQUEST),
    ERR_SYSTEM("ERR_SYSTEM", "System error", HttpStatus.INTERNAL_SERVER_ERROR),
    DATASOURCE_ENUM_NOT_FOUND("ENUM_01", "Data source not found", HttpStatus.BAD_REQUEST ),
    BUSINESS_INVALID_SEQUENCE("BUSINESS_001", "Business invalid sequence", HttpStatus.BAD_REQUEST ),
    //RESTAURANT
    UNAUTHORIZED_TO_UPDATE_THIS_RESOURCE("UNAUTHORIZED_001", "Unauthorized to update this resource", HttpStatus.UNAUTHORIZED),
    RESTAURANT_NOT_FOUND("RESTAURANT_001", "Restaurant not found", HttpStatus.NOT_FOUND),

    //Order
    DISH_NOT_FOUND("DISH_001", "Dish not found", HttpStatus.NOT_FOUND),
    DISH_SERVING_STOPPED("DISH_002", "This dish is no longer being served", HttpStatus.SERVICE_UNAVAILABLE ),
    ORDER_NOT_FOUND("ORDER_001", "Order not found", HttpStatus.NOT_FOUND),
    DRONE_UNAVAILABLE("DRONE_001", "Drone unavailable", HttpStatus.SERVICE_UNAVAILABLE),

    //PAYMENT
    PAYMENT_NOT_FOUND("PAYMENT_001", "Payment not found", HttpStatus.NOT_FOUND),
    REFUND_FAILED("PATMENT_002", "Refund failed", HttpStatus.SERVICE_UNAVAILABLE),
    ;

    ErrorCode(String code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }


    private String code;
    private String message;
    private HttpStatusCode statusCode;
}
