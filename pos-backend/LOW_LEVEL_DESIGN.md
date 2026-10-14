# Low-Level Design (LLD) Document
## POS System - Multi-Tenant Retail Management Platform

---

## 1. SYSTEM OVERVIEW

**Project Type:** Spring Boot 3.5.3 REST API
**Architecture:** Multi-Tenant SaaS POS System
**Database:** MySQL with Hibernate JPA
**Authentication:** JWT-based Stateless Authentication
**Package Base:** `com.zosh`

### Core Capabilities
- Multi-tenant store and branch management
- Role-based access control (7 user roles)
- Real-time inventory management
- Order processing and POS operations
- Shift management and cashier reconciliation
- Financial analytics and reporting
- Subscription management with payment integration
- Customer relationship management
- Refund and return processing

---

## 2. ARCHITECTURE LAYERS

```
┌─────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                    │
│              (19 REST Controllers + DTOs)                │
└───────────────────┬─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│                    SERVICE LAYER                         │
│          (13 Service Interfaces + Implementations)       │
│              Business Logic & Validation                 │
└───────────────────┬─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│                 PERSISTENCE LAYER                        │
│            (15 JPA Repositories + 15 Entities)           │
└───────────────────┬─────────────────────────────────────┘
                    │
┌───────────────────▼─────────────────────────────────────┐
│                    DATABASE LAYER                        │
│                   MySQL Database                         │
└─────────────────────────────────────────────────────────┘

Cross-Cutting Concerns:
├── Security (JWT + Spring Security)
├── Exception Handling (Global Exception Handler)
├── Validation (Bean Validation)
└── Mapping (12 Entity-DTO Mappers)
```

---

## 3. DOMAIN MODEL

### 3.1 Core Entities

#### User Management
```
User
├── id: Long (PK)
├── email: String (Unique)
├── password: String (BCrypt)
├── fullName: String
├── role: UserRole (Enum)
├── store: Store (ManyToOne)
├── branch: Branch (ManyToOne)
├── createdAt: LocalDateTime
└── updatedAt: LocalDateTime

PasswordResetToken
├── id: Long (PK)
├── token: String
├── user: User (OneToOne)
└── expiryDate: LocalDateTime
```

#### Store & Branch Hierarchy
```
Store
├── id: Long (PK)
├── name: String
├── storeAdmin: User (OneToOne)
├── contact: StoreContact (Embedded)
├── status: StoreStatus (Enum)
├── branches: List<Branch> (OneToMany)
├── employees: List<User> (OneToMany)
└── products: List<Product> (OneToMany)

Branch
├── id: Long (PK)
├── name: String
├── location: String
├── store: Store (ManyToOne)
├── manager: User (ManyToOne)
├── employees: List<User> (OneToMany)
└── inventories: List<Inventory> (OneToMany)

StoreContact (Embeddable)
├── email: String
├── phone: String
└── address: String
```

#### Product Catalog
```
Product
├── id: Long (PK)
├── name: String
├── sku: String (Unique)
├── price: Double
├── category: Category (ManyToOne)
├── store: Store (ManyToOne)
├── image: String (URL)
└── description: String

Category
├── id: Long (PK)
├── name: String
├── store: Store (ManyToOne)
└── products: List<Product> (OneToMany)

Inventory
├── id: Long (PK)
├── product: Product (ManyToOne)
├── branch: Branch (ManyToOne)
├── quantity: Integer
├── minimumStockLevel: Integer
└── lastRestockedAt: LocalDateTime
```

#### Order Processing
```
Order
├── id: Long (PK)
├── orderNumber: String (Unique)
├── orderDate: LocalDateTime
├── totalAmount: Double
├── discount: Double
├── finalAmount: Double
├── paymentType: PaymentType (Enum)
├── status: OrderStatus (Enum)
├── branch: Branch (ManyToOne)
├── cashier: User (ManyToOne)
├── customer: Customer (ManyToOne)
└── orderItems: List<OrderItem> (OneToMany, Cascade)

OrderItem
├── id: Long (PK)
├── product: Product (ManyToOne)
├── quantity: Integer
├── unitPrice: Double
├── subtotal: Double
└── order: Order (ManyToOne)

Customer
├── id: Long (PK)
├── name: String
├── email: String
├── phone: String
├── address: String
└── orders: List<Order> (OneToMany)
```

