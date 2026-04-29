package com.example.multi_stores.service;

import com.example.multi_stores.entity.*;
import com.example.multi_stores.repository.InvoiceRepository;
import com.example.multi_stores.repository.OrderItemRepository;
import com.example.multi_stores.repository.OrderRepository;
import com.example.multi_stores.repository.ProductRepository;
import com.example.multi_stores.repository.StoreRepository;
import com.example.multi_stores.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class InvoiceService {

    private static final Logger log = LoggerFactory.getLogger(InvoiceService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final EmailService emailService;

    public InvoiceService(OrderRepository orderRepository,
                          OrderItemRepository orderItemRepository,
                          ProductRepository productRepository,
                          StoreRepository storeRepository,
                          com.example.multi_stores.repository.UserRepository userRepository,
                          InvoiceRepository invoiceRepository,
                          EmailService emailService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.userRepository = userRepository;
        this.invoiceRepository = invoiceRepository;
        this.emailService = emailService;
    }

    /**
     * Generates invoice HTML, saves it to the database, and sends it to the customer by email.
     * Called after order is confirmed (PAID).
     */
    @Transactional
    public void createAndSendInvoice(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        User customer = userRepository.findById(order.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found for order"));
        Store store = storeRepository.findById(order.getStoreId())
                .orElseThrow(() -> new IllegalArgumentException("Store not found for order"));
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);

        if (invoiceRepository.existsByOrderId(orderId)) {
            log.info("Invoice already exists for order {}, skipping.", orderId);
            return;
        }

        String html = buildInvoiceHtml(order, store, customer, items);
        String invoiceNumber = "INV-" + orderId;

        Invoice invoice = new Invoice();
        invoice.setOrderId(orderId);
        invoice.setCustomerEmail(customer.getEmail());
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setHtmlContent(html);
        invoice.setCreatedAt(Instant.now());
        invoiceRepository.save(invoice);

        String subject = "Your invoice " + invoiceNumber + " – " + store.getName();
        emailService.sendHtmlEmail(customer.getEmail(), subject, html);
    }

    private String buildInvoiceHtml(Order order, Store store, User customer, List<OrderItem> items) {
        String storeName = escapeHtml(store.getName());
        String customerEmail = escapeHtml(customer.getEmail());
        String dateStr = order.getCreatedAt() != null ? DATE_FORMAT.format(order.getCreatedAt()) : "";
        String invoiceNumber = "INV-" + order.getId();

        StringBuilder rows = new StringBuilder();
        for (OrderItem item : items) {
            String productName = productRepository.findById(item.getProductId())
                    .map(Product::getName)
                    .map(InvoiceService::escapeHtml)
                    .orElse("Product #" + item.getProductId());
            String size = item.getSize() != null && !item.getSize().isBlank() ? " (" + escapeHtml(item.getSize()) + ")" : "";
            String color = item.getColor() != null && !item.getColor().isBlank() ? " · " + escapeHtml(item.getColor()) : "";
            BigDecimal lineTotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            rows.append("<tr>")
                    .append("<td>").append(productName).append(size).append(color).append("</td>")
                    .append("<td style=\"text-align:right\">").append(item.getQuantity()).append("</td>")
                    .append("<td style=\"text-align:right\">₪").append(item.getUnitPrice().setScale(2, java.math.RoundingMode.HALF_UP)).append("</td>")
                    .append("<td style=\"text-align:right\">₪").append(lineTotal.setScale(2, java.math.RoundingMode.HALF_UP)).append("</td>")
                    .append("</tr>");
        }

        String totalStr = order.getTotalPrice() != null ? order.getTotalPrice().setScale(2, java.math.RoundingMode.HALF_UP).toString() : "0.00";

        return "<!DOCTYPE html><html><head><meta charset=\"UTF-8\"><style>" +
                "body{font-family:sans-serif;max-width:600px;margin:20px auto;padding:20px;}" +
                "h1{font-size:1.25rem;border-bottom:1px solid #ddd;padding-bottom:8px;}" +
                "table{width:100%;border-collapse:collapse;margin:16px 0;}" +
                "th,td{border:1px solid #ddd;padding:8px;text-align:left;}" +
                "th{background:#f5f5f5;}" +
                ".total{font-weight:bold;font-size:1.1rem;margin-top:12px;}" +
                "</style></head><body>" +
                "<h1>Invoice " + invoiceNumber + "</h1>" +
                "<p><strong>Store:</strong> " + storeName + "</p>" +
                "<p><strong>Date:</strong> " + dateStr + "</p>" +
                "<p><strong>Customer:</strong> " + customerEmail + "</p>" +
                "<table><thead><tr><th>Item</th><th style=\"text-align:right\">Qty</th><th style=\"text-align:right\">Unit price</th><th style=\"text-align:right\">Total</th></tr></thead><tbody>" +
                rows +
                "</tbody></table>" +
                "<p class=\"total\">Total: ₪" + totalStr + "</p>" +
                "<p style=\"color:#666;font-size:0.9rem;margin-top:24px;\">Thank you for your purchase.</p>" +
                "</body></html>";
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
