package tradeService;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    Trade findByFromAndTo(String from, String to);
}
