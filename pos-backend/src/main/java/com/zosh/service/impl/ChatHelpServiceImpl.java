package com.zosh.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zosh.modal.User;
import com.zosh.payload.dto.ChatMessageDTO;
import com.zosh.payload.request.ChatHelpRequest;
import com.zosh.payload.response.ChatHelpResponse;
import com.zosh.payload.response.ChatStatusResponse;
import com.zosh.service.ChatHelpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHelpServiceImpl implements ChatHelpService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-1.5-flash}")
    private String model;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models}")
    private String geminiApiUrl;

    private static final String SYSTEM_KNOWLEDGE_BASE = """
            You are "POS Copilot", the intelligent, friendly, and expert in-app AI assistant for this Point of Sale (POS) and Multi-Branch Retail Management System.
            Your role is to guide and assist users with navigating, understanding, and using this POS application efficiently and accurately.

            ================================================================
            VERIFIED APPLICATION ARCHITECTURE & ROLES
            ================================================================
            1. Super Admin (ROLE_ADMIN):
               - Access to Super Admin Portal (/super-admin)
               - Platform Overview Dashboard (/super-admin/dashboard) with global metrics (Total Stores, Active Subscriptions, Revenue).
               - Store Management (/super-admin/stores): View all registered stores and their details (/super-admin/stores/:id).
               - Store Approval Requests (/super-admin/requests): Review and approve/reject newly submitted store onboarding requests.
               - Subscription Plans Management (/super-admin/subscriptions): Configure pricing tiers (FREE, BASIC, PRO, ENTERPRISE).
               - Global Settings (/super-admin/settings).

            2. Store Admin & Store Manager (ROLE_STORE_ADMIN, ROLE_STORE_MANAGER):
               - Store Dashboard (/store/dashboard): Real-time revenue, orders count, today's sales, top-selling products.
               - Branches Management (/store/branches): Create new branches, assign branch managers, view and update branch contact details.
               - Categories Management (/store/categories): Create, edit, and organize product categories.
               - Product Catalog (/store/products): Create, edit, and delete products with Title, SKU, Barcode, Price, Cost, Category, Stock Level, and Images.
               - Store Employees (/store/employees): Add and manage staff members, assign roles (Store Manager, Branch Manager, Cashier) and assign them to branches.
               - Store Profile (/store/stores): Manage store name, contact info, and business branding.
               - Sales & Invoices (/store/sales): Store-wide transactions and sales history.
               - Reports & Analytics (/store/reports): Detailed sales breakdowns, revenue analytics, and performance reports.
               - Subscription & Upgrade (/store/upgrade): Upgrade or renew subscription tier using Razorpay or Stripe.
               - Store Settings (/store/settings): Currency, tax rates, store operational defaults.
               - Alerts & Inventory Warnings (/store/alerts): Low stock notifications and operational alerts.

            3. Branch Manager & Branch Admin (ROLE_BRANCH_MANAGER, ROLE_BRANCH_ADMIN):
               - Branch Dashboard (/branch/dashboard): Branch-specific sales, cashier counts, today's order metrics.
               - Branch Orders (/branch/orders): Branch order tracking and receipt viewing.
               - Returns & Refunds (/branch/refunds): Review and approve/process branch refund requests.
               - Transactions (/branch/transactions): Breakdown of Cash, Card, and UPI payments.
               - Branch Inventory (/branch/inventory): View current stock levels, adjust inventory quantities, track low stock.
               - Branch Employees (/branch/employees): Manage staff assigned to this specific branch.
               - Branch Customers (/branch/customers): Lookup customer directory and order histories.
               - Branch Reports (/branch/reports): Daily branch sales reports and cashier shift breakdowns.
               - Branch Settings (/branch/settings): Branch operational configurations.

            4. Branch Cashier (ROLE_BRANCH_CASHIER):
               - POS Terminal (/cashier): Fast retail checkout. Search items by name, barcode, or SKU, or click product tiles. Adjust quantities, apply discounts.
               - Customer Assignment: Lookup customer by phone/name or quickly add a new customer during checkout.
               - Multi-Payment Support: Process payments via CASH, CARD, UPI, STRIPE, or RAZORPAY.
               - Order Completion & Receipt: Generate, view, and print itemized customer receipts.
               - Order History (/cashier/orders): Search past orders by ID or customer.
               - Returns/Refunds (/cashier/returns): Look up previous orders and process item returns/refunds.
               - Customer Directory (/cashier/customers): Search and add customer records.
               - Shift Summary (/cashier/shift-summary): View shift opening cash, payment breakdown (Cash, Card, UPI), total refunds, register balance, and close shift.

            5. Guests / Unauthenticated Users:
               - Landing Page (/): Product features, pricing table, demo information.
               - Authentication (/auth/login, /auth/register, /auth/forgot-password, /auth/reset-password).
               - Onboarding Wizard (/auth/onboarding): Required setup wizard for newly registered store admins to create their store profile before dashboard access.

            ================================================================
            STEP-BY-STEP WORKFLOWS & COMMON TASKS
            ================================================================
            - How to Create a Sale (Cashier):
              1. Go to POS Terminal (/cashier).
              2. Scan the barcode or search and click products to add them to the cart.
              3. Adjust item quantities or remove items as needed.
              4. (Optional) Search for an existing customer by phone number or click "Add Customer".
              5. Click "Checkout" / "Proceed to Payment".
              6. Select payment method: Cash (enter tender amount), Card, UPI (scan QR/enter reference), or Razorpay/Stripe.
              7. Confirm payment and print/download the receipt.

            - How to Process a Refund (Cashier / Branch Manager):
              1. Go to Returns/Refunds (/cashier/returns or /branch/refunds).
              2. Enter the original Order ID or search for the order.
              3. Select the items and quantity being returned.
              4. Select the refund reason and payment refund method.
              5. Submit the return to update inventory and balance.

            - How to Add a New Product (Store Admin):
              1. Navigate to Store -> Products (/store/products).
              2. Click "Add Product".
              3. Fill in Title, SKU, Barcode, Selling Price, Cost Price, Category, and initial Stock.
              4. Upload product image and save.

            - How to Add a Branch (Store Admin):
              1. Navigate to Store -> Branches (/store/branches).
              2. Click "Add Branch".
              3. Enter Branch Name, Address, Contact Phone, and assign a Branch Manager.
              4. Save the branch.

            - How to Close Cashier Shift (Cashier):
              1. Navigate to Shift Summary (/cashier/shift-summary).
              2. Review total cash, card, and UPI sales against the physical cash in drawer.
              3. Complete the shift closing report.

            - How to Upgrade Store Subscription:
              1. Navigate to Store -> Upgrade (/store/upgrade).
              2. Choose between Basic, Pro, or Enterprise plans.
              3. Select Razorpay or Stripe and complete the secure payment.

            ================================================================
            STRICT PRIVACY, SAFETY, AND OPERATIONAL RULES
            ================================================================
            1. You are a guidance-only assistant. You provide clear, step-by-step instructions on how the user can perform tasks in the software.
            2. You CANNOT directly execute database changes, initiate financial transactions, or modify records. If a user asks you to "delete branch" or "change price", guide them step-by-step on how they can do it on the proper screen.
            3. Never ask for, repeat, or store sensitive customer Personally Identifiable Information (PII) like credit card CVVs, raw passwords, or private customer records.
            4. Never invent or hallucinate non-existent features, buttons, or URLs. Only refer to the real pages, buttons, and workflows listed above.
            5. If a question is outside the scope of this POS system, politely explain that you are dedicated to helping with this POS system and guide the user to the administrator or appropriate channel.
            6. Format your responses with clean Markdown formatting, using bold highlights, bullet points, and numbered lists for readability.
            """;

    @Override
    public ChatStatusResponse getChatStatus() {
        boolean configured = isApiKeyConfigured();
        String activeModel = StringUtils.hasText(model) ? model.trim() : "gemini-1.5-flash";
        String message = configured
                ? "Gemini AI is configured and ready using model: " + activeModel
                : "Gemini API key is not configured. Please set the GEMINI_API_KEY environment variable on the server.";

        return ChatStatusResponse.builder()
                .configured(configured)
                .model(activeModel)
                .message(message)
                .build();
    }

    @Override
    public ChatHelpResponse getAiHelp(ChatHelpRequest request, User authenticatedUser) {
        if (!isApiKeyConfigured()) {
            return ChatHelpResponse.builder()
                    .success(false)
                    .configured(false)
                    .errorCode("API_KEY_MISSING")
                    .reply("The Gemini API key is not configured on the backend server. " +
                            "Please set the GEMINI_API_KEY environment variable and restart the backend service. " +
                            "You can obtain a free API key from Google AI Studio (https://aistudio.google.com).")
                    .suggestedActions(List.of(
                            "How do I set up GEMINI_API_KEY on Windows?",
                            "How to create a sale on POS terminal?",
                            "How to add a product?"
                    ))
                    .build();
        }

        String activeModel = StringUtils.hasText(model) ? model.trim() : "gemini-1.5-flash";
        String effectiveRole = resolveUserRole(request, authenticatedUser);
        String currentRoute = StringUtils.hasText(request.getCurrentRoute()) ? request.getCurrentRoute() : "Unknown";

        try {
            // Build Gemini request JSON
            ObjectNode rootNode = objectMapper.createObjectNode();

            // 1. System Instruction
            String dynamicSystemPrompt = SYSTEM_KNOWLEDGE_BASE +
                    "\n\nCURRENT USER CONTEXT:\n" +
                    "- Active User Role: " + effectiveRole + "\n" +
                    "- Current Page/Route: " + currentRoute + "\n" +
                    "Tailor your response specifically for this role and page if relevant, keeping explanations clear and concise.\n";

            ObjectNode systemInstructionNode = rootNode.putObject("systemInstruction");
            ArrayNode systemParts = systemInstructionNode.putArray("parts");
            systemParts.addObject().put("text", dynamicSystemPrompt);

            // 2. Contents (History + Current Prompt)
            ArrayNode contentsArray = rootNode.putArray("contents");

            // Sanitize and append history (up to last 10 turns)
            List<ChatMessageDTO> history = request.getHistory();
            if (history != null && !history.isEmpty()) {
                int startIndex = Math.max(0, history.size() - 10);
                for (int i = startIndex; i < history.size(); i++) {
                    ChatMessageDTO item = history.get(i);
                    if (item != null && StringUtils.hasText(item.getContent())) {
                        String role = ("model".equalsIgnoreCase(item.getRole()) || "assistant".equalsIgnoreCase(item.getRole()))
                                ? "model"
                                : "user";
                        ObjectNode historyContent = contentsArray.addObject();
                        historyContent.put("role", role);
                        ArrayNode parts = historyContent.putArray("parts");
                        parts.addObject().put("text", item.getContent().trim());
                    }
                }
            }

            // Append the latest user message
            ObjectNode userContent = contentsArray.addObject();
            userContent.put("role", "user");
            ArrayNode userParts = userContent.putArray("parts");
            userParts.addObject().put("text", request.getMessage().trim());

            // 3. Generation Config
            ObjectNode genConfig = rootNode.putObject("generationConfig");
            genConfig.put("temperature", 0.3);
            genConfig.put("topP", 0.95);
            genConfig.put("maxOutputTokens", 1024);

            // Prepare HTTP Request
            String cleanApiKey = apiKey.trim();
            String targetUrl = geminiApiUrl.replaceAll("/+$", "") + "/" + activeModel + ":generateContent";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-goog-api-key", cleanApiKey);

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(rootNode), headers);

            log.info("Sending AI help request to Gemini API (model: {}) for role: {}, route: {}", activeModel, effectiveRole, currentRoute);
            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                JsonNode candidates = responseJson.path("candidates");

                if (candidates.isArray() && !candidates.isEmpty()) {
                    JsonNode firstCandidate = candidates.get(0);
                    JsonNode textNode = firstCandidate.path("content").path("parts").path(0).path("text");
                    String replyText = textNode.asText("");

                    if (StringUtils.hasText(replyText)) {
                        List<String> suggestedActions = generateFollowUpSuggestions(effectiveRole, currentRoute);
                        return ChatHelpResponse.builder()
                                .reply(replyText)
                                .model(activeModel)
                                .success(true)
                                .configured(true)
                                .suggestedActions(suggestedActions)
                                .build();
                    }
                }

                // If candidate was blocked or empty
                return ChatHelpResponse.builder()
                        .reply("I could not generate a response for this inquiry. Please try rephrasing your question.")
                        .model(activeModel)
                        .success(true)
                        .configured(true)
                        .build();
            } else {
                return ChatHelpResponse.builder()
                        .reply("Received an unexpected response from Gemini AI. Please try again.")
                        .model(activeModel)
                        .success(false)
                        .errorCode("UNEXPECTED_RESPONSE")
                        .configured(true)
                        .build();
            }

        } catch (HttpClientErrorException ex) {
            log.error("Gemini API client error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return handleHttpClientError(ex, activeModel);
        } catch (HttpServerErrorException ex) {
            log.error("Gemini API server error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return ChatHelpResponse.builder()
                    .reply("Google Gemini service is temporarily experiencing high load or issues. Please wait a moment and try again.")
                    .model(activeModel)
                    .success(false)
                    .configured(true)
                    .errorCode("GEMINI_SERVER_ERROR")
                    .error(ex.getMessage())
                    .build();
        } catch (ResourceAccessException ex) {
            log.error("Network or timeout error contacting Gemini API: {}", ex.getMessage());
            return ChatHelpResponse.builder()
                    .reply("Network timeout while connecting to Gemini AI. Please check your internet connection and try again.")
                    .model(activeModel)
                    .success(false)
                    .configured(true)
                    .errorCode("TIMEOUT_ERROR")
                    .error(ex.getMessage())
                    .build();
        } catch (Exception ex) {
            log.error("Unexpected error in ChatHelpService: ", ex);
            return ChatHelpResponse.builder()
                    .reply("An unexpected error occurred while processing your request: " + ex.getMessage())
                    .model(activeModel)
                    .success(false)
                    .configured(true)
                    .errorCode("INTERNAL_ERROR")
                    .error(ex.getMessage())
                    .build();
        }
    }

    private boolean isApiKeyConfigured() {
        if (!StringUtils.hasText(apiKey)) {
            return false;
        }
        String clean = apiKey.trim();
        return !clean.equalsIgnoreCase("provide your gemin api key optional")
                && !clean.startsWith("provide your")
                && !clean.isEmpty();
    }

    private String resolveUserRole(ChatHelpRequest request, User user) {
        if (user != null && user.getRole() != null) {
            return user.getRole().name();
        }
        if (StringUtils.hasText(request.getUserRole())) {
            return request.getUserRole().trim();
        }
        return "GUEST";
    }

    private ChatHelpResponse handleHttpClientError(HttpClientErrorException ex, String activeModel) {
        int statusCode = ex.getStatusCode().value();
        String responseBody = ex.getResponseBodyAsString();

        if (statusCode == 429) {
            return ChatHelpResponse.builder()
                    .reply("Gemini API rate limit or free-tier quota has been exceeded for model `" + activeModel + "`. " +
                            "Please wait a few seconds before retrying.")
                    .model(activeModel)
                    .success(false)
                    .configured(true)
                    .errorCode("QUOTA_EXCEEDED")
                    .error("Rate limit / Quota exceeded (429)")
                    .build();
        } else if (statusCode == 400 || statusCode == 403) {
            return ChatHelpResponse.builder()
                    .reply("Invalid or unauthorized Gemini API key. Please check your `GEMINI_API_KEY` configuration in Google AI Studio.")
                    .model(activeModel)
                    .success(false)
                    .configured(true)
                    .errorCode("API_KEY_INVALID")
                    .error("Unauthorized / Invalid API Key (" + statusCode + ")")
                    .build();
        } else if (statusCode == 404) {
            return ChatHelpResponse.builder()
                    .reply("The configured Gemini model `" + activeModel + "` was not found. Please verify the `gemini.api.model` setting in application.yml or GEMINI_MODEL environment variable.")
                    .model(activeModel)
                    .success(false)
                    .configured(true)
                    .errorCode("MODEL_NOT_FOUND")
                    .error("Model Not Found (" + statusCode + ")")
                    .build();
        }

        return ChatHelpResponse.builder()
                .reply("Gemini API returned an error (" + statusCode + "). Please check your parameters and try again.")
                .model(activeModel)
                .success(false)
                .configured(true)
                .errorCode("API_CLIENT_ERROR")
                .error(responseBody)
                .build();
    }

    private List<String> generateFollowUpSuggestions(String role, String currentRoute) {
        List<String> list = new ArrayList<>();
        if (currentRoute.contains("cashier") || "ROLE_BRANCH_CASHIER".equals(role)) {
            list.add("How do I complete a transaction with split or UPI payment?");
            list.add("How do I look up past customer orders?");
            list.add("How do I close my cashier shift?");
        } else if (currentRoute.contains("branch") || "ROLE_BRANCH_MANAGER".equals(role) || "ROLE_BRANCH_ADMIN".equals(role)) {
            list.add("How do I adjust branch inventory stock?");
            list.add("How do I process a customer refund?");
            list.add("Where can I view cashier shift summaries?");
        } else if (currentRoute.contains("super-admin") || "ROLE_ADMIN".equals(role)) {
            list.add("How do I approve pending store requests?");
            list.add("How do I configure subscription plans?");
            list.add("How to view all active stores?");
        } else {
            list.add("How do I add a new product with barcode?");
            list.add("How do I create and assign a new branch?");
            list.add("How do I upgrade my store subscription?");
        }
        return list;
    }
}
