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

    protected FormField() {
    }

    public FormField(
            Event event,
            Long sortOrder,
            String label,
            FieldType fieldType,
            String options,
            Boolean isRequired
    ) {
        this.event = event;
        this.sortOrder = sortOrder;
        this.label = label;
        this.fieldType = fieldType;
        this.options = options;
        this.isRequired = isRequired;
    }

    public Long getId() {
        return id;
    }

    public Event getEvent() {
        return event;
    }

    public Long getSortOrder() {
        return sortOrder;
    }

    public String getLabel() {
        return label;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public String getOptions() {
        return options;
    }

    public Boolean getIsRequired() {
        return isRequired;
    }
}
