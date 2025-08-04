package chpay.DatabaseHandler.transactiondb.repositories;

import chpay.DatabaseHandler.transactiondb.entities.Statistic;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StatisticsRepository extends JpaRepository<Statistic, UUID> {
  Optional<Statistic> getFirstByTypeIsAndDate(Statistic.StatisticType type, LocalDate date);

  List<Statistic> getAllByTypeIsAndTimestampBetweenOrderByDate(
      Statistic.StatisticType type, LocalDateTime timestampAfter, LocalDateTime timestampBefore);

  @Query(
      "SELECT SUM(s.amount) FROM Statistic s WHERE s.type=:type AND s.timestamp BETWEEN :timestampAfter AND :timestampBefore")
  BigDecimal getSumBetweenDates(
      Statistic.StatisticType type, LocalDateTime timestampAfter, LocalDateTime timestampBefore);
}