#### Financial Management
```
ShiftReport
├── id: Long (PK)
├── shiftDate: LocalDate
├── startTime: LocalDateTime
├── endTime: LocalDateTime
├── cashier: User (ManyToOne)
├── branch: Branch (ManyToOne)
├── openingBalance: Double
├── closingBalance: Double
├── totalSales: Double
├── totalRefunds: Double
├── cashSales: Double
├── cardSales: Double
├── upiSales: Double
├── orderCount: Integer
├── refunds: List<Refund> (OneToMany)
└── paymentSummary: PaymentSummary (Embedded)

Refund
├── id: Long (PK)
├── refundDate: LocalDateTime
├── amount: Double
├── reason: String
├── order: Order (ManyToOne)
├── cashier: User (ManyToOne)
├── branch: Branch (ManyToOne)
└── shiftReport: ShiftReport (ManyToOne)

PaymentSummary (Embeddable)
├── totalCash: Double
├── totalCard: Double
├── totalUpi: Double
└── totalAmount: Double
```

#### Subscription System
```
SubscriptionPlan
├── id: Long (PK)
├── name: String
├── price: Double
├── billingCycle: BillingCycle (Enum)
├── maxBranches: Integer
├── maxEmployees: Integer
└── features: String (JSON)

Subscription
├── id: Long (PK)
├── store: Store (ManyToOne)
├── plan: SubscriptionPlan (ManyToOne)
├── status: SubscriptionStatus (Enum)
├── startDate: LocalDate
├── endDate: LocalDate
└── autoRenew: Boolean

PaymentOrder
├── id: Long (PK)
├── orderId: String (Gateway ID)
├── amount: Double
├── paymentMethod: PaymentMethod (Enum)
├── status: PaymentOrderStatus (Enum)
├── user: User (ManyToOne)
├── subscription: SubscriptionPlan (ManyToOne)
└── createdAt: LocalDateTime
```

### 3.2 Enumerations

```java
UserRole: ADMIN, STORE_ADMIN, STORE_MANAGER, BRANCH_MANAGER,
          BRANCH_ADMIN, BRANCH_CASHIER, CUSTOMER

OrderStatus: COMPLETED, PENDING, REFUNDED, CANCELLED

PaymentType: CARD, UPI, CASH

StoreStatus: ACTIVE, PENDING, BLOCKED

SubscriptionStatus: TRIAL, ACTIVE, EXPIRED, CANCELLED

BillingCycle: MONTHLY, YEARLY

PaymentMethod: RAZORPAY, STRIPE

PaymentOrderStatus: PENDING, COMPLETED, FAILED
```

---

## 4. API LAYER DESIGN

### 4.1 Controller Architecture

#### Authentication & User Management
```
AuthController (/auth)
├── POST /signup              → Register new user
├── POST /login               → Login & JWT generation
├── POST /forgot-password     → Request password reset
└── POST /reset-password      → Reset password with token

UserController (/api/users)
└── User CRUD operations
```

#### Core POS Operations
```
OrderController (/api/orders)
├── POST /                          → Create order
├── GET /{id}                       → Get order details
├── GET /branch/{branchId}          → Branch orders (with filters)
├── GET /cashier/{cashierId}        → Cashier's orders
├── GET /today/branch/{branchId}    → Today's orders
├── GET /customer/{customerId}      → Customer order history
├── GET /recent/{branchId}          → Last 5 recent orders
└── DELETE /{id}                    → Delete order

ProductController (/api/products)
├── POST /                          → Create product
├── GET /{id}                       → Get product
├── PATCH /{id}                     → Update product
├── DELETE /{id}                    → Delete product
├── GET /store/{storeId}            → Store products
└── GET /store/{storeId}/search     → Search by keyword

InventoryController (/api/inventories)
├── POST /                          → Create inventory
├── PUT /{id}                       → Update stock
├── GET /branch/{branchId}          → Branch inventory
└── GET /product/{productId}        → Product inventory
```

#### Store & Branch Management
```
StoreController (/api/stores)
├── POST /              → Create store
├── GET /{id}           → Get store
├── PUT /{id}           → Update store
└── DELETE /            → Delete store

BranchController (/api/branches)
├── POST /                      → Create branch
├── GET /{id}                   → Get branch
├── GET /store/{storeId}        → List branches
├── PUT /{id}                   → Update branch
└── DELETE /{id}                → Delete branch
```

#### Financial & Reporting
```
RefundController (/api/refunds)
├── POST /                      → Create refund
├── GET /cashier/{cashierId}    → By cashier
├── GET /shift/{shiftReportId}  → By shift
├── GET /branch/{branchId}      → By branch
└── GET /date-range             → By date range

ShiftReportController (/api/shift-reports)
├── POST /start                 → Start shift
├── PATCH /end                  → End shift
├── GET /{id}                   → Get shift report
├── GET /cashier/{cashierId}    → Cashier shifts
└── GET /branch/{branchId}      → Branch shifts
```

