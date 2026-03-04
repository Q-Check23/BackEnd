package team23.q_check.event.domain.model.form;

import jakarta.persistence.*;
import team23.q_check.event.domain.model.Registration;

@Entity
@Table(name = "form_answers", uniqueConstraints = {
        @UniqueConstraint(
                name = "uq_form_answers_registration_field",
                columnNames = {"registration_id", "field_id"}
        )
})
public class FormAnswer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private Registration registration;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "field_id", nullable = false)
    private FormField field;

    @Lob
    @Column(name = "answer_value", nullable = true)
    private String answerValue;

    protected FormAnswer() {
    }

    public FormAnswer(Registration registration, FormField field, String answerValue) {
        this.registration = registration;
        this.field = field;
        this.answerValue = answerValue;
    }

    public Long getId() {
        return id;
    }

    public Registration getRegistration() {
        return registration;
    }

    public FormField getField() {
        return field;
    }

    public String getAnswerValue() {
        return answerValue;
    }
}
