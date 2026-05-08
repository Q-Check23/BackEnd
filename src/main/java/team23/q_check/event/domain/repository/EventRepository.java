package team23.q_check.event.domain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import team23.q_check.event.domain.model.Event;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

    long countByClub_Id(Long clubId);

    @Query("SELECT e FROM Event e WHERE e.club.id IN :clubIds AND YEAR(e.startTime) = :year AND MONTH(e.startTime) = :month ORDER BY e.startTime ASC")
    List<Event> findByClubIdsAndYearMonth(@Param("clubIds") List<Long> clubIds,
                                          @Param("year") int year,
                                          @Param("month") int month);

    @Query("""
            SELECT e FROM Event e
            WHERE e.id IN :eventIds
            AND (LOWER(e.title) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(COALESCE(e.location, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                 OR LOWER(e.club.name) LIKE LOWER(CONCAT('%', :query, '%')))
            ORDER BY e.startTime ASC
            """)
    List<Event> searchInUserEvents(@Param("eventIds") List<Long> eventIds,
                                   @Param("query") String query);

    @Query("""
            SELECT e FROM Event e
            WHERE e.id IN :eventIds
            AND (:startDate IS NULL OR e.startTime >= :startDate)
            AND (:endDate IS NULL OR e.startTime <= :endDate)
            AND (:eventName IS NULL OR LOWER(e.title) LIKE LOWER(CONCAT('%', :eventName, '%')))
            AND (:clubName IS NULL OR LOWER(e.club.name) LIKE LOWER(CONCAT('%', :clubName, '%')))
            ORDER BY e.startTime ASC
            """)
    List<Event> filterUserEvents(@Param("eventIds") List<Long> eventIds,
                                 @Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate,
                                 @Param("eventName") String eventName,
                                 @Param("clubName") String clubName);
}
