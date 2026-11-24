package io.github.debajdutta.orderservice.config;

import io.github.debajdutta.orderservice.order.domain.Order;
import io.github.debajdutta.orderservice.order.domain.OrderStatus;
import io.github.debajdutta.orderservice.order.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Tops the orders table up to app.seed-order-count (default 100) on startup.
 * Existing rows are never touched; restarting does nothing once the target is reached.
 *
 * Data is varied on purpose so list/search/paging can be exercised:
 * 20 customers, 15 products, all five statuses, quantities 1-10,
 * and createdAt spread over the last ~120 days.
 * The random generator is seeded, so the same data is produced every time.
 *
 * Turn off with app.seed-data=false (tests do this).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed-data", havingValue = "true", matchIfMissing = true)
public class SampleDataLoader implements ApplicationRunner {

    private static final List<String> CUSTOMERS = List.of(
            "Alice Johnson", "Bob Smith", "Carol White", "David Brown", "Emma Davis",
            "Frank Miller", "Grace Wilson", "Henry Moore", "Isabella Taylor", "Jack Anderson",
            "Karen Thomas", "Liam Jackson", "Mia Harris", "Noah Martin", "Olivia Thompson",
            "Priya Sharma", "Quentin Garcia", "Rashmi Rao", "Samuel Lee", "alice cooper");

    private record Product(String name, String price) {
    }

    private static final List<Product> PRODUCTS = List.of(
            new Product("Laptop Stand", "49.99"), new Product("Mechanical Keyboard", "129.00"),
            new Product("USB-C Hub", "34.50"), new Product("27-inch Monitor", "249.99"),
            new Product("Wireless Mouse", "29.99"), new Product("Noise Cancelling Headphones", "199.00"),
            new Product("Webcam 1080p", "59.90"), new Product("Desk Lamp", "20.00"),
            new Product("Ergonomic Chair", "349.00"), new Product("Standing Desk", "499.00"),
            new Product("Laptop Sleeve 15%", "24.99"), new Product("HDMI Cable 2m", "9.99"),
            new Product("Portable SSD 1TB", "89.00"), new Product("Docking Station", "179.00"),
            new Product("Mouse Pad XL", "14.50"));

    /** Weighted so there are plenty of open orders: 30% CREATED, 20% each CONFIRMED/SHIPPED/DELIVERED, 10% CANCELLED. */
    private static final OrderStatus[] STATUS_BAG = {
            OrderStatus.CREATED, OrderStatus.CREATED, OrderStatus.CREATED,
            OrderStatus.CONFIRMED, OrderStatus.CONFIRMED,
            OrderStatus.SHIPPED, OrderStatus.SHIPPED,
            OrderStatus.DELIVERED, OrderStatus.DELIVERED,
            OrderStatus.CANCELLED};

    private final OrderRepository repository;
    private final JdbcTemplate jdbc;
    private final int targetCount;

    public SampleDataLoader(OrderRepository repository, JdbcTemplate jdbc,
                            @Value("${app.seed-order-count:100}") int targetCount) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.targetCount = targetCount;
    }

    @Override
    public void run(ApplicationArguments args) {
        long existing = repository.count();
        if (existing >= targetCount) {
            log.info("Seed skipped: {} orders already present (target {})", existing, targetCount);
            return;
        }
        int toCreate = (int) (targetCount - existing);
        Random random = new Random(42L + existing);

        List<Order> orders = new ArrayList<>(toCreate);
        for (int i = 0; i < toCreate; i++) {
            Product product = PRODUCTS.get(random.nextInt(PRODUCTS.size()));
            int quantity = random.nextInt(10) < 7 ? 1 + random.nextInt(3) : 4 + random.nextInt(7); // mostly small
            Order order = Order.create(
                    CUSTOMERS.get(random.nextInt(CUSTOMERS.size())),
                    product.name(), quantity, new BigDecimal(product.price()), "seed");
            order.setStatus(STATUS_BAG[random.nextInt(STATUS_BAG.length)]); // seed data may skip transitions
            orders.add(order);
        }
        List<Order> saved = repository.saveAll(orders);

        // @CreationTimestamp always stamps "now", so back-date the new rows with plain SQL
        // to give date-range searches something to work with.
        Instant now = Instant.now();
        List<Object[]> dates = new ArrayList<>(saved.size());
        for (Order o : saved) {
            Instant created = now.minus(Duration.ofMinutes(random.nextInt(120 * 24 * 60)));
            long ageMinutes = Duration.between(created, now).toMinutes();
            Instant updated = created.plus(Duration.ofMinutes(random.nextLong(Math.min(ageMinutes, 5 * 24 * 60) + 1)));
            dates.add(new Object[]{Timestamp.from(created), Timestamp.from(updated), o.getId()});
        }
        jdbc.batchUpdate("UPDATE orders SET created_at = ?, updated_at = ? WHERE id = ?", dates);

        log.info("Seeded {} orders (now {} total)", toCreate, repository.count());
    }
}
