package com.parkyc.poelens.common.dto;

public class CommonDTO {

    public record Response<T>(String code, String message, T returnObject) {
    }

    public record Exception(String code, String message) {
    }
}
