package ewm.event.model;

import ewm.category.model.Category;
import ewm.common.model.Location;
import ewm.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* === Relations === */

    @ManyToOne(optional = false)
    @JoinColumn(name = "initiator_id")
    private User initiator;

    @ManyToOne(optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    /* === Basic fields === */

    @Column(nullable = false, length = 2000)
    private String annotation;

    @Column(nullable = false, length = 7000)
    private String description;

    @Column(nullable = false, length = 120)
    private String title;

    @Embedded
    private Location location;

    @Column(nullable = false)
    private Boolean paid = false;

    @Column(nullable = false)
    private Integer participantLimit = 0;

    @Column(nullable = false)
    private Boolean requestModeration = true;

    @Column(nullable = false)
    private Long confirmedRequests = 0L;

    /* === Dates === */

    @Column(nullable = false)
    private LocalDateTime eventDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdOn;

    private LocalDateTime publishedOn;

    /* === State === */

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventState state;
}