#### Analytics
```
BranchAnalyticsController (/api/branch/analytics)
├── GET /{branchId}/daily-sales
├── GET /{branchId}/top-products
├── GET /{branchId}/cashier-performance
├── GET /{branchId}/category-sales
└── GET /{branchId}/overview

StoreAnalyticsController (/api/store/analytics)
├── GET /{storeAdminId}/overview
├── GET /{storeAdminId}/sales-trends
├── GET /{storeAdminId}/sales/monthly
├── GET /{storeAdminId}/sales/daily
├── GET /{storeAdminId}/sales/category
├── GET /{storeAdminId}/sales/payment-method
├── GET /{storeAdminId}/sales/branch
└── GET /{storeAdminId}/alerts

AdminDashboardController (/api/super-admin)
├── GET /dashboard/summary
├── GET /dashboard/store-registrations
├── GET /dashboard/store-status
└── PUT /stores/{id}/moderate
```

#### Subscription & Payments
```
SubscriptionController (/api/subscriptions)
├── POST /subscribe     → New subscription
├── POST /upgrade       → Upgrade plan
└── PUT /{id}/activate  → Activate

PaymentController (/api/payments)
├── POST /create        → Create payment link
└── PATCH /proceed      → Process payment
```

### 4.2 DTO Design Pattern

**Request DTOs:**
- `LoginDto` - Login credentials
- `ForgotPasswordRequest` - Password reset request
- `ResetPasswordRequest` - Password reset submission

**Response DTOs:**
- `AuthResponse` - JWT token + user info
- `ProductDTO`, `OrderDTO`, `BranchDTO`, etc.

**Analytics DTOs:**
- `DailySalesDTO`, `ProductPerformanceDTO`
- `CashierPerformanceDTO`, `CategorySalesDTO`
- `BranchDashboardOverviewDTO`

**Mapper Pattern:**
```java
// Static mapping methods
ProductDTO dto = ProductMapper.toDto(product);
Product entity = ProductMapper.toEntity(productDTO);
```

---

## 5. SERVICE LAYER DESIGN

### 5.1 Service Architecture

```
Interface-Based Design:
├── Service Interface (contract)
└── ServiceImpl (implementation)

Example:
public interface OrderService {
    OrderDTO createOrder(OrderDTO orderDTO);
    OrderDTO getOrderById(Long id);
    List<OrderDTO> getOrdersByBranch(Long branchId);
}

@Service
public class OrderServiceImpl implements OrderService {
    // Implementation with business logic
}
```

### 5.2 Key Services

**Authentication & Security:**
- `AuthService` - Login, signup, password reset, JWT generation
- `UserService` - User CRUD, role management, JWT parsing
- `EmailService` - Password reset emails

**Core Business:**
- `ProductService` - Product CRUD, search, store-wise products
- `CategoryService` - Category CRUD
- `OrderService` - Order creation, retrieval, filtering
- `InventoryService` - Stock management, branch-wise tracking
- `CustomerService` - Customer CRUD, search

**Store Management:**
- `StoreService` - Store CRUD, admin assignment, approval/blocking
- `BranchService` - Branch CRUD, manager assignment
- `EmployeeService` - Employee management

**Financial:**
- `RefundService` - Refund creation, retrieval by various filters
- `ShiftReportService` - Shift start/end, sales calculation
- `PaymentService` - Payment gateway integration (Razorpay/Stripe)
- `SubscriptionService` - Subscription CRUD, upgrade/cancel

**Analytics:**
- `BranchAnalyticsService` - Branch-level KPIs
- `StoreAnalyticsService` - Store-level analytics
- `AdminDashboardService` - System-wide statistics

---

## 6. DATA ACCESS LAYER

### 6.1 Repository Pattern

All repositories extend `JpaRepository<Entity, Long>`

**Custom Query Examples:**

```java
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // Method name queries
    List<Order> findByBranchId(Long branchId);
    List<Order> findByCashierId(Long cashierId);
    List<Order> findByCustomerId(Long customerId);

    // JPQL queries with aggregation
    @Query("SELECT SUM(o.finalAmount) FROM Order o " +
           "WHERE o.branch.id = :branchId AND DATE(o.orderDate) = CURRENT_DATE")
    Double getTodayTotalSales(@Param("branchId") Long branchId);

    // DTO projection
    @Query("SELECT new com.zosh.payload.DailySalesDTO(DATE(o.orderDate), " +
           "SUM(o.finalAmount), COUNT(o)) FROM Order o " +
           "WHERE o.branch.id = :branchId GROUP BY DATE(o.orderDate)")
    List<DailySalesDTO> getDailySales(@Param("branchId") Long branchId);
}
```

### 6.2 Complex Queries

**ProductRepository:**
- SKU-based lookup
- Store-wise product retrieval
- Keyword search

**InventoryRepository:**
- Branch-wise stock queries
- Low stock alerts
- Product availability checks

**RefundRepository:**
- Date range queries
- Branch/cashier/shift filtering

**BranchRepository:**
- Store-wise branches
- Top performing branches
- Branches with no sales alerts

---

## 7. SECURITY DESIGN

