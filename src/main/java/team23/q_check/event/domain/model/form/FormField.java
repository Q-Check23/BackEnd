package team23.q_check.event.domain.model.form;

import jakarta.persistence.*;
import team23.q_check.event.domain.model.Event;

@Entity
@Table(name = "form_fields")
public class FormField {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @Column(nullable = false)
    private Long sortOrder;

    @Column(nullable = false, length = 255)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FieldType fieldType;

    // SELECT인 경우
    @Column(columnDefinition = "json", nullable = true)
    private String options;

    @Column(nullable = false)
    private Boolean isRequired = false;
}
