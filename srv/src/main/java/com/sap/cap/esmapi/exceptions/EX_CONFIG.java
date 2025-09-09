package com.sap.cap.esmapi.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
public class EX_CONFIG extends RuntimeException
{
    public EX_CONFIG(String message)
    {

        super(message);
        log.error(message);
    }
}