### 7.1 Authentication Flow

```
┌──────────┐         ┌──────────────┐         ┌────────────┐
│  Client  │         │   Backend    │         │  Database  │
└────┬─────┘         └──────┬───────┘         └─────┬──────┘
     │                      │                       │
     │ POST /auth/login     │                       │
     │ {email, password}    │                       │
     ├─────────────────────>│                       │
     │                      │ Find user by email    │
     │                      ├──────────────────────>│
     │                      │<──────────────────────┤
     │                      │ User entity           │
     │                      │                       │
     │                      │ Verify BCrypt         │
     │                      │ password              │
     │                      │                       │
     │                      │ Generate JWT          │
     │                      │ (24h expiry)          │
     │                      │                       │
     │ JWT + User info      │                       │
     │<─────────────────────┤                       │
     │                      │                       │
     │ GET /api/orders      │                       │
     │ Header: Bearer {JWT} │                       │
     ├─────────────────────>│                       │
     │                      │ JwtValidator          │
     │                      │ validates token       │
     │                      │                       │
     │                      │ Set SecurityContext   │
     │                      │                       │
     │                      │ Process request       │
     │                      │                       │
```

### 7.2 JWT Configuration

**Token Structure:**
```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "email": "user@example.com",
    "authorities": "ROLE_BRANCH_CASHIER",
    "iat": 1234567890,
    "exp": 1234654290
  }
}
```

**Implementation:**
```java
// JwtProvider.java
public String generateToken(Authentication auth) {
    return Jwts.builder()
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + 86400000))
        .claim("email", auth.getName())
        .claim("authorities", getAuthorities(auth))
        .signWith(key)
        .compact();
}

// JwtValidator.java (Filter)
@Override
protected void doFilterInternal(HttpServletRequest request,
                                HttpServletResponse response,
                                FilterChain filterChain) {
    String jwt = extractJwt(request);
    if (jwt != null) {
        String email = extractEmail(jwt);
        Authentication auth = createAuthentication(email, jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
    filterChain.doFilter(request, response);
}
```

