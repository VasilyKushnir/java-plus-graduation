package ewm.request.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(
        name = "participation_requests",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_request",
                columnNames = {"requester_id", "event_id"}
        )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long requesterId;

    private Long eventId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RequestStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime created;

    @PrePersist
    void onCreate() {
        if (created == null) {
            created = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        }
        if (status == null) {
            status = RequestStatus.PENDING;
        }
    }
}