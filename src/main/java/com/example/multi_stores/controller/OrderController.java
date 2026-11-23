package com.example.multi_stores.controller;

import com.example.multi_stores.entity.Order;
import com.example.multi_stores.entity.OrderStatus;
import com.example.multi_stores.entity.User;
import com.example.multi_stores.repository.UserRepository;
import com.example.multi_stores.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    public OrderController(OrderService orderService, UserRepository userRepository) {
        this.orderService = orderService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(
            @RequestBody CreateOrderRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Long customerId = getUserId(userDetails);
        if (customerId == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not authenticated"));
        }
        if (request.getStoreId() == null || request.getItems() == null || request.getItems().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Store and items are required"));
        }
        if (request.getShipping() == null || blank(request.getShipping().getName())
                || blank(request.getShipping().getCity()) || blank(request.getShipping().getAddress())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name, city and address for delivery are required"));
        }
        try {
            List<OrderService.OrderItemInput> items = request.getItems().stream()
                    .map(i -> new OrderService.OrderItemInput(i.getProductId(), i.getQuantity(), i.getSize(), i.getColor()))
                    .collect(Collectors.toList());
            Order order = orderService.createOrder(customerId, request.getStoreId(), items, request.getShippingFee(),
                    new OrderService.Shipping(request.getShipping().getName(), request.getShipping().getCity(),
                            request.getShipping().getAddress(), request.getShipping().getNotes()));
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "id", order.getId(),
                    "totalPrice", order.getTotalPrice(),
                    "status", order.getStatus().name()
            ));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/my")
    public List<OrderSummary> myOrders(@AuthenticationPrincipal UserDetails userDetails) {
        Long customerId = getUserId(userDetails);
        if (customerId == null) return List.of();
        return orderService.findByCustomerId(customerId).stream()
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    @GetMapping("/admin/all")
    public ResponseEntity<?> allOrdersForAdmin(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getAuthUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not authenticated"));
        }
        try {
            return ResponseEntity.ok(orderService.findAllOrdersDetailedForAdmin(user.getRole()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/fulfillment")
    public ResponseEntity<Map<String, Object>> updateFulfillment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not authenticated"));
        }
        try {
            Order order = orderService.updateFulfillment(id, body == null ? null : body.get("fulfillment"), user.getRole());
            return ResponseEntity.ok(Map.of("id", order.getId(), "fulfillment", order.getFulfillment()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private static boolean blank(String s) {
        return s == null || s.isBlank();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteOrderAsAdmin(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        User user = getAuthUser(userDetails);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Not authenticated"));
        }
        try {
            orderService.deleteOrderAsPrivilegedUser(id, user.getId(), user.getRole());
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
    }

    private OrderSummary toSummary(Order o) {
        return new OrderSummary(o.getId(), o.getStoreId(), o.getTotalPrice(), o.getStatus());
    }

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) return null;
        return ((com.example.multi_stores.entity.User) userDetails).getId();
    }

    private User getAuthUser(UserDetails userDetails) {
        if (userDetails == null) return null;
        if (userDetails instanceof User) return (User) userDetails;
        String email = userDetails.getUsername();
        if (email == null || email.isBlank()) return null;
        return userRepository.findByEmail(email).orElse(null);
    }

    public static class CreateOrderRequest {
        private Long storeId;
        private List<OrderItemDto> items;
        /** Sent with one order of the checkout (the first); the others have none. */
        private java.math.BigDecimal shippingFee;
        private ShippingDto shipping;

        public CreateOrderRequest() {}

        public CreateOrderRequest(Long storeId, List<OrderItemDto> items) {
            this.storeId = storeId;
            this.items = items;
        }

        public Long getStoreId() { return storeId; }
        public void setStoreId(Long storeId) { this.storeId = storeId; }
        public List<OrderItemDto> getItems() { return items; }
        public void setItems(List<OrderItemDto> items) { this.items = items; }
        public java.math.BigDecimal getShippingFee() { return shippingFee; }
        public void setShippingFee(java.math.BigDecimal shippingFee) { this.shippingFee = shippingFee; }
        public ShippingDto getShipping() { return shipping; }
        public void setShipping(ShippingDto shipping) { this.shipping = shipping; }
    }

    public static class ShippingDto {
        private String name;
        private String city;
        private String address;
        private String notes;

        public ShippingDto() {}

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getAddress() { return address; }
        public void setAddress(String address) { this.address = address; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
    }

    public static class OrderItemDto {
        private Long productId;
        private int quantity;
        private String size;
        private String color;

        public OrderItemDto() {}

        public Long getProductId() { return productId; }
        public void setProductId(Long productId) { this.productId = productId; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public String getSize() { return size; }
        public void setSize(String size) { this.size = size; }
        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }

    public static class OrderSummary {
        private Long id;
        private Long storeId;
        private java.math.BigDecimal totalPrice;
        private OrderStatus status;

        public OrderSummary(Long id, Long storeId, java.math.BigDecimal totalPrice, OrderStatus status) {
            this.id = id;
            this.storeId = storeId;
            this.totalPrice = totalPrice;
            this.status = status;
        }

        public Long getId() { return id; }
        public Long getStoreId() { return storeId; }
        public java.math.BigDecimal getTotalPrice() { return totalPrice; }
        public OrderStatus getStatus() { return status; }
    }
}
