package ewm.common.error;

import ewm.common.dto.ApiError;
import ewm.common.exception.BadRequestException;
import ewm.common.exception.ConflictException;
import ewm.common.exception.NotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult().getFieldErrors().stream().map(e -> e.getField() + ": " + e.getDefaultMessage()).collect(Collectors.toList());

        ApiError body = ApiError.builder().errors(errors).message("Validation failed").reason("Incorrectly made request.").status(HttpStatus.BAD_REQUEST.name()).timestamp(LocalDateTime.now()).build();

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> errors = ex.getConstraintViolations().stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).collect(Collectors.toList());

        ApiError body = ApiError.builder().errors(errors).message("Validation failed").reason("Incorrectly made request.").status(HttpStatus.BAD_REQUEST.name()).timestamp(LocalDateTime.now()).build();

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiError> handleMissingParam(MissingServletRequestParameterException ex) {
        ApiError body = ApiError.builder().errors(List.of(ex.getMessage())).message("Validation failed").reason("Incorrectly made request.").status(HttpStatus.BAD_REQUEST.name()).timestamp(LocalDateTime.now()).build();
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException ex) {
        return buildError(ex.getMessage(), "Не найден необходимый объект.", HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException ex) {
        return buildError(ex.getMessage(), "Не выполнены условия для запрашиваемой операции.", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        return buildError(ex.getMessage(), "Некорректный запрос", HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiError> handleBadRequest(BadRequestException ex) {
        return buildError(ex.getMessage(), "Некорректный запрос", HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<ApiError> buildError(String message, String reason, HttpStatus status) {
        ApiError error = ApiError.builder().message(message).reason(reason).status(status.name()).timestamp(LocalDateTime.now()).build();
        return new ResponseEntity<>(error, status);
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ApiError> handleOther(Throwable ex) {
        log.error("Unhandled error", ex);
        return buildError(ex.getMessage(), "Internal server error.", HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
