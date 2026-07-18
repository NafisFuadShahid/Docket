"use client";

import { useRef, useState } from "react";
import { api } from "@/lib/api";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Bot, Send, User } from "lucide-react";

interface Message {
  role: "user" | "assistant";
  content: string;
  citations?: string[];
}

export default function AssistantPage() {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const bottomRef = useRef<HTMLDivElement>(null);

  const send = async () => {
    const text = input.trim();
    if (!text || loading) return;
    setMessages((prev) => [...prev, { role: "user", content: text }]);
    setInput("");
    setLoading(true);

    try {
      const res = await api.post<Record<string, unknown>>(
        "/api/v1/assistant/chat",
        { message: text, conversation_id: conversationId },
      );
      const convId = res?.conversation_id;
      if (typeof convId === "string") setConversationId(convId);

      const content = typeof res?.content === "string" ? res.content : "No response received.";
      const rawCitations = Array.isArray(res?.citations) ? res.citations : [];
      const citations = rawCitations
        .map((c: unknown) => (typeof c === "string" ? c : typeof c === "object" && c !== null && "source" in c ? String((c as { source: unknown }).source) : ""))
        .filter(Boolean);

      setMessages((prev) => [...prev, { role: "assistant", content, citations }]);
    } catch {
      setMessages((prev) => [
        ...prev,
        { role: "assistant", content: "Sorry, I encountered an error. Please try again." },
      ]);
    } finally {
      setLoading(false);
      setTimeout(() => bottomRef.current?.scrollIntoView({ behavior: "smooth" }), 100);
    }
  };

  return (
    <div className="flex h-[calc(100vh-8rem)] flex-col">
      <h1 className="text-2xl font-bold mb-4">AI Compliance Assistant</h1>

      <Card className="flex-1 flex flex-col overflow-hidden">
        <div className="flex-1 overflow-y-auto p-4">
          {messages.length === 0 ? (
            <div className="flex h-full items-center justify-center text-center py-20">
              <div>
                <Bot className="h-12 w-12 mx-auto text-emerald-600 mb-3" />
                <h2 className="text-lg font-semibold">Compliance Assistant</h2>
                <p className="text-sm text-muted-foreground max-w-sm mt-1">
                  Ask questions about Bangladesh banking regulations, circulars, and compliance requirements. All answers include citations.
                </p>
                <div className="mt-4 flex flex-wrap gap-2 justify-center">
                  {[
                    "What are the latest AML/CFT requirements?",
                    "Summarize loan classification rules",
                    "What are the reporting deadlines?",
                  ].map((q) => (
                    <Button
                      key={q}
                      variant="outline"
                      size="sm"
                      className="text-xs"
                      onClick={() => setInput(q)}
                    >
                      {q}
                    </Button>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            <div className="space-y-4">
              {messages.map((msg, i) => (
                <div key={i} className={`flex gap-3 ${msg.role === "user" ? "justify-end" : ""}`}>
                  {msg.role === "assistant" && (
                    <div className="h-7 w-7 rounded-full bg-emerald-100 dark:bg-emerald-950 flex items-center justify-center shrink-0">
                      <Bot className="h-3.5 w-3.5 text-emerald-600" />
                    </div>
                  )}
                  <div
                    className={`max-w-[70%] rounded-lg px-4 py-2.5 text-sm ${
                      msg.role === "user"
                        ? "bg-emerald-600 text-white"
                        : "bg-muted"
                    }`}
                  >
                    <p className="whitespace-pre-wrap">{msg.content}</p>
                    {msg.citations && msg.citations.length > 0 && (
                      <div className="mt-2 border-t border-border/50 pt-2">
                        <p className="text-xs font-medium opacity-70 mb-1">Citations:</p>
                        {msg.citations.map((c, j) => (
                          <p key={j} className="text-xs opacity-60">{c}</p>
                        ))}
                      </div>
                    )}
                  </div>
                  {msg.role === "user" && (
                    <div className="h-7 w-7 rounded-full bg-muted flex items-center justify-center shrink-0">
                      <User className="h-3.5 w-3.5" />
                    </div>
                  )}
                </div>
              ))}
              {loading && (
                <div className="flex gap-3">
                  <div className="h-7 w-7 rounded-full bg-emerald-100 dark:bg-emerald-950 flex items-center justify-center shrink-0">
                    <Bot className="h-3.5 w-3.5 text-emerald-600" />
                  </div>
                  <div className="bg-muted rounded-lg px-4 py-2.5">
                    <div className="flex gap-1">
                      <span className="h-2 w-2 rounded-full bg-muted-foreground/50 animate-bounce [animation-delay:0ms]" />
                      <span className="h-2 w-2 rounded-full bg-muted-foreground/50 animate-bounce [animation-delay:150ms]" />
                      <span className="h-2 w-2 rounded-full bg-muted-foreground/50 animate-bounce [animation-delay:300ms]" />
                    </div>
                  </div>
                </div>
              )}
              <div ref={bottomRef} />
            </div>
          )}
        </div>

        <CardContent className="border-t p-3">
          <div className="flex gap-2">
            <Textarea
              placeholder="Ask a compliance question..."
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter" && !e.shiftKey) {
                  e.preventDefault();
                  send();
                }
              }}
              rows={1}
              className="min-h-[40px] max-h-[120px] resize-none flex-1"
            />
            <Button
              size="icon"
              className="h-10 w-10 bg-emerald-600 hover:bg-emerald-700 shrink-0"
              onClick={send}
              disabled={loading || !input.trim()}
            >
              <Send className="h-4 w-4" />
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
