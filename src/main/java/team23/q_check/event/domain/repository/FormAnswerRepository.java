package team23.q_check.event.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.event.domain.model.form.FormAnswer;

import java.util.List;

public interface FormAnswerRepository extends JpaRepository<FormAnswer, Long> {
    List<FormAnswer> findAllByRegistration_Id(Long registrationId);

    List<FormAnswer> findAllByRegistration_Event_Id(Long eventId);

    void deleteAllByRegistration_Event_Id(Long eventId);
}
