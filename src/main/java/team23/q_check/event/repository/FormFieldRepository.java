package team23.q_check.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import team23.q_check.event.domain.model.form.FormField;

import java.util.List;

public interface FormFieldRepository extends JpaRepository<FormField, Long> {
    List<FormField> findAllByEvent_IdOrderBySortOrderAsc(Long eventId);

    void deleteAllByEvent_Id(Long eventId);
}
