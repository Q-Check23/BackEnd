package team23.q_check.event.domain.model;

import jakarta.persistence.*;
import team23.q_check.identity.domain.model.User;

import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_logs")
public class AttendanceLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private Registration registration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "checked_in_by_user_id", nullable = false)
    private User checkedInByUser;

    @Column(name = "check_in_time", nullable = false, updatable = false)
    private LocalDateTime checkInTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "method", nullable = false, length = 20)
    private AttendanceMethod method;

    protected AttendanceLog() {
    }

    public AttendanceLog(
            Event event,
            Registration registration,
            User checkedInByUser,
            LocalDateTime checkInTime,
            AttendanceMethod method
    ) {
        this.event = event;
        this.registration = registration;
        this.checkedInByUser = checkedInByUser;
        this.checkInTime = checkInTime;
        this.method = method;
    }

    public Long getId() {
        return id;
    }

    public AttendanceMethod getMethod() {
        return method;
    }
}
