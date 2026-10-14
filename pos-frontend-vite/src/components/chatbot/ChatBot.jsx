import React, { useState, useEffect, useRef } from "react";
import { useLocation } from "react-router";
import { useSelector } from "react-redux";
import api from "@/utils/api";
import {
  MessageSquare,
  X,
  Send,
  Sparkles,
  RotateCcw,
  Bot,
  User,
  AlertCircle,
  Minimize2,
  ChevronDown,
  HelpCircle,
  ExternalLink,
} from "lucide-react";
import { Button } from "@/components/ui/button";

// Helper to render formatted markdown-like text safely
const FormattedMessage = ({ content }) => {
  if (!content) return null;

  // Split lines
  const lines = content.split("\n");

  return (
    <div className="space-y-1.5 text-sm leading-relaxed">
      {lines.map((line, idx) => {
        const trimmed = line.trim();
        if (!trimmed) {
          return <div key={idx} className="h-1" />;
        }

        // Headers
        if (trimmed.startsWith("### ")) {
          return (
            <h4 key={idx} className="font-semibold text-foreground text-sm mt-2 mb-1">
              {trimmed.replace("### ", "")}
            </h4>
          );
        }
        if (trimmed.startsWith("## ")) {
          return (
            <h3 key={idx} className="font-bold text-foreground text-base mt-2 mb-1">
              {trimmed.replace("## ", "")}
            </h3>
          );
        }

        // Bullet points
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
          const bulletContent = trimmed.substring(2);
          return (
            <div key={idx} className="flex items-start space-x-2 pl-1">
              <span className="text-primary font-bold mt-0.5">•</span>
              <span className="flex-1">{parseInlineFormatting(bulletContent)}</span>
            </div>
          );
        }

        // Numbered list
        const numMatch = trimmed.match(/^(\d+)\.\s+(.*)/);
        if (numMatch) {
          return (
            <div key={idx} className="flex items-start space-x-2 pl-1">
              <span className="font-semibold text-primary text-xs mt-0.5 min-w-4">
                {numMatch[1]}.
              </span>
              <span className="flex-1">{parseInlineFormatting(numMatch[2])}</span>
            </div>
          );
        }

        return <p key={idx}>{parseInlineFormatting(line)}</p>;
      })}
    </div>
  );
};

// Inline helper for **bold** and `code`
const parseInlineFormatting = (text) => {
  if (!text) return "";
  // Split by bold (**...**) and inline code (`...`)
  const parts = [];
  let remaining = text;
  let key = 0;

  const regex = /(\*\*.*?\*\*|`.*?`)/g;
  let lastIndex = 0;
  let match;

  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push(text.substring(lastIndex, match.index));
    }
    const token = match[0];
    if (token.startsWith("**") && token.endsWith("**")) {
      parts.push(
        <strong key={key++} className="font-semibold text-foreground">
          {token.slice(2, -2)}
        </strong>
      );
    } else if (token.startsWith("`") && token.endsWith("`")) {
      parts.push(
        <code
          key={key++}
          className="bg-muted px-1.5 py-0.5 rounded text-xs font-mono text-primary border border-border/50"
        >
          {token.slice(1, -1)}
        </code>
      );
    }
    lastIndex = regex.lastIndex;
  }

  if (lastIndex < text.length) {
    parts.push(text.substring(lastIndex));
  }

  return parts.length > 0 ? parts : text;
};

// Dynamic suggested questions based on role & route
const getSuggestedPrompts = (role, pathname) => {
  if (!role || pathname === "/") {
    return [
      "What features does POS Pro offer?",
      "How does multi-store & multi-branch work?",
      "How do pricing and subscription plans work?",
      "How do I sign up and onboard my store?",
    ];
  }

  if (pathname.includes("/products")) {
    return [
      "How do I add a new product?",
      "What fields are required for creating a product?",
      "How do product categories work?",
      "Can I update inventory stock directly?",
    ];
  }

  if (pathname.includes("/branches")) {
    return [
      "How do I create a new branch?",
      "How do I assign a manager to a branch?",
      "Can a cashier work across multiple branches?",
    ];
  }

  if (pathname.includes("/cashier") || role === "ROLE_BRANCH_CASHIER") {
    return [
      "How do I start and complete a new order?",
      "How do I search for a customer during checkout?",
      "How do I process a customer refund/return?",
      "How does shift summary report work?",
    ];
  }

  if (pathname.includes("/super-admin") || role === "ROLE_ADMIN") {
    return [
      "How do I approve or reject pending store requests?",
      "How do subscription plans work across stores?",
      "Where can I view overall platform metrics?",
    ];
  }

  if (role === "ROLE_STORE_ADMIN" || role === "ROLE_STORE_MANAGER") {
    return [
      "How do I add new employees and assign roles?",
      "Where do I view sales reports and analytics?",
      "How do I upgrade my store subscription?",
      "How do I configure store tax & currency settings?",
    ];
  }

  return [
    "How can I navigate this POS system?",
    "What actions can I perform with my role?",
    "How do I manage orders and customers?",
  ];
};