### 7.3 Security Configuration

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        http
            .sessionManagement(session ->
                session.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/api/super-admin/**").hasRole("ADMIN")
                .requestMatchers("/api/**").authenticated()
                .anyRequest().permitAll())
            .addFilterBefore(jwtValidator, UsernamePasswordAuthenticationFilter.class)
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

### 7.4 Authorization Utility

```java
// SecurityUtil.java
public class SecurityUtil {

    public static void checkAuthority(Store store, User user) {
        if (user.getRole() == UserRole.ROLE_ADMIN) return;

        if (user.getRole() == UserRole.ROLE_STORE_ADMIN) {
            if (!store.getStoreAdmin().getId().equals(user.getId())) {
                throw new AccessDeniedException("Unauthorized access");
            }
        } else if (user.getStore() == null ||
                   !user.getStore().getId().equals(store.getId())) {
            throw new AccessDeniedException("Unauthorized access");
        }
    }

    // Similar methods for Product, Branch, Inventory
}
```

---

## 8. EXCEPTION HANDLING

### 8.1 Global Exception Handler

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserException.class)
    public ResponseEntity<ExceptionResponse> handleUserException(
            UserException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ExceptionResponse(ex.getMessage(),
                                       HttpStatus.BAD_REQUEST));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ExceptionResponse> handleNotFound(
            ResourceNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ExceptionResponse(ex.getMessage(),
                                       HttpStatus.NOT_FOUND));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ExceptionResponse> handleAccessDenied(
            AccessDeniedException ex) {
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(new ExceptionResponse(ex.getMessage(),
                                       HttpStatus.FORBIDDEN));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ExceptionResponse> handleDataIntegrity(
            DataIntegrityViolationException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ExceptionResponse("Data integrity violation",
                                       HttpStatus.CONFLICT));
    }
}
```

### 8.2 Custom Exceptions

```java
public class UserException extends RuntimeException {
    public UserException(String message) {
        super(message);
    }
}

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resource, Long id) {
        super(resource + " not found with id: " + id);
    }
}
```

---

## 9. DESIGN PATTERNS USED

### 9.1 Architectural Patterns

**1. Layered Architecture**
```
Presentation → Service → Repository → Database
- Clear separation of concerns
- Dependency direction: top to bottom
- Each layer depends only on layer below
```

**2. Repository Pattern**
```java
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByStoreId(Long storeId);
}
// Abstracts data access logic
```

**3. Service Layer Pattern**
```java
public interface ProductService {
    ProductDTO createProduct(ProductDTO dto);
}

@Service
public class ProductServiceImpl implements ProductService {
    // Business logic implementation
}
```

**4. DTO Pattern**
```java
// Decouples API from domain model
public class ProductDTO {
    private Long id;
    private String name;
    private Double price;
    // No JPA annotations, clean API contract
}
```

### 9.2 Design Patterns

**5. Mapper Pattern**
```java
public class ProductMapper {
    public static ProductDTO toDto(Product entity) { }
    public static Product toEntity(ProductDTO dto) { }
}
```

**6. Dependency Injection**
```java
@RequiredArgsConstructor
public class OrderServiceImpl {
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    // Constructor injection via Lombok
}
```

**7. Strategy Pattern (Payment Gateway)**
```java
if (paymentMethod == PaymentMethod.RAZORPAY) {
    // Razorpay implementation
} else if (paymentMethod == PaymentMethod.STRIPE) {
    // Stripe implementation
}
```

**8. Builder Pattern**
```java
@Builder
public class OrderDTO {
    // Lombok generates builder
}

OrderDTO order = OrderDTO.builder()
    .orderNumber("ORD-001")
    .totalAmount(100.0)
    .build();
```

**9. AOP (Aspect-Oriented Programming)**
```java
@ControllerAdvice  // Cross-cutting concern
public class GlobalExceptionHandler { }
```

**10. Filter Chain Pattern**
```java
// JWT validation filter in Spring Security filter chain
public class JwtValidator extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(...) {
        // Validate JWT, set SecurityContext
        filterChain.doFilter(request, response);
    }
}
```

---

## 10. DATABASE DESIGN

### 10.1 Key Relationships

```sql
-- User belongs to Store and Branch
User
  ├─> Store (ManyToOne)
  └─> Branch (ManyToOne)

-- Store has multiple Branches, Products, Users
Store
  ├─> Branch (OneToMany)
  ├─> Product (OneToMany)
  └─> User (OneToMany)

-- Order processing chain
Order
  ├─> OrderItem (OneToMany, Cascade ALL)
  ├─> Branch (ManyToOne)
  ├─> User/Cashier (ManyToOne)
  └─> Customer (ManyToOne)

-- Inventory tracking
Inventory
  ├─> Branch (ManyToOne)
  └─> Product (ManyToOne)
  -- Composite unique: (branch_id, product_id)

-- Shift management
ShiftReport
  ├─> User/Cashier (ManyToOne)
  ├─> Branch (ManyToOne)
  └─> Refund (OneToMany)
```

### 10.2 Indexing Strategy

**Primary Indexes:**
- All `id` fields (auto-indexed as PK)

**Unique Constraints:**
- `User.email`
- `Product.sku`
- `Order.orderNumber`
- `Inventory(branch_id, product_id)` - composite

**Foreign Key Indexes:**
- All `@ManyToOne` relationships auto-indexed
- Examples: `Order.branch_id`, `Order.cashier_id`, `Product.store_id`

**Recommended Additional Indexes:**
```sql
-- Frequently queried fields
CREATE INDEX idx_order_date ON orders(order_date);
CREATE INDEX idx_order_branch_date ON orders(branch_id, order_date);
CREATE INDEX idx_shift_cashier_date ON shift_reports(cashier_id, shift_date);
```

### 10.3 Cascade Operations

```java
@OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
private List<OrderItem> orderItems;
// Deleting Order deletes all OrderItems
```

---

## 11. EXTERNAL INTEGRATIONS

### 11.1 Payment Gateway Integration

**Razorpay SDK:**
```java
RazorpayClient razorpay = new RazorpayClient(apiKey, apiSecret);

JSONObject options = new JSONObject();
options.put("amount", amount * 100); // paise
options.put("currency", "INR");

com.razorpay.Order razorpayOrder = razorpay.orders.create(options);
```

**Stripe SDK:**
```java
Stripe.apiKey = stripeApiKey;

SessionCreateParams params = SessionCreateParams.builder()
    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
    .addLineItem(...)
    .setMode(SessionCreateParams.Mode.PAYMENT)
    .setSuccessUrl(successUrl)
    .setCancelUrl(cancelUrl)
    .build();

Session session = Session.create(params);
```

### 11.2 Email Service

```java
@Service
public class EmailServiceImpl {

    @Autowired
    private JavaMailSender mailSender;

    public void sendPasswordResetEmail(String email, String token) {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        helper.setTo(email);
        helper.setSubject("Password Reset Request");
        helper.setText(buildEmailContent(token), true); // HTML

        mailSender.send(message);
    }
}
```

**Configuration:**
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

---

## 12. CONFIGURATION

### 12.1 Application Configuration

```yaml
# application.yml
server:
  port: 5000

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/pos
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
        format_sql: true

  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_APP_PASSWORD}

jwt:
  secret: ${JWT_SECRET_KEY}
  expiration: 86400000  # 24 hours

razorpay:
  key:
    id: ${RAZORPAY_KEY_ID}
    secret: ${RAZORPAY_KEY_SECRET}

stripe:
  api:
    key: ${STRIPE_API_KEY}
```

### 12.2 CORS Configuration

```java
@Bean
CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(Arrays.asList(
        "http://localhost:3000",
        "http://localhost:5173",
        "https://your-production-domain.com"
    ));
    configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH"));
    configuration.setAllowedHeaders(Arrays.asList("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
}
```

---

## 13. KEY WORKFLOWS

### 13.1 Order Creation Workflow

```
1. Client sends POST /api/orders with OrderDTO
   ├─> Contains: orderItems, paymentType, discount, customerId

2. OrderController receives request
   ├─> Validates JWT token
   ├─> Extracts cashier from SecurityContext
   └─> Calls OrderService.createOrder()

3. OrderService.createOrder()
   ├─> Validates customer exists
   ├─> For each OrderItem:
   │   ├─> Fetch Product
   │   ├─> Calculate subtotal = quantity × unitPrice
   │   └─> Update Inventory (reduce stock)
   ├─> Calculate totalAmount = sum of subtotals
   ├─> Apply discount
   ├─> Set finalAmount = totalAmount - discount
   ├─> Generate orderNumber
   ├─> Set branch from cashier
   └─> Save Order (cascades to OrderItems)

4. Return OrderDTO to client
```

### 13.2 Shift Management Workflow

```
START SHIFT:
1. POST /api/shift-reports/start
   ├─> Extract cashier from JWT
   ├─> Create ShiftReport with:
   │   ├─> startTime = now
   │   ├─> shiftDate = today
   │   ├─> openingBalance = request.openingBalance
   │   └─> status = OPEN
   └─> Save and return

DURING SHIFT:
   ├─> Orders created reference cashier
   ├─> Refunds created reference shift
   └─> All transactions tracked

END SHIFT:
1. PATCH /api/shift-reports/end
   ├─> Set endTime = now
   ├─> Query all orders for this cashier today
   ├─> Calculate:
   │   ├─> totalSales = sum(order.finalAmount)
   │   ├─> orderCount = count(orders)
   │   ├─> cashSales = sum where paymentType = CASH
   │   ├─> cardSales = sum where paymentType = CARD
   │   ├─> upiSales = sum where paymentType = UPI
   │   └─> totalRefunds = sum(refunds.amount)
   ├─> Set closingBalance = openingBalance + totalSales - totalRefunds
   └─> Update ShiftReport
```

### 13.3 Authentication Workflow

```
SIGNUP:
1. POST /auth/signup {email, password, fullName, role}
   ├─> Validate email not exists
   ├─> Hash password with BCrypt
   ├─> Create User entity
   ├─> Save to database
   └─> Return success message

LOGIN:
1. POST /auth/login {email, password}
   ├─> Find User by email
   ├─> Verify password with BCrypt
   ├─> Generate JWT token with:
   │   ├─> email claim
   │   ├─> authorities claim (role)
   │   └─> 24h expiration
   └─> Return AuthResponse {jwt, message, role}

PASSWORD RESET:
1. POST /auth/forgot-password {email}
   ├─> Find User by email
   ├─> Generate random token
   ├─> Create PasswordResetToken (expiry = 1 hour)
   ├─> Send email with reset link
   └─> Return success message

2. POST /auth/reset-password {token, newPassword}
   ├─> Find PasswordResetToken
   ├─> Validate not expired
   ├─> Hash new password
   ├─> Update User password
   ├─> Delete token
   └─> Return success message
```

---

## 14. PERFORMANCE CONSIDERATIONS

### 14.1 Query Optimization

**Use DTO Projections:**
```java
@Query("SELECT new com.zosh.payload.ProductPerformanceDTO(" +
       "p.name, SUM(oi.quantity), SUM(oi.subtotal)) " +
       "FROM OrderItem oi JOIN oi.product p " +
       "WHERE oi.order.branch.id = :branchId " +
       "GROUP BY p.id ORDER BY SUM(oi.quantity) DESC")
List<ProductPerformanceDTO> getTopProducts(@Param("branchId") Long branchId);
// Returns only needed fields, not full entities
```

**Pagination:**
```java
Page<Order> findByBranchId(Long branchId, Pageable pageable);
// Use in controller: orderService.findAll(PageRequest.of(page, size))
```

**Fetch Strategies:**
```java
@ManyToOne(fetch = FetchType.LAZY)  // Don't load until accessed
private Store store;

@OneToMany(fetch = FetchType.EAGER) // Load immediately
private List<OrderItem> orderItems;
```

### 14.2 Caching Opportunities

**Spring Cache:**
```java
@Cacheable(value = "products", key = "#storeId")
public List<ProductDTO> getProductsByStore(Long storeId) {
    // Cache store products (rarely change)
}

@CacheEvict(value = "products", key = "#product.store.id")
public ProductDTO updateProduct(ProductDTO product) {
    // Invalidate cache on update
}
```

**Recommended Cache Configurations:**
- Product catalog by store (TTL: 1 hour)
- Category list by store (TTL: 2 hours)
- Subscription plans (TTL: 24 hours)
- Store settings (TTL: 30 minutes)

### 14.3 Database Connection Pooling

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

---

## 15. SCALABILITY CONSIDERATIONS

### 15.1 Multi-Tenancy Design

**Current Implementation:**
- Data isolation via `store_id` and `branch_id` columns
- Every query filters by tenant (store/branch)
- Row-level security through service layer

**Example:**
```java
@PreAuthorize("hasRole('STORE_ADMIN')")
public List<ProductDTO> getProducts() {
    User currentUser = userService.getCurrentUser();
    return productRepository.findByStoreId(currentUser.getStore().getId());
}
```

### 15.2 Horizontal Scaling

**Stateless Design:**
- No server-side sessions (JWT in client)
- Multiple backend instances can run in parallel
- Load balancer distributes requests

**Database Scaling:**
- Read replicas for analytics queries
- Master for writes, replicas for reads
- Partition by store_id for sharding

### 15.3 Microservices Migration Path

**Current Monolith can be split into:**
```
1. Auth Service         → User management, JWT
2. Store Service        → Store/Branch CRUD
3. Product Service      → Product catalog, inventory
4. Order Service        → Order processing, POS
5. Payment Service      → Payment gateway, subscriptions
6. Analytics Service    → Reporting, dashboards
7. Notification Service → Email, alerts
```

---

## 16. TESTING STRATEGY

### 16.1 Unit Testing

```java
@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void testCreateOrder_Success() {
        // Given
        OrderDTO orderDTO = OrderDTO.builder()
            .orderItems(List.of(...))
            .build();

        when(productRepository.findById(1L))
            .thenReturn(Optional.of(product));
        when(orderRepository.save(any()))
            .thenReturn(savedOrder);

        // When
        OrderDTO result = orderService.createOrder(orderDTO);

        // Then
        assertNotNull(result);
        assertEquals(100.0, result.getFinalAmount());
        verify(orderRepository).save(any());
    }
}
```

### 16.2 Integration Testing

```java
@SpringBootTest
@AutoConfigureMockMvc
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "BRANCH_CASHIER")
    void testCreateOrder() throws Exception {
        String orderJson = objectMapper.writeValueAsString(orderDTO);

        mockMvc.perform(post("/api/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content(orderJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").exists());
    }
}
```

### 16.3 Repository Testing

```java
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Test
    void testFindByBranchId() {
        // Given
        Order order = new Order();
        order.setBranch(branch);
        orderRepository.save(order);

        // When
        List<Order> orders = orderRepository.findByBranchId(branch.getId());

        // Then
        assertEquals(1, orders.size());
    }
}
```

---

## 17. DEPLOYMENT ARCHITECTURE

### 17.1 Recommended Deployment

```
┌──────────────────────────────────────────────────────┐
│                   Load Balancer                       │
│               (Nginx / AWS ALB)                       │
└────────────────┬─────────────────────────────────────┘
                 │
     ┌───────────┼───────────┐
     │           │           │
┌────▼───┐  ┌───▼────┐  ┌──▼─────┐
│Backend │  │Backend │  │Backend │
│Instance│  │Instance│  │Instance│
│  :5000 │  │  :5000 │  │  :5000 │
└────┬───┘  └───┬────┘  └──┬─────┘
     │          │           │
     └──────────┼───────────┘
                │
      ┌─────────▼──────────┐
      │   MySQL Database   │
      │  (Master-Replica)  │
      └────────────────────┘
```

### 17.2 Docker Configuration

**Dockerfile:**
```dockerfile
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 5000
ENTRYPOINT ["java", "-jar", "app.jar"]
```

**docker-compose.yml:**
```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${DB_PASSWORD}
      MYSQL_DATABASE: pos
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "3306:3306"

  backend:
    build: .
    environment:
      DB_USERNAME: ${DB_USERNAME}
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "5000:5000"
    depends_on:
      - mysql

volumes:
  mysql_data:
```

### 17.3 Environment Variables

```bash
# Database
DB_USERNAME=pos_user
DB_PASSWORD=secure_password

# JWT
JWT_SECRET=your-256-bit-secret-key

# Email
MAIL_USERNAME=your-email@gmail.com
MAIL_APP_PASSWORD=app-specific-password

# Payment Gateways
RAZORPAY_KEY_ID=rzp_test_xxxx
RAZORPAY_KEY_SECRET=secret_xxxx
STRIPE_API_KEY=sk_test_xxxx
```

---

## 18. MONITORING & LOGGING

### 18.1 Logging Strategy

```java
@Slf4j  // Lombok annotation
@Service
public class OrderServiceImpl {

    public OrderDTO createOrder(OrderDTO dto) {
        log.info("Creating order for cashier: {}", getCurrentUser().getEmail());

        try {
            Order order = processOrder(dto);
            log.info("Order created successfully: {}", order.getOrderNumber());
            return OrderMapper.toDto(order);
        } catch (Exception e) {
            log.error("Error creating order", e);
            throw new OrderException("Failed to create order");
        }
    }
}
```

### 18.2 Application Metrics

**Spring Boot Actuator:**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  metrics:
    export:
      prometheus:
        enabled: true
```

**Key Metrics to Monitor:**
- Request rate and latency
- Database connection pool usage
- Order creation rate
- Failed login attempts
- Payment success/failure rate
- Low inventory alerts

---

## 19. SECURITY BEST PRACTICES

### 19.1 Implemented

- JWT-based stateless authentication
- BCrypt password hashing (10 rounds)
- HTTPS enforcement (via proxy)
- CORS configuration
- Role-based access control
- Input validation via Bean Validation
- SQL injection prevention (PreparedStatements via JPA)
- XSS prevention (JSON serialization)

### 19.2 Recommendations

**1. Environment Variables:**
```java
// Move hardcoded secrets to env vars
@Value("${jwt.secret}")
private String jwtSecret;
```

**2. Rate Limiting:**
```java
@RateLimiter(name = "authRateLimiter", fallbackMethod = "rateLimitFallback")
public ResponseEntity<?> login(LoginDto loginDto) { }
```

**3. Password Reset Token Expiry:**
```java
// Already implemented - validate expiry
if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
    throw new TokenExpiredException("Reset token expired");
}
```

**4. Audit Logging:**
```java
@Entity
public class AuditLog {
    private String action;
    private String userId;
    private LocalDateTime timestamp;
    private String ipAddress;
}
```

---

## 20. FUTURE ENHANCEMENTS

### 20.1 Technical Improvements

1. **Caching Layer:** Redis for sessions, product catalog
2. **Message Queue:** RabbitMQ/Kafka for async operations
3. **CDN Integration:** Image hosting for product photos
4. **GraphQL API:** Alternative to REST for flexible queries
5. **WebSocket:** Real-time order notifications
6. **Elasticsearch:** Advanced product search
7. **Monitoring:** Prometheus + Grafana dashboards
8. **CI/CD:** GitHub Actions / Jenkins pipeline

### 20.2 Feature Enhancements

1. **Loyalty Program:** Points, rewards, tiers
2. **Promotions:** Coupons, discounts, flash sales
3. **Multi-Currency:** International support
4. **Barcode Scanning:** Product lookup via barcode
5. **Receipt Printing:** PDF generation, email receipts
6. **Mobile App:** React Native POS app
7. **Offline Mode:** Local storage, sync when online
8. **Advanced Analytics:** ML-based sales forecasting

---

## APPENDIX

### A. Technology Stack Summary

| Layer | Technology |
|-------|------------|
| Backend Framework | Spring Boot 3.5.3 |
| Language | Java 17 |
| Database | MySQL 8.x |
| ORM | Hibernate JPA |
| Security | Spring Security + JWT |
| Build Tool | Maven |
| Payment | Razorpay, Stripe |
| Email | Spring Mail (Gmail SMTP) |
| Utilities | Lombok |

### B. Key Dependencies

```xml
<dependencies>
    <!-- Spring Boot Starters -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-mail</artifactId>
    </dependency>

    <!-- Database -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>

    <!-- Security -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.6</version>
    </dependency>

    <!-- Payment Gateways -->
    <dependency>
        <groupId>com.razorpay</groupId>
        <artifactId>razorpay-java</artifactId>
        <version>1.4.8</version>
    </dependency>
    <dependency>
        <groupId>com.stripe</groupId>
        <artifactId>stripe-java</artifactId>
        <version>28.3.1</version>
    </dependency>

    <!-- Utilities -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>
```

### C. Package Structure Reference

```
com.zosh
├── PosSystemApplication.java
├── configrations
│   ├── AppConfig.java
│   ├── EmailUtil.java
│   ├── JwtConstant.java
│   ├── JwtProvider.java
│   ├── JwtValidator.java
│   └── SecurityConfig.java
├── controller (19 controllers)
├── domain (10 enums)
├── exception (3 handlers)
├── mapper (12 mappers)
├── modal (18 entities)
├── payload (DTOs organized by feature)
├── repository (15 repositories)
├── service (13 interfaces)
├── service/impl (11 implementations)
└── util
    └── SecurityUtil.java
```

---

**Document Version:** 1.0
**Last Updated:** October 2025
**Author:** Generated from POS System Codebase Analysis
**Project:** Multi-Tenant Point of Sale System
