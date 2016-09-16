package org.yamj.core.api.json;

import static org.yamj.core.tools.ExceptionTools.isLockingError;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.yamj.core.api.model.ApiStatus;

@ControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ApiExceptionHandler.class);
    
    @ResponseBody
    @ExceptionHandler(Exception.class)
    @SuppressWarnings("unused")
    public ResponseEntity<Object> defaultException(Exception ex, WebRequest request) { //NOSONAR
        if (isLockingError(ex)) {
            LOG.trace("Locking error occured", ex);
            return new ResponseEntity<Object>(ApiStatus.locked(), HttpStatus.LOCKED);
        }
        
        LOG.error("Handle api exception", ex);
        final Throwable rootCause = ExceptionUtils.getRootCause(ex);
        final ApiStatus apiStatus = ApiStatus.internalError(rootCause == null ? ex.getMessage() : rootCause.getMessage());
        return new ResponseEntity<Object>(apiStatus, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}