const ChatBot = () => {
  const location = useLocation();
  const { userProfile } = useSelector((state) => state.user || {});
  const userRole = userProfile?.role || null;

  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState(() => {
    const saved = sessionStorage.getItem("pos_chat_messages");
    if (saved) {
      try {
        return JSON.parse(saved);
      } catch (e) {
        // fallback
      }
    }
    return [
      {
        role: "model",
        content:
          "👋 Hello! I'm **POS Copilot**, your real-time AI assistant powered by Google Gemini. How can I help you with your store, products, orders, or POS operations today?",
      },
    ];
  });

  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [backendStatus, setBackendStatus] = useState(null);
  const [errorMessage, setErrorMessage] = useState(null);
  const [suggestedActions, setSuggestedActions] = useState([]);

  const messagesEndRef = useRef(null);
  const inputRef = useRef(null);

  // Save session messages
  useEffect(() => {
    try {
      sessionStorage.setItem("pos_chat_messages", JSON.stringify(messages));
    } catch (e) {
      // ignore
    }
  }, [messages]);

  // Scroll to bottom on new message
  useEffect(() => {
    if (isOpen) {
      messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
    }
  }, [messages, isOpen, loading]);

  // Focus input on open
  useEffect(() => {
    if (isOpen) {
      setTimeout(() => inputRef.current?.focus(), 150);
      checkBackendStatus();
    }
  }, [isOpen]);

  const checkBackendStatus = async () => {
    try {
      const res = await api.get("/api/chat/status");
      setBackendStatus(res.data);
    } catch (err) {
      setBackendStatus({
        configured: false,
        model: "offline",
        message: "Backend chat service currently unreachable.",
      });
    }
  };

  const handleSendMessage = async (textToSend) => {
    const query = (textToSend || input).trim();
    if (!query || loading) return;

    const userMsg = { role: "user", content: query };
    const updatedHistory = [...messages, userMsg];
    setMessages(updatedHistory);
    setInput("");
    setLoading(true);
    setErrorMessage(null);
    setSuggestedActions([]);

    try {
      const jwt = localStorage.getItem("jwt");
      const headers = jwt ? { Authorization: `Bearer ${jwt}` } : {};

      // Prepare history formatted for API (excluding initial welcome greeting if desired or keeping all)
      const apiHistory = updatedHistory
        .filter((m) => m.role === "user" || m.role === "model")
        .slice(-10) // Keep last 10 messages for context
        .map((m) => ({
          role: m.role,
          content: m.content,
        }));

      const payload = {
        message: query,
        currentRoute: location.pathname,
        userRole: userRole || "GUEST",
        history: apiHistory,
      };

      const res = await api.post("/api/chat/help", payload, { headers });
      const data = res.data;

      if (data.success && data.reply) {
        setMessages((prev) => [
          ...prev,
          {
            role: "model",
            content: data.reply,
            model: data.model,
          },
        ]);
        if (data.suggestedActions && data.suggestedActions.length > 0) {
          setSuggestedActions(data.suggestedActions);
        }
      } else {
        const errorText =
          data.reply ||
          data.error ||
          "Sorry, I encountered an issue retrieving an AI response. Please try again.";
        setErrorMessage(errorText);
        setMessages((prev) => [
          ...prev,
          {
            role: "model",
            content: `⚠️ **AI Service Notice:**\n${errorText}`,
            isError: true,
          },
        ]);
        if (data.suggestedActions) {
          setSuggestedActions(data.suggestedActions);
        }
      }
    } catch (err) {
      console.error("Chat error:", err);
      const serverErr =
        err.response?.data?.error ||
        err.response?.data?.message ||
        "Unable to connect to the AI backend. Please verify your backend server is running on port 5000.";
      setErrorMessage(serverErr);
      setMessages((prev) => [
        ...prev,
        {
          role: "model",
          content: `⚠️ **Connection Error:**\n${serverErr}`,
          isError: true,
        },
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleClearChat = () => {
    const welcome = [
      {
        role: "model",
        content:
          "✨ Chat cleared! I'm ready for your next question. How can I assist you?",
      },
    ];
    setMessages(welcome);
    setErrorMessage(null);
    setSuggestedActions([]);
    sessionStorage.removeItem("pos_chat_messages");
  };

  const handleKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const prompts = getSuggestedPrompts(userRole, location.pathname);

  return (
    <>
      {/* Floating Action Button */}
      <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end gap-2 group">
        {!isOpen && (
          <button
            onClick={() => setIsOpen(true)}
            aria-label="Open AI Help Assistant"
            className="flex items-center gap-2.5 px-4 py-3 rounded-full bg-gradient-to-r from-emerald-600 via-teal-600 to-emerald-700 text-white shadow-2xl hover:shadow-emerald-500/25 hover:scale-105 active:scale-95 transition-all duration-300 font-medium text-sm border border-white/20 backdrop-blur-md cursor-pointer animate-in fade-in zoom-in-90"
          >
            <div className="relative flex items-center justify-center">
              <Sparkles className="w-5 h-5 text-amber-300 animate-pulse" />
              <span className="absolute -top-1 -right-1 flex h-2 w-2">
                <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-300 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-2 w-2 bg-emerald-400"></span>
              </span>
            </div>
            <span className="font-semibold tracking-wide">Ask POS AI</span>
          </button>
        )}
      </div>

      {/* Slide-over / Modal Chat Window */}
      {isOpen && (
        <div
          className="fixed bottom-6 right-6 z-50 w-[92vw] sm:w-[420px] h-[600px] max-h-[85vh] bg-card/95 backdrop-blur-xl border border-border/80 rounded-2xl shadow-2xl flex flex-col overflow-hidden animate-in fade-in slide-in-from-bottom-6 duration-300"
          role="dialog"
          aria-label="POS Copilot Chat Window"
        >
          {/* Header */}
          <div className="bg-gradient-to-r from-emerald-600 via-teal-700 to-slate-800 text-white p-4 flex items-center justify-between border-b border-white/10 select-none shadow-sm">
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-xl bg-white/15 backdrop-blur-md border border-white/20 flex items-center justify-center shadow-inner">
                <Bot className="w-5 h-5 text-emerald-200" />
              </div>
              <div>
                <div className="flex items-center gap-2">
                  <h3 className="font-semibold text-sm tracking-wide">POS Copilot</h3>
                  <span className="inline-flex items-center px-1.5 py-0.2 rounded-full text-[10px] font-medium bg-emerald-400/20 text-emerald-200 border border-emerald-300/30">
                    Gemini AI
                  </span>
                </div>
                <p className="text-[11px] text-emerald-100/80">
                  {userRole
                    ? userRole.replace("ROLE_", "").replace("_", " ")
                    : "Store Guest"}{" "}
                  • Online Assistant
                </p>
              </div>
            </div>

            <div className="flex items-center gap-1">
              <Button
                variant="ghost"
                size="icon"
                onClick={handleClearChat}
                title="Start new chat / Clear conversation"
                className="h-8 w-8 text-white/80 hover:text-white hover:bg-white/15 rounded-lg transition-colors cursor-pointer"
              >
                <RotateCcw className="w-4 h-4" />
              </Button>
              <Button
                variant="ghost"
                size="icon"
                onClick={() => setIsOpen(false)}
                title="Close chat"
                className="h-8 w-8 text-white/80 hover:text-white hover:bg-white/15 rounded-lg transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </Button>
            </div>
          </div>

          {/* Messages Body */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4 bg-muted/20 scroll-smooth">
            {messages.map((msg, index) => {
              const isUser = msg.role === "user";
              return (
                <div
                  key={index}
                  className={`flex items-start gap-2.5 ${
                    isUser ? "flex-row-reverse" : "flex-row"
                  }`}
                >
                  <div
                    className={`w-7 h-7 rounded-full flex items-center justify-center text-xs shrink-0 mt-0.5 ${
                      isUser
                        ? "bg-primary text-primary-foreground"
                        : msg.isError
                        ? "bg-destructive/20 text-destructive border border-destructive/30"
                        : "bg-emerald-600/15 text-emerald-700 dark:text-emerald-400 border border-emerald-500/20"
                    }`}
                  >
                    {isUser ? (
                      <User className="w-4 h-4" />
                    ) : msg.isError ? (
                      <AlertCircle className="w-4 h-4" />
                    ) : (
                      <Sparkles className="w-4 h-4" />
                    )}
                  </div>

                  <div
                    className={`max-w-[82%] rounded-2xl px-3.5 py-2.5 shadow-sm text-sm ${
                      isUser
                        ? "bg-primary text-primary-foreground rounded-tr-none"
                        : msg.isError
                        ? "bg-destructive/10 text-destructive-foreground border border-destructive/20 rounded-tl-none"
                        : "bg-card text-card-foreground border border-border/70 rounded-tl-none"
                    }`}
                  >
                    <FormattedMessage content={msg.content} />

                    {msg.model && (
                      <div className="mt-1.5 pt-1.5 border-t border-border/40 flex items-center justify-between text-[10px] text-muted-foreground">
                        <span>Answered by {msg.model}</span>
                      </div>
                    )}
                  </div>
                </div>
              );
            })}

            {/* Loading / Typing Indicator */}
            {loading && (
              <div className="flex items-start gap-2.5">
                <div className="w-7 h-7 rounded-full bg-emerald-600/15 text-emerald-600 flex items-center justify-center text-xs shrink-0 mt-0.5 border border-emerald-500/20">
                  <Sparkles className="w-4 h-4 animate-spin" />
                </div>
                <div className="bg-card border border-border/70 rounded-2xl rounded-tl-none px-4 py-3 shadow-sm flex items-center gap-1.5">
                  <div className="w-2 h-2 rounded-full bg-emerald-600 animate-bounce" />
                  <div className="w-2 h-2 rounded-full bg-emerald-600 animate-bounce [animation-delay:0.2s]" />
                  <div className="w-2 h-2 rounded-full bg-emerald-600 animate-bounce [animation-delay:0.4s]" />
                  <span className="text-xs text-muted-foreground ml-2">
                    Analyzing POS context...
                  </span>
                </div>
              </div>
            )}

            {/* Suggested Followup Actions */}
            {suggestedActions && suggestedActions.length > 0 && !loading && (
              <div className="pt-2 space-y-1.5">
                <p className="text-[11px] font-medium text-muted-foreground flex items-center gap-1">
                  <HelpCircle className="w-3 h-3" /> Suggested next steps:
                </p>
                <div className="flex flex-wrap gap-1.5">
                  {suggestedActions.map((action, i) => (
                    <button
                      key={i}
                      onClick={() => handleSendMessage(action)}
                      className="text-xs bg-emerald-500/10 hover:bg-emerald-500/20 text-emerald-800 dark:text-emerald-300 border border-emerald-500/20 rounded-lg px-2.5 py-1 text-left transition-colors cursor-pointer"
                    >
                      {action}
                    </button>
                  ))}
                </div>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Quick Prompts Bar (when only 1 or 2 messages exist) */}
          {messages.length <= 2 && !loading && (
            <div className="p-3 bg-muted/40 border-t border-border/60">
              <p className="text-[11px] font-medium text-muted-foreground mb-2 flex items-center gap-1">
                <Sparkles className="w-3 h-3 text-amber-500" /> Common Questions:
              </p>
              <div className="flex flex-col gap-1.5 max-h-28 overflow-y-auto pr-1">
                {prompts.map((q, idx) => (
                  <button
                    key={idx}
                    onClick={() => handleSendMessage(q)}
                    className="text-left text-xs bg-background hover:bg-accent text-foreground hover:text-accent-foreground border border-border/70 rounded-lg px-2.5 py-1.5 transition-colors duration-150 cursor-pointer flex items-center justify-between group"
                  >
                    <span className="truncate">{q}</span>
                    <Send className="w-3 h-3 text-muted-foreground group-hover:text-primary shrink-0 ml-1.5" />
                  </button>
                ))}
              </div>
            </div>
          )}

          {/* Input & Footer Area */}
          <div className="p-3 bg-card border-t border-border/80">
            <div className="flex items-center gap-2">
              <input
                ref={inputRef}
                type="text"
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder="Ask POS Copilot anything..."
                disabled={loading}
                className="flex-1 bg-muted/50 border border-input rounded-xl px-3.5 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500/30 focus:border-emerald-500 transition-all placeholder:text-muted-foreground/60 disabled:opacity-50"
              />
              <Button
                onClick={() => handleSendMessage()}
                disabled={!input.trim() || loading}
                size="icon"
                className="rounded-xl h-10 w-10 bg-emerald-600 hover:bg-emerald-700 text-white shrink-0 shadow-md cursor-pointer disabled:opacity-40"
                title="Send message (Enter)"
              >
                <Send className="w-4 h-4" />
              </Button>
            </div>

            <div className="mt-2 flex items-center justify-between text-[11px] text-muted-foreground/80 px-1">
              <span className="truncate">
                {location.pathname === "/"
                  ? "Home / Landing"
                  : location.pathname}
              </span>
              <span className="text-[10px] text-muted-foreground/60">
                Press Enter ↵ to send
              </span>
            </div>
          </div>
        </div>
      )}
    </>
  );
};

export default ChatBot;
