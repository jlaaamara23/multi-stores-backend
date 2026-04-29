package com.example.multi_stores.service;

import com.example.multi_stores.entity.*;
import com.example.multi_stores.repository.OrderItemRepository;
import com.example.multi_stores.repository.OrderRepository;
import com.example.multi_stores.repository.ProductRepository;
import com.example.multi_stores.repository.StoreRepository;
import com.example.multi_stores.repository.UserRepository;
import com.example.multi_stores.util.WeightPriceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final ProductService productService;
    private final InvoiceService invoiceService;

    public OrderService(OrderRepository orderRepository,
                        OrderItemRepository orderItemRepository,
                        ProductRepository productRepository,
                        StoreRepository storeRepository,
                        UserRepository userRepository,
                        ProductService productService,
                        InvoiceService invoiceService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.productService = productService;
        this.invoiceService = invoiceService;
    }

    @Transactional
    public Order createOrder(Long customerId, Long storeId, List<OrderItemInput> items) {
        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemInput input : items) {
            Product product = productRepository.findById(input.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + input.getProductId()));
            int available = productService.getAvailableQuantity(product.getId(), input.getSize(), input.getColor());
            if (available < input.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for product: " + product.getName()
                        + (input.getSize() != null ? " (size " + input.getSize() + ")" : "")
                        + (input.getColor() != null ? " (color " + input.getColor() + ")" : ""));
            }
            Store store = storeRepository.findById(product.getStoreId())
                    .orElseThrow(() -> new IllegalArgumentException("Store not found: " + product.getStoreId()));
            BigDecimal unitPrice = product.getPrice();
            if (input.getSize() != null && !input.getSize().isBlank()
                    && (store.getCategory() == StoreCategory.SPICES || store.getCategory() == StoreCategory.GROCERY)) {
                var packKg = WeightPriceUtil.parsePackWeightKg(input.getSize());
                if (packKg.isPresent()) {
                    unitPrice = product.getPrice().multiply(packKg.get()).setScale(2, RoundingMode.HALF_UP);
                }
            }
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(input.getQuantity()));
            totalPrice = totalPrice.add(lineTotal);
            orderItems.add(OrderItem.builder()
                    .productId(product.getId())
                    .quantity(input.getQuantity())
                    .unitPrice(unitPrice)
                    .size(input.getSize())
                    .color(input.getColor())
                    .build());
        }

        BigDecimal minimumOrder = new BigDecimal("80");
        if (totalPrice.compareTo(minimumOrder) < 0) {
            throw new IllegalStateException("Minimum order is ₪80. Your total is ₪" + totalPrice.setScale(2, java.math.RoundingMode.HALF_UP) + ".");
        }

        Order order = Order.builder()
                .customerId(customerId)
                .storeId(storeId)
                .totalPrice(totalPrice)
                .status(OrderStatus.PENDING)
                .createdAt(Instant.now())
                .build();
        order = orderRepository.save(order);

        for (OrderItem oi : orderItems) {
            oi.setOrderId(order.getId());
            orderItemRepository.save(oi);
        }
        order.setItems(orderItems);
        return order;
    }

    @Transactional
    public void completeOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Order is not pending: " + orderId);
        }
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        for (OrderItem item : items) {
            productService.decrementStock(item.getProductId(), item.getSize(), item.getColor(), item.getQuantity());
        }
        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);
        log.info("Order {} completed, stock updated.", orderId);
        try {
            invoiceService.createAndSendInvoice(orderId);
        } catch (Exception e) {
            log.error("Failed to create/send invoice for order {}: {}", orderId, e.getMessage());
        }
    }

    public List<Order> findByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId);
    }

    public List<Order> findByStoreId(Long storeId) {
        return orderRepository.findByStoreId(storeId);
    }

    public List<AdminOrderDetails> findAllOrdersDetailedForAdmin(UserRole userRole) {
        if (userRole != UserRole.ADMIN) {
            throw new IllegalStateException("Only admin can view all orders");
        }

        List<Order> orders = new ArrayList<>(orderRepository.findAll());
        orders.sort(Comparator.comparing(Order::getId).reversed());
        if (orders.isEmpty()) return Collections.emptyList();

        List<Long> orderIds = orders.stream().map(Order::getId).collect(Collectors.toList());
        List<Long> customerIds = orders.stream().map(Order::getCustomerId).distinct().collect(Collectors.toList());
        List<Long> storeIds = orders.stream().map(Order::getStoreId).distinct().collect(Collectors.toList());

        List<OrderItem> orderItems = orderItemRepository.findByOrderIdIn(orderIds);
        Map<Long, List<OrderItem>> itemsByOrderId = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        List<Long> productIds = orderItems.stream().map(OrderItem::getProductId).distinct().collect(Collectors.toList());

        Map<Long, Product> productById = productRepository.findByIdIn(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));
        Map<Long, User> userById = new HashMap<>();
        for (User u : userRepository.findAllById(customerIds)) {
            userById.put(u.getId(), u);
        }
        Map<Long, Store> storeById = new HashMap<>();
        for (Store s : storeRepository.findAllById(storeIds)) {
            storeById.put(s.getId(), s);
        }

        List<AdminOrderDetails> result = new ArrayList<>();
        for (Order o : orders) {
            User customer = userById.get(o.getCustomerId());
            Store store = storeById.get(o.getStoreId());
            List<AdminOrderItemDetails> itemDtos = new ArrayList<>();
            for (OrderItem item : itemsByOrderId.getOrDefault(o.getId(), Collections.emptyList())) {
                Product product = productById.get(item.getProductId());
                String productName = product != null ? product.getName() : ("Product #" + item.getProductId());
                itemDtos.add(new AdminOrderItemDetails(
                        item.getProductId(),
                        productName,
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSize(),
                        item.getColor()
                ));
            }
            result.add(new AdminOrderDetails(
                    o.getId(),
                    o.getCustomerId(),
                    customer != null ? customer.getEmail() : null,
                    customer != null ? customer.getPhone() : null,
                    o.getStoreId(),
                    store != null ? store.getName() : null,
                    o.getTotalPrice(),
                    o.getStatus(),
                    o.getCreatedAt(),
                    itemDtos
            ));
        }

        return result;
    }

    @Transactional
    public void deleteOrderAsPrivilegedUser(Long orderId, Long userId, UserRole userRole) {
        if (userRole != UserRole.ADMIN && userRole != UserRole.OWNER) {
            throw new IllegalStateException("Only admin or owner can delete orders");
        }
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        if (userRole == UserRole.OWNER) {
            Store store = storeRepository.findById(order.getStoreId())
                    .orElseThrow(() -> new IllegalArgumentException("Store not found: " + order.getStoreId()));
            if (!store.getOwnerId().equals(userId)) {
                throw new IllegalStateException("You can delete orders only for your own stores");
            }
        }

        // Keep accounting/history safe: only not-yet-fulfilled orders can be removed.
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CANCELLED) {
            throw new IllegalStateException("Only PENDING or CANCELLED orders can be deleted");
        }

        orderItemRepository.deleteByOrderId(orderId);
        orderRepository.delete(order);
    }

    public static class OrderItemInput {
        private Long productId;
        private int quantity;
        private String size;
        private String color;

        public OrderItemInput() {}

        public OrderItemInput(Long productId, int quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public OrderItemInput(Long productId, int quantity, String size) {
            this.productId = productId;
            this.quantity = quantity;
            this.size = size;
        }

        public OrderItemInput(Long productId, int quantity, String size, String color) {
            this.productId = productId;
            this.quantity = quantity;
            this.size = size;
            this.color = color;
        }

        public Long getProductId() { return productId; }
        public int getQuantity() { return quantity; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    public static class AdminOrderItemDetails {
        private Long productId;
        private String productName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private String size;
        private String color;

        public AdminOrderItemDetails(Long productId, String productName, Integer quantity, BigDecimal unitPrice, String size, String color) {
            this.productId = productId;
            this.productName = productName;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.size = size;
            this.color = color;
        }

        public Long getProductId() { return productId; }
        public String getProductName() { return productName; }
        public Integer getQuantity() { return quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public String getSize() { return size; }
        public String getColor() { return color; }
    }

    public static class AdminOrderDetails {
        private Long id;
        private Long customerId;
        private String customerEmail;
        private String customerPhone;
        private Long storeId;
        private String storeName;
        private BigDecimal totalPrice;
        private OrderStatus status;
        private Instant createdAt;
        private List<AdminOrderItemDetails> items;

        public AdminOrderDetails(Long id, Long customerId, String customerEmail, String customerPhone,
                                 Long storeId, String storeName, BigDecimal totalPrice, OrderStatus status,
                                 Instant createdAt, List<AdminOrderItemDetails> items) {
            this.id = id;
            this.customerId = customerId;
            this.customerEmail = customerEmail;
            this.customerPhone = customerPhone;
            this.storeId = storeId;
            this.storeName = storeName;
            this.totalPrice = totalPrice;
            this.status = status;
            this.createdAt = createdAt;
            this.items = items;
        }

        public Long getId() { return id; }
        public Long getCustomerId() { return customerId; }
        public String getCustomerEmail() { return customerEmail; }
        public String getCustomerPhone() { return customerPhone; }
        public Long getStoreId() { return storeId; }
        public String getStoreName() { return storeName; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public OrderStatus getStatus() { return status; }
        public Instant getCreatedAt() { return createdAt; }
        public List<AdminOrderItemDetails> getItems() { return items; }
    }
}
